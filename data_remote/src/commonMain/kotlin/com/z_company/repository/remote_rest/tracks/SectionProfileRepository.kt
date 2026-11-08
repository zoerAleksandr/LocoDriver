package com.z_company.repository.remote_rest.tracks

import com.z_company.domain.track.AxisPoint
import com.z_company.domain.track.EdgeTraversal
import com.z_company.domain.track.GraphEdge
import com.z_company.domain.track.GraphNode
import com.z_company.domain.track.KmMark
import com.z_company.domain.track.ProfileElement
import com.z_company.domain.track.SectionAxis
import com.z_company.domain.track.SectionBox
import com.z_company.domain.track.SectionBrakeTest
import com.z_company.domain.track.SectionCrossing
import com.z_company.domain.track.SectionGraph
import com.z_company.domain.track.SectionProfile
import com.z_company.domain.track.SectionStation
import com.z_company.domain.track.SectionSummary
import com.z_company.domain.track.SignalKind
import com.z_company.domain.track.SignalTrack
import com.z_company.domain.track.TrackSignal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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

    /** Идентификаторы сохранённых на диске участков (без списка `index`). Экран «Участки без интернета». */
    fun cachedIds(): Set<String> = emptySet()

    /** Удалить один сохранённый участок (пользователь освобождает место). */
    fun remove(sectionId: String) {}

    /** Размер файла сохранённого участка в байтах (0 - нет). */
    fun sizeBytes(sectionId: String): Long = 0L

    /**
     * Транзакция замены явно скачанного участка: старый файл живёт до успешной
     * загрузки карты. Файловая реализация хранит backup на диске, чтобы пережить падение
     * процесса. Значения по умолчанию нужны для нефайловых test/cache реализаций.
     */
    fun beginOfflineReplace(sectionId: String) {}
    fun commitOfflineReplace(sectionId: String) {}
    fun rollbackOfflineReplace(sectionId: String) {}
}

/** Итог явной загрузки участка пользователем ([SectionProfileRepository.downloadForOffline]). */
enum class OfflineDownloadResult { OK, RATE_LIMITED, DEVICE_REJECTED, NOT_AVAILABLE, FAILED }

