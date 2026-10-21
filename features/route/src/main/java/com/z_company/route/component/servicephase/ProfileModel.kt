package com.z_company.route.component.servicephase

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.z_company.domain.track.ElevationLine
import com.z_company.domain.track.LegMatch
import com.z_company.domain.track.LivePosition
import com.z_company.domain.track.LiveProfile
import com.z_company.domain.track.SectionPicker
import com.z_company.domain.track.SectionProfile
import com.z_company.domain.util.GeoFix
import com.z_company.domain.util.TrainMapStop
import com.z_company.repository.remote_rest.tracks.SectionProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import kotlin.math.abs

/** Строка списка выбора участка. */
internal class SectionRow(val section: SectionProfile, val distanceM: Double?, val fitsLeg: Boolean?)

/** Что показывать вместо графика, когда рисовать нечего. */
internal enum class ProfileEmpty { LOADING, NO_LOCATION, NO_SECTIONS, NO_NEARBY, NO_DATA }

/**
 * Состояние «Профиля пути» на карте: загруженные участки, выбранный участок (по положению либо вручную), направление и
 * живая позиция на профиле. Координаты нигде не логируются, на сервер ничего не уходит.
 */
@Stable
internal class ProfileModel(private val repo: SectionProfileRepository) {
    var sections by mutableStateOf<List<SectionProfile>>(emptyList())
        private set
    var loaded by mutableStateOf(false)
        private set

    /** Участок, выбранный пользователем; null - подбирается автоматически по положению и плечу. */
    var manualId by mutableStateOf<String?>(null)
        private set
    private var autoId by mutableStateOf<String?>(null)

    /** Направление, выбранное пользователем; null - по плечу, а без плеча - по движению. */
    private var manualReversed by mutableStateOf<Boolean?>(null)
    private var motionReversed by mutableStateOf<Boolean?>(null)

    var live by mutableStateOf<LiveProfile?>(null)
        private set
    var position by mutableStateOf<LivePosition?>(null)
        private set

    /** Названия станций плеча поезда по порядку (отправление ... прибытие); меньше двух - плеча нет. */
    var stopNames: List<String> = emptyList()
    private var lastFix: GeoFix? = null
    private var hasFix by mutableStateOf(false)
    private var motionLastKm: Double? = null
    private var motionSum = 0.0

    val section: SectionProfile? get() = live?.section
    val reversed: Boolean get() = live?.reversed ?: false

    /** Плечо поезда заполнено (есть отправление и прибытие). */
    val hasLeg: Boolean get() = stopNames.size >= 2

    /** Подходит ли выбранный участок плечу; null - плеча нет. */
    val fitsLeg: Boolean? get() = section?.let { LegMatch.fits(it, stopNames) }

    val emptyReason: ProfileEmpty?
        get() = when {
            !loaded -> ProfileEmpty.LOADING
            sections.isEmpty() -> ProfileEmpty.NO_SECTIONS
            section == null && !hasFix -> ProfileEmpty.NO_LOCATION
            section == null -> ProfileEmpty.NO_NEARBY
            section?.hasProfile != true -> ProfileEmpty.NO_DATA
            else -> null
        }

    private var elevationCache: Pair<String, ElevationLine>? = null

    /** Линия высот выбранного участка (кэшируется по id и версии элементов). */
    fun elevation(): ElevationLine? {
        val s = section ?: return null
        val key = s.id + s.axisHash + s.elements.size
        elevationCache?.takeIf { it.first == key }?.let { return it.second }
        return ElevationLine.fromElements(s.elements).also { elevationCache = key to it }
    }

    suspend fun load() {
        // Сначала то, что на диске (мгновенно и без сети), затем обновление.
        sections = withContext(Dispatchers.Default) { repo.allSections(refresh = false) }
        if (sections.isNotEmpty()) { loaded = true; rebuild() }
        val fresh = withContext(Dispatchers.Default) { repo.allSections(refresh = true) }
        if (fresh.isNotEmpty() || sections.isEmpty()) sections = fresh
        loaded = true
        rebuild()
    }

    fun onFix(fix: GeoFix?, speedKmh: Double?, timeMs: Long) {
        lastFix = fix
        hasFix = fix != null
        if (fix == null || sections.isEmpty()) return
        if (manualId == null) autoPick(fix)
        ensureLive()
        val l = live ?: return
        val p = l.onFix(timeMs, fix.latitude, fix.longitude, speedKmh, fix.accuracyMeters)
        position = p
        detectMotionDirection(p)
        // Участок выбран автоматически, а мы вне его оси - возможно, поезд уехал на другой участок: подберём заново.
        if (manualId == null && p?.offRoute == true) autoPick(fix, force = true)
    }

    /** Пользователь выбрал участок: автоподбор отключается, пока не вызван [useAuto]. */
    fun choose(id: String) {
        manualId = id
        manualReversed = null
        motionReversed = null
        resetMotion()
        rebuild()
    }

