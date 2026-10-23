package com.z_company.route.component.servicephase

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.z_company.domain.track.Calibration
import com.z_company.domain.track.ElevationLine
import com.z_company.domain.track.LegMatch
import com.z_company.domain.track.LivePosition
import com.z_company.domain.track.LiveProfile
import com.z_company.domain.track.SectionPicker
import com.z_company.domain.track.SectionSummary
import com.z_company.domain.track.SectionProfile
import com.z_company.domain.util.GeoFix
import com.z_company.domain.util.TrainMapStop
import com.z_company.repository.remote_rest.tracks.SectionProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import kotlin.math.abs

/** Строка списка выбора участка. */
internal class SectionRow(val summary: SectionSummary, val distanceM: Double?, val fitsLeg: Boolean?)

/** Поезд маршрута для линии на профиле; [lengthM] null - длина не указана (линия не рисуется, остаётся точка). */
internal data class ProfileTrain(val id: String, val number: String?, val lengthM: Int?)

/** Что показывать вместо графика, когда рисовать нечего. */
internal enum class ProfileEmpty { LOADING, NO_LOCATION, NO_SECTIONS, NO_NEARBY, NO_DATA }

/**
 * Состояние «Профиля пути» на карте: загруженные участки, выбранный участок (по положению либо вручную), направление и
 * живая позиция на профиле. Координаты нигде не логируются, на сервер ничего не уходит.
 */