/** Сохранённый на телефоне участок: версия, хэш оси и размер файла. */
data class OfflineSectionInfo(val updatedAt: Double, val axisHash: String, val bytes: Long)

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
    private var graphMem: SectionGraph? = null
    private var graphFetchedAt: kotlin.time.TimeMark? = null

    /** Участки, которые сервер отдал, но использовать нельзя (нет оси): id -> `updatedAt`, пока он тот же - заново не качаем. */
    private val unusable = HashMap<String, Double>()

    /** После отказа сервера по лимиту (429) загрузки участков не повторяются [RATE_LIMIT_PAUSE]: повторы только продлевают блокировку. */
    private var pausedUntil: kotlin.time.TimeMark? = null

    /**
     * Участок не скачался (сеть, 5xx): следующую попытку - не раньше [FAIL_BACKOFF]. Без этого экран карты повторял всю цепочку
     * (до 8 участков, каждый - challenge+регистрация+запрос) каждые 30 с и сжигал лимиты сервера у человека в зоне без связи.
     */
    private val failedUntil = HashMap<String, kotlin.time.TimeMark>()
    private val failCount = HashMap<String, Int>()

    /** Когда последний раз успешно обновляли список: чаще [INDEX_REFRESH_MIN] сервер за ним не спрашиваем (открытие/закрытие экрана). */
    private var indexFetchedAt: kotlin.time.TimeMark? = null

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
    suspend fun sectionsNear(
        lat: Double, lon: Double, radiusM: Double = NEAR_RADIUS_M, limit: Int = NEAR_LIMIT, refresh: Boolean = false,
        /** Только память/диск, без сети: мгновенно, что уже сохранено (потом - обычный вызов, он докачает и обновит). */
        cacheOnly: Boolean = false,
    ): List<SectionProfile> {
        val index = loadIndex(refresh)
        val withBox = index.mapNotNull { dto -> SectionBox.of(dto.bbox)?.let { dto to it.distanceM(lat, lon) } }
        // Без габарита берём только участки с геометрией (старый сервер не отдаёт габарит): у участка без геометрии качать нечего,
        // а раньше каждый вызов тратил на каждый такой участок запрос к серверу (лимиты загрузок аккаунта).
        val noBox = index.filter { it.hasGeometry && SectionBox.of(it.bbox) == null }
        val near = withBox.filter { it.second <= radiusM }.sortedBy { it.second }.take(limit).map { it.first }
        // Участки без габарита (нет геометрии либо старый сервер) отсеять нечем - берём как есть; без геометрии section() вернёт null.
        return (near + noBox).distinctBy { it.id }.mapNotNull {
            runCatching { if (cacheOnly) cached(it.id) else section(it.id, refresh = false) }.getOrNull()
        }
    }

    /** Участок целиком; null - нет ни в кэше, ни на сервере, либо у него нет геометрии. */
    suspend fun section(id: String, refresh: Boolean = true): SectionProfile? = lock.withLock {
        val summary = loadIndexLocked(refresh).firstOrNull { it.id == id }
        val inMem = memory[id]
        if (inMem != null && (summary == null || inMem.first == summary.updatedAt)) return inMem.second
        if (summary != null && unusable[id] == summary.updatedAt) return null
        val cached = readCached(id)
        if (cached != null && (summary == null || cached.first == summary.updatedAt)) {
            memory[id] = cached
            return cached.second
        }
        // Кэш устарел (у списка `updatedAt` новее) или его нет - качаем независимо от [refresh]: список уже обновлён,
        // и дополнения автора участка должны доходить до карты. Сбой сети - остаётся прежний кэш.
        val fresh = download(id)
        val result = fresh ?: cached
        result?.let { memory[id] = it }
        result?.second
    }

    /** Участок только из памяти/диска, без сети (фоновые потребители вроде журнала проследования); null - не сохранён. */
    suspend fun cached(id: String): SectionProfile? = lock.withLock {
        memory[id]?.second ?: readCached(id)?.also { memory[id] = it }?.second
    }

    /**
     * Граф стыков и развилок (топология участков для плеча через несколько участков). Сеть - не чаще [GRAPH_REFRESH_MIN];
     * сбой сети или старый сервер без эндпоинта - кэш памяти/диска. null - графа нет (старый сервер, ничего не сохранено).
     */
    suspend fun graph(refresh: Boolean = true): SectionGraph? {
        val cachedGraph = graphMem ?: readCachedGraph()?.also { graphMem = it }
        val stale = graphFetchedAt?.elapsedNow()?.let { it >= GRAPH_REFRESH_MIN } ?: true
        if (!refresh || !stale) return cachedGraph
        val dto = runCatching { api.graph() }.getOrNull() ?: return cachedGraph
        val parsed = toGraph(dto) ?: return cachedGraph
        graphFetchedAt = kotlin.time.TimeSource.Monotonic.markNow()
        graphMem = parsed
        runCatching { cache.save(GRAPH_KEY, json.encodeToString(AdvisorGraphDto.serializer(), dto)) }
        return parsed
    }

    private fun toGraph(d: AdvisorGraphDto): SectionGraph? {
        if (d.edges.isEmpty()) return null
        return SectionGraph(
            version = d.version,
            nodes = d.nodes.map { GraphNode(it.id, it.lat, it.lon, it.name) },
            edges = d.edges.map { GraphEdge(it.id, it.sectionId, it.fromKm, it.toKm, it.fromNode, it.toNode, it.lengthKm) },
        )
    }

    private fun readCachedGraph(): SectionGraph? {
        val text = cache.load(GRAPH_KEY) ?: return null
        return runCatching { json.decodeFromString(AdvisorGraphDto.serializer(), text) }.getOrNull()?.let(::toGraph)
    }

    /**
     * Путь плеча между станциями по графу стыков: сервер ищет по всем опубликованным участкам
     * (клиенту не нужно докачивать их владельцев, чтобы найти путь). null - пути нет, нет сети или старый сервер.
     */
    suspend fun graphPath(fromName: String, toName: String): List<EdgeTraversal>? {
        val dto = runCatching { api.graphPath(fromName, toName) }.getOrNull() ?: return null
        return dto.traversals.map { t ->
            val e = t.edge
            EdgeTraversal(GraphEdge(e.id, e.sectionId, e.fromKm, e.toKm, e.fromNode, e.toNode, e.lengthKm), t.startKm, t.endKm)
        }.takeIf { it.isNotEmpty() }
    }

    /**
     * Все оси для подбора ближайшего участка по положению. Скачивает недостающие участки (по одному, сбой одного не мешает остальным).
     * Пока сервер не отдаёт габарит участка в списке, нужны оси всех опубликованных.
     */
    suspend fun allSections(refresh: Boolean = true): List<SectionProfile> =
        loadIndex(refresh).mapNotNull { runCatching { section(it.id, refresh = false) }.getOrNull() }

    /**
     * Синхронизация профилей участков (в общей синхронизации приложения): обновить список, перекачать уже сохранённые участки,
     * изменившиеся на сервере (сравнение `updatedAt` списка с сохранённым), убрать с диска снятые с публикации.
     * true - сервер ответил; false - сети нет (кэш не тронут). Сбой одного участка не мешает остальным.
     */
    suspend fun syncAll(): Boolean {
        val fresh = runCatching { api.sections() }.getOrNull() ?: return false
        lock.withLock { storeIndexLocked(fresh) }
        // Только участки, уже сохранённые на этом телефоне: новые по-прежнему качаются по положению (габарит, квоты сервера).
        fresh.filter { it.hasGeometry && readCached(it.id) != null }.forEach { runCatching { section(it.id, refresh = false) } }
        cleanUp()
        return true
    }

    /**
     * Что уже лежит на телефоне: id -> версия и размер. Читает файлы (десятки участков - мгновенно); сеть не нужна.
     * Участок с нечитаемым файлом (ключ потерян) в список не попадает - он будет скачан заново.
     */
    suspend fun offlineSections(): Map<String, OfflineSectionInfo> = lock.withLock {
        cache.cachedIds().mapNotNull { id ->
            readCached(id)?.let { id to OfflineSectionInfo(it.first, it.second.axisHash, cache.sizeBytes(id)) }
        }.toMap()
    }

    /** Начать/завершить атомарную замену профиля вместе с паком карты. */
    suspend fun beginOfflineReplace(id: String) = lock.withLock { cache.beginOfflineReplace(id) }

    suspend fun commitOfflineReplace(id: String) = lock.withLock { cache.commitOfflineReplace(id) }

    suspend fun rollbackOfflineReplace(id: String) = lock.withLock {
        cache.rollbackOfflineReplace(id)
        memory.remove(id)
    }

    /** Все участки, сохранённые на телефоне (только диск/память, без сети): основа для расчёта плеча без интернета. */
    suspend fun cachedSections(): List<SectionProfile> =
        lock.withLock { cache.cachedIds() }.mapNotNull { runCatching { cached(it) }.getOrNull() }

    /**
     * Явная загрузка участка «для работы без интернета». Действие пользователя: паузы и задержки после прошлых сбоев не
     * мешают (число попыток ограничено нажатиями), но ответ сервера - лимит, отказ устройства - возвращается как есть.
     */
    suspend fun downloadForOffline(id: String): OfflineDownloadResult {
        lock.withLock {
            failedUntil.remove(id); failCount.remove(id)
            pausedUntil = null
            unusable.remove(id)
        }
        val profile = runCatching { section(id, refresh = false) }.getOrNull()
        return when {
            profile != null -> OfflineDownloadResult.OK
            rateLimited -> OfflineDownloadResult.RATE_LIMITED
            deviceRejected -> OfflineDownloadResult.DEVICE_REJECTED
            lock.withLock { unusable.containsKey(id) } -> OfflineDownloadResult.NOT_AVAILABLE
            else -> OfflineDownloadResult.FAILED
        }
    }

    /** Убрать участок с телефона (освободить место). Список участков не трогается: его можно скачать снова. */
    suspend fun removeOffline(id: String) = lock.withLock {
        memory.remove(id)
        cache.remove(id)
    }

    /** Удалить с диска участки, снятые с публикации. */
    suspend fun cleanUp() = lock.withLock {
        val ids = loadIndexLocked(false).map { it.id }.toSet()
        if (ids.isNotEmpty()) cache.retain(ids)
    }

    /**
     * Запрос к серверу - ВНЕ замка и с таймаутом: при слабой сети он раньше держал замок десятки секунд, и всё, что читает
     * кэш ([cached], [section], [sectionsNear]), ждало сеть - профиль не появлялся, хотя лежал на диске.
     */
    private suspend fun loadIndex(refresh: Boolean): List<AdvisorSectionSummaryDto> {
        if (refresh && !indexFresh()) {
            val fresh = withTimeoutOrNull(INDEX_TIMEOUT_MS) { runCatching { api.sections() }.getOrNull() }
            if (fresh != null) return lock.withLock { storeIndexLocked(fresh); fresh }
        }
        return lock.withLock { loadIndexLocked(false) }
    }

    private fun indexFresh(): Boolean =
        index != null && indexFetchedAt?.let { it.elapsedNow() < INDEX_REFRESH_MIN } == true

    private fun storeIndexLocked(fresh: List<AdvisorSectionSummaryDto>) {
        index = fresh
        indexFetchedAt = kotlin.time.TimeSource.Monotonic.markNow()
        runCatching { cache.saveIndex(json.encodeToString(ListSerializer(AdvisorSectionSummaryDto.serializer()), fresh)) }
    }

    private suspend fun loadIndexLocked(refresh: Boolean): List<AdvisorSectionSummaryDto> {
        if (refresh && !indexFresh()) {
            val fresh = runCatching { api.sections() }.getOrNull()
            if (fresh != null) {
                storeIndexLocked(fresh)
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

    /** Последняя загрузка профиля отклонена сервером по лимиту (429): повторить позже. */
    @kotlin.concurrent.Volatile
    var rateLimited: Boolean = false
        private set

    /** Устройство не прошло проверку подлинности (403: root, эмулятор, чужая сборка): профили с сервера не придут, пока не изменится. */
    @kotlin.concurrent.Volatile
    var deviceRejected: Boolean = false
        private set

    private suspend fun download(id: String): Pair<Double, SectionProfile>? {
        // Пауза после 429/отказа устройства: сети не трогаем, остаётся то, что в кэше. Флаги уже выставлены при отказе.
        pausedUntil?.let { if (!it.hasPassedNow()) return null else pausedUntil = null }
        failedUntil[id]?.let { if (!it.hasPassedNow()) return null }
        val res = runCatching { api.section(id) }
        val error = res.exceptionOrNull()
        rateLimited = error?.message?.contains("429") == true
        // Отказ устройства (аттестация) постоянный: без паузы карта опрашивает сервер каждые 30 с. 403 с другим смыслом
        // (device_required: токен не принят) клиент уже повторил внутри api.section, здесь это тоже сбой, но не «устройство плохое».
        val rejected = error is AdvisorApiException && error.status == 403
        deviceRejected = rejected
        val now = kotlin.time.TimeSource.Monotonic.markNow()
        when {
            rateLimited -> pausedUntil = now + RATE_LIMIT_PAUSE
            rejected -> pausedUntil = now + DEVICE_FAIL_PAUSE
            error != null -> {
                // 1, 2, 4, 8, 15 мин и дальше раз в 15: долгая плохая связь не превращается в сотни запросов в час.
                val n = (failCount[id] ?: 0) + 1
                failCount[id] = n
                failedUntil[id] = now + minOf(FAIL_BACKOFF * (1 shl minOf(n - 1, 4)), FAIL_BACKOFF_MAX)
            }
        }
        error?.let { println("SectionProfile download ${id.take(8)} failed: ${it::class.simpleName}: ${it.message?.take(80)}") }
        val dto = res.getOrNull() ?: return null
        failedUntil.remove(id)
        failCount.remove(id)
        val profile = withContext(Dispatchers.Default) { toProfile(dto) } ?: run { unusable[id] = dto.updatedAt; return null }
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
            elements = d.elements.map { ProfileElement(it.startKm, it.endKm, it.gradePermille, it.curveRadiusM, it.speedLimitKmh, it.noData) },
            kmMarks = d.kmMarks.map { KmMark(it.km, it.kmAbs, it.dir, it.note) },
            stations = d.stations.map { SectionStation(it.km, it.name, it.lat, it.lon) },
            signals = d.signals.map {
                TrackSignal(
                    km = it.km,
                    label = it.label,
                    track = if (it.track.equals("O", ignoreCase = true)) SignalTrack.ODD else SignalTrack.EVEN,
                    kind = when (it.kind.lowercase()) {
                        "station" -> SignalKind.STATION
                        "shunt" -> SignalKind.SHUNT
                        "distant" -> SignalKind.DISTANT
                        else -> SignalKind.PASS
                    },
                    facing = it.facing.coerceIn(-1, 1),
                )
            }.sortedBy { it.km },
            crossings = d.crossings.map { SectionCrossing(it.km, it.label, it.kind) }.sortedBy { it.km },
            brakeTests = d.brakeTests.map { SectionBrakeTest(it.km, it.label, it.kind) }.sortedBy { it.km },
        )
    }

    private companion object {
        /** Радиус отбора участков вокруг положения: ось + запас на соседние участки и погрешность. */
        const val NEAR_RADIUS_M = 5_000.0
        const val INDEX_TIMEOUT_MS = 10_000L
        const val NEAR_LIMIT = 8
        /** Ключ дискового кэша графа (не id участка): файловый кэш его не удаляет и не показывает в «Скачанных». */
        const val GRAPH_KEY = "graph"
        val RATE_LIMIT_PAUSE = kotlin.time.Duration.parse("10m")
        val DEVICE_FAIL_PAUSE = kotlin.time.Duration.parse("30m")
        val FAIL_BACKOFF = kotlin.time.Duration.parse("1m")
        val FAIL_BACKOFF_MAX = kotlin.time.Duration.parse("15m")
        val INDEX_REFRESH_MIN = kotlin.time.Duration.parse("5m")
        val GRAPH_REFRESH_MIN = kotlin.time.Duration.parse("5m")
    }
}
