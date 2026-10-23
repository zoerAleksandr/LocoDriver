package com.z_company.repository.remote_rest.tracks

import com.z_company.domain.track.AxisPoint
import com.z_company.domain.track.KmMark
import com.z_company.domain.track.ProfileElement
import com.z_company.domain.track.SectionAxis
import com.z_company.domain.track.SectionBox
import com.z_company.domain.track.SectionProfile
import com.z_company.domain.track.SectionStation
import com.z_company.domain.track.SectionSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Диск для профилей участков: JSON-текст на участок и список. Реализация платформенная (Android: `cacheDir`). */
interface SectionProfileCache {
    fun loadIndex(): String?
    fun saveIndex(text: String)
    fun load(sectionId: String): String?
    fun save(sectionId: String, text: String)

    /** Удалить участки, которых нет в [keepIds] (сняты с публикации). */
    fun retain(keepIds: Set<String>)
}

/**
 * Профили участков советника для экрана «Профиль пути»: список участков и полный участок (ось, профиль, километраж, станции).
 * Работает офлайн по кэшу: сеть нужна только для обновления, при любой сетевой ошибке отдаётся то, что сохранено.
 * Участок перекачивается, когда у него изменился `updatedAt` в списке. Без входа и без согласия: данные общие, о пользователе
 * ничего не уходит. Координаты и названия не логируются.
 */
class SectionProfileRepository(
    private val api: AdvisorApi,
    private val cache: SectionProfileCache,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val lock = Mutex()
    private val memory = HashMap<String, Pair<Double, SectionProfile>>()
    private var index: List<AdvisorSectionSummaryDto>? = null

    /** Список опубликованных участков. [refresh] - спросить сервер; ошибка сети - молча кэш (или пусто). */
    suspend fun summaries(refresh: Boolean = true): List<SectionSummary> =
        loadIndex(refresh).map {
            SectionSummary(
                it.id, it.name, it.fromStation, it.toStation, it.lengthKm, it.elementsCount > 0, it.updatedAt.toLong(),
                SectionBox.of(it.bbox),
            )
        }

    /**
     * Участки рядом с точкой: оси только тех, чей габарит не дальше [radiusM] (не больше [limit] ближайших). Если сервер габарит
     * не отдаёт (старая версия) - как раньше, все участки. Сбой одного участка не мешает остальным.
     */
    suspend fun sectionsNear(lat: Double, lon: Double, radiusM: Double = NEAR_RADIUS_M, limit: Int = NEAR_LIMIT, refresh: Boolean = false): List<SectionProfile> {
        val index = loadIndex(refresh)
        val withBox = index.mapNotNull { dto -> SectionBox.of(dto.bbox)?.let { dto to it.distanceM(lat, lon) } }
        val noBox = index.filter { SectionBox.of(it.bbox) == null }
        val near = withBox.filter { it.second <= radiusM }.sortedBy { it.second }.take(limit).map { it.first }
        // Участки без габарита (нет геометрии либо старый сервер) отсеять нечем - берём как есть; без геометрии section() вернёт null.
        return (near + noBox).distinctBy { it.id }.mapNotNull { runCatching { section(it.id, refresh = false) }.getOrNull() }
    }

    /** Участок целиком; null - нет ни в кэше, ни на сервере, либо у него нет геометрии. */
    suspend fun section(id: String, refresh: Boolean = true): SectionProfile? = lock.withLock {
        val summary = loadIndexLocked(refresh).firstOrNull { it.id == id }
        val inMem = memory[id]
        if (inMem != null && (summary == null || inMem.first == summary.updatedAt)) return inMem.second
        val cached = readCached(id)
        if (cached != null && (summary == null || cached.first == summary.updatedAt)) {
            memory[id] = cached
            return cached.second
        }
        val fresh = if (refresh || cached == null) download(id) else null
        val result = fresh ?: cached
        result?.let { memory[id] = it }
        result?.second
    }

    /**
     * Все оси для подбора ближайшего участка по положению. Скачивает недостающие участки (по одному, сбой одного не мешает остальным).
     * Пока сервер не отдаёт габарит участка в списке, нужны оси всех опубликованных.
     */
    suspend fun allSections(refresh: Boolean = true): List<SectionProfile> =
        loadIndex(refresh).mapNotNull { runCatching { section(it.id, refresh = false) }.getOrNull() }

    /** Удалить с диска участки, снятые с публикации. */
    suspend fun cleanUp() = lock.withLock {
        val ids = loadIndexLocked(false).map { it.id }.toSet()
        if (ids.isNotEmpty()) cache.retain(ids)
    }

    private suspend fun loadIndex(refresh: Boolean) = lock.withLock { loadIndexLocked(refresh) }

    private suspend fun loadIndexLocked(refresh: Boolean): List<AdvisorSectionSummaryDto> {
        if (refresh) {
            val fresh = runCatching { api.sections() }.getOrNull()
            if (fresh != null) {
                index = fresh
                runCatching { cache.saveIndex(json.encodeToString(ListSerializer(AdvisorSectionSummaryDto.serializer()), fresh)) }
                return fresh
            }
        }
        index?.let { return it }
        val cached = withContext(Dispatchers.Default) {
            cache.loadIndex()?.let { runCatching { json.decodeFromString(ListSerializer(AdvisorSectionSummaryDto.serializer()), it) }.getOrNull() }
        }.orEmpty()
        index = cached
        return cached
    }

    private suspend fun readCached(id: String): Pair<Double, SectionProfile>? = withContext(Dispatchers.Default) {
        val text = cache.load(id) ?: return@withContext null
        val dto = runCatching { json.decodeFromString(AdvisorSectionDto.serializer(), text) }.getOrNull() ?: return@withContext null
        toProfile(dto)?.let { dto.updatedAt to it }
    }

    private suspend fun download(id: String): Pair<Double, SectionProfile>? {
        val dto = runCatching { api.section(id) }.getOrNull() ?: return null
        val profile = withContext(Dispatchers.Default) { toProfile(dto) } ?: return null
        runCatching { cache.save(id, json.encodeToString(AdvisorSectionDto.serializer(), dto)) }
        return dto.updatedAt to profile
    }

    private fun toProfile(d: AdvisorSectionDto): SectionProfile? {
        val hash = d.axisHash ?: return null
        if (d.geometry.size < 2) return null
        return SectionProfile(
            id = d.id,
            name = d.name,
            fromStation = d.fromStation,
            toStation = d.toStation,
            lengthKm = d.lengthKm,
            axisHash = hash,
            axis = SectionAxis(d.id, hash, d.geometry.map { AxisPoint(it.km, it.lat, it.lon) }),
            elements = d.elements.map { ProfileElement(it.startKm, it.endKm, it.gradePermille, it.curveRadiusM, it.speedLimitKmh) },
            kmMarks = d.kmMarks.map { KmMark(it.km, it.kmAbs, it.dir, it.note) },
            stations = d.stations.map { SectionStation(it.km, it.name, it.lat, it.lon) },
        )
    }

    private companion object {
        /** Радиус отбора участков вокруг положения: ось + запас на соседние участки и погрешность. */
        const val NEAR_RADIUS_M = 5_000.0
        const val NEAR_LIMIT = 8
    }
}