@Stable
internal class ProfileModel(
    private val repo: SectionProfileRepository,
    private val calibrations: ProfileCalibrationStore,
    private val scope: CoroutineScope,
) {
    /** Список всех участков (название, длина, габарит) - без осей; по нему выбор вручную и поиск. */
    var summaries by mutableStateOf<List<SectionSummary>>(emptyList())
        private set

    /** Участки с загруженной осью и профилем: ближние к положению и выбранные вручную. */
    var sections by mutableStateOf<List<SectionProfile>>(emptyList())
        private set
    var loaded by mutableStateOf(false)
        private set
    private var loadingNear by mutableStateOf(false)
    private var nearCenter: GeoFix? = null

    /** Не удалось загрузить выбранный вручную участок (нет сети и нет в кэше). */
    var chooseError by mutableStateOf<String?>(null)
        private set

    /** Поезда маршрута и выбранный из них: по длине рисуется линия поезда на профиле. */
    var trains by mutableStateOf<List<ProfileTrain>>(emptyList())
    var selectedTrainId by mutableStateOf<String?>(null)
        private set
    val train: ProfileTrain? get() = trains.firstOrNull { it.id == selectedTrainId } ?: trains.firstOrNull()
    val trainLengthKm: Double? get() = train?.lengthM?.takeIf { it > 0 }?.let { it / 1000.0 }
    fun chooseTrain(id: String) { selectedTrainId = id }
    fun initTrains(list: List<ProfileTrain>, initialId: String?) {
        trains = list
        if (selectedTrainId == null || list.none { it.id == selectedTrainId }) {
            selectedTrainId = initialId?.takeIf { id -> list.any { it.id == id } } ?: list.lastOrNull()?.id
        }
    }

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
            summaries.isEmpty() && sections.isEmpty() -> ProfileEmpty.NO_SECTIONS
            section == null && !hasFix -> ProfileEmpty.NO_LOCATION
            section == null && loadingNear -> ProfileEmpty.LOADING
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

    /** Список участков: сначала с диска (мгновенно, без сети), затем обновление. Оси подгружаются по положению ([ensureNear]). */
    suspend fun load() {
        summaries = withContext(Dispatchers.Default) { repo.summaries(refresh = false) }
        if (summaries.isNotEmpty()) loaded = true
        val fresh = withContext(Dispatchers.Default) { repo.summaries(refresh = true) }
        if (fresh.isNotEmpty() || summaries.isEmpty()) summaries = fresh
        loaded = true
        lastFix?.let { ensureNear(it) }
    }

    /** Оси ближних участков (по габариту из списка; старый сервер без габарита - все). Не чаще, чем при сдвиге на 2 км. */
    private fun ensureNear(fix: GeoFix) {
        if (!loaded || loadingNear) return
        nearCenter?.let { c ->
            val dLat = (fix.latitude - c.latitude) * com.z_company.domain.track.FlatMetric.M_PER_DEG_LAT
            val dLon = (fix.longitude - c.longitude) * com.z_company.domain.track.FlatMetric.kx(fix.latitude)
            if (dLat * dLat + dLon * dLon < NEAR_RELOAD_M * NEAR_RELOAD_M) return
        }
        loadingNear = true
        scope.launch {
            val found = runCatching { withContext(Dispatchers.Default) { repo.sectionsNear(fix.latitude, fix.longitude) } }.getOrDefault(emptyList())
            merge(found)
            nearCenter = fix
            loadingNear = false
            rebuild()
        }
    }

    private fun merge(list: List<SectionProfile>) {
        if (list.isEmpty()) return
        val ids = list.map { it.id }.toSet()
        sections = sections.filter { it.id !in ids } + list
    }

    fun onFix(fix: GeoFix?, speedKmh: Double?, timeMs: Long) {
        lastFix = fix
        hasFix = fix != null
        if (fix == null) return
        ensureNear(fix)
        if (sections.isEmpty()) return
        if (manualId == null) autoPick(fix)
        ensureLive()
        val l = live ?: return
        val p = l.onFix(timeMs, fix.latitude, fix.longitude, speedKmh, fix.accuracyMeters)
        position = p
        detectMotionDirection(p)
        // Участок выбран автоматически, а мы вне его оси - возможно, поезд уехал на другой участок: подберём заново.
        if (manualId == null && p?.offRoute == true) autoPick(fix, force = true)
    }

    /** Пользователь выбрал участок: автоподбор отключается, пока не вызван [useAuto]. Ось незагруженного участка подгружается. */
    fun choose(id: String) {
        chooseError = null
        if (sections.any { it.id == id }) { applyChoice(id); return }
        scope.launch {
            val loadedSection = runCatching { withContext(Dispatchers.Default) { repo.section(id, refresh = false) } }.getOrNull()
            if (loadedSection == null) {
                chooseError = "Не удалось загрузить участок. Проверьте интернет."
            } else {
                merge(listOf(loadedSection))
                applyChoice(id)
            }
        }
    }

    private fun applyChoice(id: String) {
        manualId = id
        manualReversed = null
        motionReversed = null
        resetMotion()
        rebuild()
    }

    fun useAuto() {
        chooseError = null
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

    /**
     * Список для выбора: сверху участки плеча поезда, затем по расстоянию до положения (до оси - если ось загружена, иначе до
     * габарита), без положения - по названию; [query] - по названию и станциям (станции - у загруженных).
     */
    fun rows(query: String): List<SectionRow> {
        val fix = lastFix
        val q = query.trim().lowercase().replace('ё', 'е')
        val loadedById = sections.associateBy { it.id }
        fun n(t: String) = t.lowercase().replace('ё', 'е')
        return summaries
            .filter { sum ->
                q.isEmpty() || n(sum.name).contains(q) || n(sum.fromStation).contains(q) || n(sum.toStation).contains(q) ||
                    loadedById[sum.id]?.stations?.any { n(it.name).contains(q) } == true
            }
            .map { sum ->
                val loadedSection = loadedById[sum.id]
                val dist = when {
                    fix == null -> null
                    loadedSection != null -> SectionPicker.nearest(listOf(loadedSection.axis), fix.latitude, fix.longitude, maxDistanceM = Double.MAX_VALUE / 4, limit = 1).firstOrNull()?.distanceM
                    else -> sum.box?.distanceM(fix.latitude, fix.longitude)
                }
                val fits = loadedSection?.let { LegMatch.fits(it, stopNames) } ?: legGuess(sum)
                SectionRow(sum, dist, fits)
            }
            .sortedWith(
                compareBy<SectionRow> { if (it.fitsLeg == true) 0 else 1 }
                    .thenBy { it.distanceM ?: Double.MAX_VALUE }
                    .thenBy { it.summary.name },
            )
    }

    /** Участок ещё не загружен: плечо угадываем по названию (в нём обе крайние станции плеча); null - плеча нет или не видно. */
    private fun legGuess(sum: SectionSummary): Boolean? {
        if (stopNames.size < 2) return null
        val text = "${sum.name} ${sum.fromStation} ${sum.toStation}"
        return if (SectionPicker.mentions(text, stopNames.first()) && SectionPicker.mentions(text, stopNames.last())) true else null
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
            live = LiveProfile(s, rev).also { it.applyCalibration(calibrations.get(s.id, s.axisHash)) }
            position = keep ?: live?.position
            calibrationKm = live?.calibrationKm ?: 0.0
        }
    }

    /** Поправка по оси для выбранного участка, км (0 - нет); для показа «поправка +120 м». */
    var calibrationKm by mutableStateOf(0.0)
        private set

    /** Подбор поправки по введённому «я на [absKm]»; ничего не применяет. */
    fun calibrationFor(absKm: Double): Calibration = live?.calibrationFor(absKm) ?: Calibration.NoPosition

    /** Применить и запомнить поправку для этого участка. */
    fun applyCalibration(shiftKm: Double) {
        val l = live ?: return
        l.applyCalibration(shiftKm)
        calibrations.set(l.section.id, l.section.axisHash, shiftKm)
        calibrationKm = shiftKm
        position = l.position
    }

    fun resetCalibration() = applyCalibration(0.0)

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
        const val NEAR_RELOAD_M = 2_000.0
    }
}

/**
 * Модель профиля на жизнь карты; живой фикс подаётся с каждым обновлением геолокации. [trains] - поезда маршрута (линия
 * поезда на профиле), [trainId] - с каким поездом открыта карта (иначе последний в списке).
 */
@Composable
internal fun rememberProfileModel(
    fix: GeoFix?,
    speedKmh: Double?,
    fixAtMs: Long,
    stops: List<TrainMapStop>,
    trains: List<ProfileTrain> = emptyList(),
    trainId: String? = null,
): ProfileModel {
    val repo: SectionProfileRepository = koinInject()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val model = remember { ProfileModel(repo, ProfileCalibrationStore(context), scope) }
    model.stopNames = remember(stops) { stops.map { it.name }.filter { it.isNotBlank() } }
    LaunchedEffect(trains, trainId) { model.initTrains(trains, trainId) }
    LaunchedEffect(Unit) { model.load() }
    LaunchedEffect(fix, fixAtMs, model.loaded) { model.onFix(fix, speedKmh, if (fixAtMs > 0) fixAtMs else System.currentTimeMillis()) }
    return model
}