    fun useAuto() {
        manualId = null
        manualReversed = null
        motionReversed = null
        resetMotion()
        lastFix?.let { autoPick(it, force = true) }
        rebuild()
    }

    fun toggleDirection() {
        manualReversed = !reversed
        rebuild()
    }

    /** Список для выбора: по расстоянию до положения (если есть), иначе по названию; [query] - по названию, станциям. */
    fun rows(query: String): List<SectionRow> {
        val fix = lastFix
        val q = query.trim().lowercase().replace('ё', 'е')
        val near = fix?.let { SectionPicker.nearest(sections.map { it.axis }, it.latitude, it.longitude, maxDistanceM = Double.MAX_VALUE / 4, limit = Int.MAX_VALUE) }
            ?.associate { it.sectionId to it.distanceM }.orEmpty()
        return sections
            .filter { q.isEmpty() || matches(it, q) }
            .map { SectionRow(it, near[it.id], LegMatch.fits(it, stopNames)) }
            .sortedWith(
                compareBy<SectionRow> { if (it.fitsLeg == true) 0 else 1 }
                    .thenBy { it.distanceM ?: Double.MAX_VALUE }
                    .thenBy { it.section.name },
            )
    }

    private fun matches(s: SectionProfile, q: String): Boolean {
        fun n(t: String) = t.lowercase().replace('ё', 'е')
        return n(s.name).contains(q) || n(s.fromStation).contains(q) || n(s.toStation).contains(q) ||
            s.stations.any { n(it.name).contains(q) }
    }

    private fun autoPick(fix: GeoFix, force: Boolean = false) {
        if (!force && autoId != null && live?.position?.offRoute != true) return
        val near = SectionPicker.nearest(sections.map { it.axis }, fix.latitude, fix.longitude)
        if (near.isEmpty()) { autoId = null; return }
        // Среди близких предпочитаем участок, на котором есть станции плеча.
        val legIds = LegMatch.sections(near.mapNotNull { m -> sections.firstOrNull { it.id == m.sectionId } }, stopNames).map { it.id }.toSet()
        val pick = near.firstOrNull { it.sectionId in legIds } ?: near.first()
        if (pick.sectionId != autoId) { autoId = pick.sectionId; motionReversed = null; resetMotion() }
    }

    private fun currentId(): String? = manualId ?: autoId

    private fun reversedNow(section: SectionProfile): Boolean =
        manualReversed ?: LegMatch.reversed(section, stopNames) ?: motionReversed ?: false

    private fun ensureLive() {
        val id = currentId() ?: run { live = null; position = null; return }
        val s = sections.firstOrNull { it.id == id } ?: run { live = null; position = null; return }
        val rev = reversedNow(s)
        val cur = live
        if (cur == null || cur.section.id != s.id || cur.reversed != rev || cur.section !== s) {
            val keep = cur?.takeIf { it.section.id == s.id }?.position
            live = LiveProfile(s, rev)
            position = keep
        }
    }

    private fun rebuild() {
        ensureLive()
        lastFix?.let { f -> live?.let { position = it.onFix(System.currentTimeMillis(), f.latitude, f.longitude, null, f.accuracyMeters) } }
    }

    /**
     * Без плеча и без ручного выбора направление берём из движения: суммируем знаковые шаги между соседними фиксами,
     * набрали 300 м - определилось. Шаги больше [MAX_STEP_KM] (скачок позиции, потеря GPS, смена участка) не считаются
     * и обнуляют накопленное, чтобы направление не определялось по скачку.
     */
    private fun detectMotionDirection(p: LivePosition?) {
        if (p == null || !p.locked || p.offRoute || manualReversed != null || motionReversed != null) return
        val s = section ?: return
        if (LegMatch.reversed(s, stopNames) != null) return
        val prev = motionLastKm
        motionLastKm = p.kmAxis
        if (prev == null) return
        val step = p.kmAxis - prev
        if (abs(step) > MAX_STEP_KM) { motionSum = 0.0; return }
        motionSum += step
        if (abs(motionSum) >= MOTION_KM) {
            motionReversed = motionSum < 0
            ensureLive()
        }
    }

    private fun resetMotion() { motionLastKm = null; motionSum = 0.0 }

    private companion object {
        const val MOTION_KM = 0.3
        const val MAX_STEP_KM = 1.0
    }
}

/** Модель профиля на жизнь карты; живой фикс подаётся с каждым обновлением геолокации. */
@Composable
internal fun rememberProfileModel(fix: GeoFix?, speedKmh: Double?, fixAtMs: Long, stops: List<TrainMapStop>): ProfileModel {
    val repo: SectionProfileRepository = koinInject()
    val model = remember { ProfileModel(repo) }
    model.stopNames = remember(stops) { stops.map { it.name }.filter { it.isNotBlank() } }
    LaunchedEffect(Unit) { model.load() }
    LaunchedEffect(fix, fixAtMs, model.loaded) { model.onFix(fix, speedKmh, if (fixAtMs > 0) fixAtMs else System.currentTimeMillis()) }
    return model
}
