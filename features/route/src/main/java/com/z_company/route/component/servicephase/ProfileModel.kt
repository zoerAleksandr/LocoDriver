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
import com.z_company.domain.track.ProfileWindow
import com.z_company.domain.track.SectionPicker
import com.z_company.domain.track.SectionSummary
import com.z_company.domain.track.ResolvedWarning
import com.z_company.domain.track.TrackWarning
import com.z_company.domain.track.TrackWarnings
import com.z_company.domain.track.RestrictionKind
import com.z_company.domain.track.RestrictionPass
import com.z_company.domain.track.SectionProfile
import com.z_company.domain.track.SignalAhead
import com.z_company.domain.track.SignalRules
import com.z_company.domain.track.TrackSignal
import com.z_company.domain.track.TravelParity
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
internal data class ProfileTrain(val id: String, val number: String?, val lengthM: Int?, val routeId: String = "")

/** Что показывать вместо графика, когда рисовать нечего. */
internal enum class ProfileEmpty { LOADING, NO_LOCATION, NO_SECTIONS, OFF_RAILS, NO_NEARBY, NO_DATA, NEED_LOGIN }

/**
 * Состояние «Профиля пути» на карте: загруженные участки, выбранный участок (по положению либо вручную), направление и
 * живая позиция на профиле. Координаты нигде не логируются, на сервер ничего не уходит.
 */
@Stable
internal class ProfileModel(
    private val repo: SectionProfileRepository,
    private val calibrations: ProfileCalibrationStore,
    private val scope: CoroutineScope,
    private val railLookup: com.z_company.domain.util.RailStationLookup? = null,
    private val warningStore: TrackWarningStore? = null,
    private val voice: WarningVoice? = null,
    val voiceSettings: WarningVoiceSettings? = null,
    /** Журнал проследования: «действующие на этом проезде» предупреждения привязаны к поезду и переживают закрытие карты. */
    private val journal: com.z_company.domain.track.RestrictionJournalStore? = null,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
) {
    /** Не вошёл в аккаунт: участки отдаются только вошедшему (25.10.2026), кэш прежнего аккаунта не показываем. */
    var needLogin by mutableStateOf(false)
        private set

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

    /** Едем по неправильному пути (чётное направление по нечётному и наоборот): показываются светофоры другого литера. */
    var wrongTrack by mutableStateOf(false)
        private set

    /** Поезд после ручного разворота: хвост по другую сторону от головы на виде (и на карте). */
    var trainFlipped by mutableStateOf(false)
        private set

    var live by mutableStateOf<LiveProfile?>(null)
        private set
    var position by mutableStateOf<LivePosition?>(null)
        private set

    /** Названия станций плеча поезда по порядку (отправление ... прибытие); меньше двух - плеча нет. */
    var stopNames: List<String> = emptyList()

    /** Станции плеча с координатами (карта поезда); по ним подбирается участок плеча без GPS-положения. */
    var stopPoints: List<com.z_company.domain.util.TrainMapStop> = emptyList()

    /** Крайние станции плеча по названиям: не зависят от того, есть ли у станции координаты ([stopNames] строится по станциям с координатами). */
    var legEnds: Pair<String, String>? = null
    private var lastFix: GeoFix? = null
    private var hasFix by mutableStateOf(false)

    /** Точка положения не на железнодорожных путях (по серверу: дальше ~60 м от рельсов); null - неизвестно (нет сети, лимит). */
    var offRails by mutableStateOf<Boolean?>(null)
        private set
    private var railCheckedAtMs = 0L
    private var railCheckedFix: GeoFix? = null
    private var motionLastKm: Double? = null
    private var motionSum = 0.0

    // --- Предупреждения на участке (ограничения скорости, токоприёмники, ток, прочее) ---

    /** Сохранённые предупреждения текущего участка (с прошлых проездов тоже). */
    var savedWarnings by mutableStateOf<List<TrackWarning>>(emptyList())
        private set
    private var loadedWarningsKey: String? = null
    private var activeWarningIds by mutableStateOf<Map<String, Set<String>>>(emptyMap())
    private var offeredSections by mutableStateOf<Set<String>>(emptySet())
    private val spokenAt = HashMap<String, Long>()
    private val passes = com.z_company.domain.track.WarningPassTracker()
    private var passDir = 0

    /**
     * Ключ хранения предупреждений: участок + направление + плечо (первая и последняя станции плеча). Предупреждения разных плеч
     * и направлений не смешиваются; у поезда без плеча ключ без плеча.
     */
    private val warningsKey: String?
        get() {
            val s = section ?: return null
            fun norm(n: String) = n.lowercase().filter { it.isLetterOrDigit() }
            val leg = if (stopNames.size >= 2) "${norm(stopNames.first())}>${norm(stopNames.last())}" else ""
            return "${s.id}|${if (reversed) "-" else "+"}|$leg"
        }

    /** Сохранённые предупреждения, действующие сейчас на выбранном участке. */
    val activeWarnings: List<ResolvedWarning>
        get() {
            val s = section ?: return emptyList()
            val ids = warningsKey?.let { activeWarningIds[it] } ?: return emptyList()
            return savedWarnings.filter { it.id in ids }.mapNotNull { TrackWarnings.resolve(s, it, stopPoints) }
        }

    fun isWarningActive(id: String): Boolean = warningsKey?.let { activeWarningIds[it] }?.contains(id) == true

    /** Участок выбран и поддерживает предупреждения. */
    val canEditWarnings: Boolean get() = live != null && warningStore != null

    /** Прошлые предупреждения этого участка, которые ещё не применили и не отклонили в этом проезде. */
    val warningOffer: List<TrackWarning>
        get() {
            val k = warningsKey ?: return emptyList()
            if (k in offeredSections || !activeWarningIds[k].isNullOrEmpty()) return emptyList()
            return savedWarnings
        }

    fun voices(): List<android.speech.tts.Voice> = voice?.voices().orEmpty()
    fun applyVoice() { voice?.apply() }
    fun testVoice() {
        voice?.apply()
        voice?.speak("Через 500 метров ограничение скорости 40 километров в час")
    }

    private fun syncWarnings() {
        val s = section ?: return
        val k = warningsKey ?: return
        if (loadedWarningsKey == k) return
        loadedWarningsKey = k
        savedWarnings = warningStore?.load(k, s.id).orEmpty()
        spokenAt.clear()
        passes.reset()
        adoptManualRestrictions(s)
        seedFromJournal(s.id)
    }

    /**
     * Ограничения, добавленные этому поезду без участка (нет профиля), с известными км/пикетом: когда профиль подходящего плечу участка
     * появился и такой километр на нём есть, ограничение переходит на участок (предупреждение сохраняется в хранилище участка, запись
     * журнала получает `sectionId`) - дальше оно отслеживается автоматически. Без км или с километром вне участка - остаётся как есть.
     */
    private fun adoptManualRestrictions(s: SectionProfile) {
        val t = train ?: return
        val j = journal ?: return
        if (stopNames.size >= 2 && LegMatch.fits(s, stopNames) != true) return
        // Без километровых отметок километр записи на оси не определить (resolve взял бы 0 км): такие участки ничего не подцепляют.
        if (s.kmMarks.isEmpty()) return
        val rejected = mutableListOf<String>()
        runCatching {
            for (e in j.forTrain(t.id)) {
                if (e.tracked || e.startAbsKm == null || e.endAbsKm == null) continue
                val w = TrackWarning(
                    id = e.warningId, sectionId = s.id, startAbsKm = e.startAbsKm, endAbsKm = e.endAbsKm, startRelKm = 0.0, endRelKm = 0.0,
                    type = com.z_company.domain.track.WarningType.entries.firstOrNull { RestrictionKind.of(it) == e.kind } ?: com.z_company.domain.track.WarningType.OTHER,
                    speedKmh = e.limitKmh, note = e.note, place = e.place,
                )
                val r = TrackWarnings.resolve(s, w, stopPoints) ?: continue
                // Серая зона/за краем оси: километраж неизвестен - ограничение на профиль не ставим, остаётся в журнале без участка.
                if (TrackWarnings.placementIssue(s, r) != null) {
                    if (noticedRejections.add(e.warningId)) rejected += e.place.ifBlank { "ограничение" }
                    continue
                }
                if (savedWarnings.none { it.id == w.id }) {
                    savedWarnings = savedWarnings + w
                    warningsKey?.let { warningStore?.save(it, savedWarnings) }
                }
                j.upsert(e.copy(sectionId = s.id, startRelKm = r.fromKm, endRelKm = r.toKm, updatedAt = maxOf(nowMs(), e.updatedAt + 1)))
            }
        }
        if (rejected.isNotEmpty()) notice = RejectNotice(rejected)
    }

    // --- Журнал проследования: связь предупреждений участка с поездом ---

    /**
     * Записи журнала этого поезда по участку: подтягиваем в «действующие» (под ключом хранения [warningsKey]: участок + направление + плечо -
     * по нему профиль читает действующие), чтобы выбор переживал закрытие карты и ограничения из «Поезда» были видны на карте. Запись
     * журнала с участком, которой нет в хранилище участка (подцеплена прежней версией или стёрлось хранилище), восстанавливается.
     */
    private fun seedFromJournal(sectionId: String) {
        val t = train ?: return
        val j = journal ?: return
        val k = warningsKey ?: return
        val s = section ?: return
        val all = runCatching { j.forTrain(t.id) }.getOrDefault(emptyList()).filter { it.sectionId == sectionId }
        if (all.isEmpty()) return
        fun warningOf(e: RestrictionPass) = TrackWarning(
            id = e.warningId, sectionId = sectionId, startAbsKm = e.startAbsKm, endAbsKm = e.endAbsKm,
            startRelKm = e.startRelKm, endRelKm = e.endRelKm,
            type = com.z_company.domain.track.WarningType.entries.firstOrNull { RestrictionKind.of(it) == e.kind } ?: com.z_company.domain.track.WarningType.OTHER,
            speedKmh = e.limitKmh, note = e.note, place = e.place,
        )
        // Подцеплено прежде в серую зону/за край оси (километраж неизвестен): снимаем с участка, остаётся в журнале без участка.
        val (entries, bad) = all.partition { e ->
            val r = TrackWarnings.resolve(s, warningOf(e), stopPoints)
            r != null && TrackWarnings.placementIssue(s, r) == null
        }
        if (bad.isNotEmpty()) {
            val badIds = bad.map { it.warningId }.toSet()
            runCatching {
                bad.forEach { e -> j.upsert(e.copy(sectionId = RestrictionPass.NO_SECTION, startRelKm = 0.0, endRelKm = 0.0, updatedAt = maxOf(nowMs(), e.updatedAt + 1))) }
            }
            if (savedWarnings.any { it.id in badIds }) {
                savedWarnings = savedWarnings.filter { it.id !in badIds }
                warningStore?.save(k, savedWarnings)
            }
            val fresh = bad.filter { noticedRejections.add(it.warningId) }.map { it.place.ifBlank { "ограничение" } }
            if (fresh.isNotEmpty()) notice = RejectNotice(fresh)
        }
        val missing = entries.filter { e -> savedWarnings.none { it.id == e.warningId } }.map(::warningOf)
        if (missing.isNotEmpty()) {
            savedWarnings = savedWarnings + missing
            warningStore?.save(k, savedWarnings)
        }
        activeWarningIds = activeWarningIds + (k to (activeWarningIds[k].orEmpty() + entries.map { it.warningId }))
        offeredSections = offeredSections + k
    }

    /** Предупреждение уже проследовано этим поездом: снять галочку нельзя (запись удаляется только из журнала поезда). */
    fun isWarningLocked(id: String): Boolean {
        val t = train ?: return false
        return runCatching { journal?.get(t.id, id)?.hasData }.getOrNull() == true
    }

    private fun attachToTrain(w: TrackWarning) {
        val t = train ?: return
        val j = journal ?: return
        if (t.routeId.isBlank()) return
        runCatching {
            val old = j.peek(t.id, w.id)
            val now = maxOf(nowMs(), (old?.updatedAt ?: 0L) + 1)
            if (old != null && !old.isDeleted) return@runCatching
            j.upsert(RestrictionPass.planned(t.id, t.routeId, w, now, t.lengthM))
        }
    }

    private fun detachFromTrain(id: String) {
        val t = train ?: return
        val j = journal ?: return
        runCatching {
            val cur = j.get(t.id, id) ?: return@runCatching
            if (!cur.hasData) j.remove(t.id, id, nowMs())
        }
    }

    private fun refreshSnapshot(w: TrackWarning) {
        val t = train ?: return
        val j = journal ?: return
        runCatching {
            val cur = j.get(t.id, w.id) ?: return@runCatching
            val fresh = RestrictionPass.planned(t.id, cur.routeId, w, maxOf(nowMs(), cur.updatedAt + 1), cur.trainLengthM)
            // Данные проследования сохраняются, обновляется только снимок самого предупреждения.
            j.upsert(
                cur.copy(
                    kind = fresh.kind, title = fresh.title, place = fresh.place, note = fresh.note, limitKmh = fresh.limitKmh,
                    startAbsKm = fresh.startAbsKm, endAbsKm = fresh.endAbsKm, startRelKm = fresh.startRelKm, endRelKm = fresh.endRelKm,
                    updatedAt = fresh.updatedAt,
                ),
            )
        }
    }

    /** Предупреждение, открытое для редактирования (нажатие на карточку на карте или на зону на профиле). */
    var editingWarning by mutableStateOf<TrackWarning?>(null)
        private set
    fun editWarning(id: String) { editingWarning = savedWarnings.firstOrNull { it.id == id } }
    fun closeWarningEdit() { editingWarning = null }

    fun updateWarning(w: TrackWarning) {
        val k = warningsKey ?: return
        savedWarnings = savedWarnings.map { if (it.id == w.id) w else it }
        warningStore?.save(k, savedWarnings)
        spokenAt.remove(w.id)
        passes.forget(w.id)
        refreshSnapshot(w)
    }

    fun addWarning(w: TrackWarning) {
        val k = warningsKey ?: return
        savedWarnings = savedWarnings + w
        warningStore?.save(k, savedWarnings)
        activeWarningIds = activeWarningIds + (k to (activeWarningIds[k].orEmpty() + w.id))
        offeredSections = offeredSections + k
        attachToTrain(w)
    }

    fun deleteWarning(id: String) {
        val k = warningsKey ?: return
        if (editingWarning?.id == id) editingWarning = null
        savedWarnings = savedWarnings.filter { it.id != id }
        passes.forget(id)
        detachFromTrain(id)
        warningStore?.save(k, savedWarnings)
        activeWarningIds = activeWarningIds + (k to (activeWarningIds[k].orEmpty() - id))
    }

    fun setWarningActive(id: String, on: Boolean) {
        val k = warningsKey ?: return
        val cur = activeWarningIds[k].orEmpty()
        if (!on && isWarningLocked(id)) return
        activeWarningIds = activeWarningIds + (k to if (on) cur + id else cur - id)
        passes.forget(id)
        if (on) savedWarnings.firstOrNull { it.id == id }?.let { attachToTrain(it) } else detachFromTrain(id)
        offeredSections = offeredSections + k
    }

    /** Применить выбранные из прошлых предупреждений (пустой набор - не нужны). */
    fun answerOffer(ids: Set<String>) {
        val k = warningsKey ?: return
        activeWarningIds = activeWarningIds + (k to ids)
        offeredSections = offeredSections + k
        savedWarnings.filter { it.id in ids }.forEach { attachToTrain(it) }
    }

    /** Озвучка на подходе: один раз за [WarningVoiceSettings.distanceM] до зоны, затем с периодом повтора, пока зона впереди. */
    private fun announceWarnings(p: LivePosition?, nowMs: Long) {
        val settings = voiceSettings ?: return
        val v = voice ?: return
        if (!settings.enabled || p == null || p.offRoute || !p.locked) return
        val dir = if (reversed) -1 else 1
        if (dir != passDir) { passDir = dir; passes.reset() }
        activeWarnings.forEach { r ->
            // Заход головы и (у ограничения скорости) выход всей длиной поезда; вход в зону озвучивается один раз.
            passes.update(r, p.kmAxis, dir, trainLengthKm).forEach { e ->
                when (e) {
                    com.z_company.domain.track.WarningEvent.ENTER -> v.speak(r.warning.spokenEnter(), append = true)
                    com.z_company.domain.track.WarningEvent.EXIT -> r.warning.spokenExit()?.let { v.speak(it, append = true) }
                }
            }
            val d = TrackWarnings.distanceAheadM(r, p.kmAxis, dir)
            val id = r.warning.id
            if (d == null || d > settings.distanceM * 1.15) { spokenAt.remove(id); return@forEach }
            if (d > settings.distanceM) return@forEach
            val last = spokenAt[id]
            val due = last == null || (settings.repeatSec > 0 && nowMs - last >= settings.repeatSec * 1000L)
            if (due) {
                spokenAt[id] = nowMs
                v.speak(r.warning.spoken(TrackWarnings.roundedMeters(d)))
            }
        }
    }

    /** Начало замера пройденного расстояния: точка, где нажали «Линейку». Положение на оси участка запоминается, если привязка была. */
    private class MeasureStart(val sectionId: String?, val axisKm: Double?, val fix: GeoFix)
    private var measureFrom by mutableStateOf<MeasureStart?>(null)

    /** Панель замера показана (кнопка-линейка в шапке); сам замер запускает «Старт» на панели. */
    var measureShown by mutableStateOf(false)
        private set

    fun toggleMeasurePanel() {
        if (measureShown) { resetMeasure(); measureShown = false } else measureShown = true
    }

    /** Замер остановлен кнопкой «Стоп»: число заморожено до «Сброс». */
    var measureStopped by mutableStateOf(false)
        private set

    /** Замер идёт (считает). */
    val measuring: Boolean get() = measureFrom != null && !measureStopped

    /** Есть замер - идёт или остановлен, число показывается. */
    val hasMeasure: Boolean get() = measureFrom != null

    /** Пройдено от точки начала замера, м; null - замера нет или нет положения. */
    var measuredM by mutableStateOf<Double?>(null)
        private set

    /** Расстояние считается по прямой от точки начала (участок сменился или привязки к оси нет), а не вдоль пути. */
    var measureApprox by mutableStateOf(false)
        private set

    /** Точка начала замера на оси выбранного участка, км; null - замера нет или он начат на другом участке. */
    val measureStartAxisKm: Double?
        get() = measureFrom?.takeIf { it.sectionId != null && it.sectionId == section?.id }?.axisKm

    /** Начать замер от текущего положения; без фикса GPS ничего не делает. */
    fun startMeasure() {
        val fix = lastFix ?: return
        val p = position?.takeIf { !it.offRoute && it.locked }
        measureFrom = MeasureStart(if (p != null) section?.id else null, p?.kmAxis, fix)
        measuredM = 0.0
        measureApprox = p == null
        measureStopped = false
    }

    /** «Стоп»: перестать считать, число остаётся на экране. */
    fun stopMeasure() {
        if (measureFrom != null) measureStopped = true
    }

    /** «Сброс»: убрать замер; следующий - «Старт». */
    fun resetMeasure() {
        measureFrom = null
        measuredM = null
        measureApprox = false
        measureStopped = false
    }

    private fun updateMeasure(fix: GeoFix, p: LivePosition?) {
        val from = measureFrom ?: return
        if (measureStopped) return
        val onAxis = from.axisKm != null && from.sectionId == section?.id && p != null && !p.offRoute && p.locked
        if (onAxis) {
            measuredM = abs(p!!.kmAxis - from.axisKm!!) * 1000.0
            measureApprox = false
        } else {
            val dLat = (fix.latitude - from.fix.latitude) * com.z_company.domain.track.FlatMetric.M_PER_DEG_LAT
            val dLon = (fix.longitude - from.fix.longitude) * com.z_company.domain.track.FlatMetric.kx(fix.latitude)
            measuredM = kotlin.math.sqrt(dLat * dLat + dLon * dLon)
            measureApprox = true
        }
    }

    val section: SectionProfile? get() = live?.section
    val reversed: Boolean get() = live?.reversed ?: false

    /** Есть ли у участка светофоры (иначе блок чётности и слой светофоров не показываются). */
    val hasSignals: Boolean get() = section?.signals?.isNotEmpty() == true

    /** Направление выбрано пользователем (иначе по плечу или по движению). */
    val directionIsManual: Boolean get() = manualReversed != null

    /** Сторона оси, в которую идёт поезд сейчас: +1 к росту км оси, −1 к убыли. */
    val travelAxisDir: Int get() = if (reversed) -1 else 1

    private val signalsKm: Double get() = position?.takeIf { !it.offRoute }?.kmAxis ?: 0.0

    /** Чётность направления движения по светофорам участка; null - светофоров нет. */
    val parity: TravelParity? get() = section?.let { SignalRules.parity(it.signals, signalsKm, travelAxisDir) }

    /** Светофоры, которые нужны сейчас: нужного литера для выбранного направления и пути. */
    val shownSignals: List<TrackSignal>
        get() {
            val s = section ?: return emptyList()
            val p = parity ?: return emptyList()
            return SignalRules.visible(s.signals, p, wrongTrack)
        }

    /** Ближайшие светофоры впереди по ходу (для строки «Сейчас»). */
    fun signalsAhead(limit: Int = 2): List<SignalAhead> {
        val s = section ?: return emptyList()
        val p = parity ?: return emptyList()
        val pos = position?.takeIf { !it.offRoute } ?: return emptyList()
        return SignalRules.ahead(s.signals, pos.kmAxis, travelAxisDir, p, wrongTrack, limit)
    }

    /** Плечо поезда заполнено (есть отправление и прибытие). */
    val hasLeg: Boolean get() = stopNames.size >= 2

    /** Подходит ли выбранный участок плечу; null - плеча нет. */
    val fitsLeg: Boolean? get() = section?.let { LegMatch.fits(it, stopNames) }

    val emptyReason: ProfileEmpty?
        get() = when {
            needLogin -> ProfileEmpty.NEED_LOGIN
            !loaded -> ProfileEmpty.LOADING
            summaries.isEmpty() && sections.isEmpty() -> ProfileEmpty.NO_SECTIONS
            section == null && !hasFix -> ProfileEmpty.NO_LOCATION
            section == null && loadingNear -> ProfileEmpty.LOADING
            section == null && offRails == true -> ProfileEmpty.OFF_RAILS
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
        needLogin = com.z_company.repository.remote_rest.RemoteRestClient.advisorBearerToken().isNullOrBlank()
        if (needLogin) {
            summaries = emptyList()
            sections = emptyList()
            loaded = true
            return
        }
        summaries = withContext(Dispatchers.Default) { repo.summaries(refresh = false) }
        if (summaries.isNotEmpty()) loaded = true
        val fresh = withContext(Dispatchers.Default) { repo.summaries(refresh = true) }
        if (fresh.isNotEmpty() || summaries.isEmpty()) summaries = fresh
        loaded = true
        lastFix?.let { ensureNear(it) }
    }

    /** Оси ближних участков (по габариту из списка; старый сервер без габарита - все). Не чаще, чем при сдвиге на 2 км. */
    private fun ensureNear(fix: GeoFix) {
        if (!loaded || loadingNear || needLogin) return
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

    private var legStationsKey: String? = null
    private var legStationsCache: List<com.z_company.domain.track.SectionStation> = emptyList()

    /**
     * Станции для подписей профиля: станции участка плюс станции плеча поезда (в том числе промежуточные, загруженные в справочник),
     * которых на участке ещё нет: их координаты проецируются на ось (не дальше [LEG_STATION_MAX_OFFSET_M] от неё).
     */
    fun stationsOnProfile(section: SectionProfile): List<com.z_company.domain.track.SectionStation> {
        val pts = stopPoints
        if (pts.isEmpty()) return section.stations
        val key = "${section.id}|${section.axisHash}|${pts.joinToString(",") { it.name }}"
        if (key != legStationsKey) {
            val extra = pts.filter { p -> section.stations.none { SectionPicker.sameStation(it.name, p.name) } }.mapNotNull { p ->
                val hit = section.axis.locator.project(p.latitude, p.longitude) ?: return@mapNotNull null
                if (hit.offsetM > LEG_STATION_MAX_OFFSET_M) null
                else com.z_company.domain.track.SectionStation(hit.kmM / 1000.0, p.name, p.latitude, p.longitude)
            }
            legStationsCache = (section.stations + extra).sortedBy { it.km }
            legStationsKey = key
        }
        return legStationsCache
    }

    /** У поезда выбрано плечо, но оно не проверено на карте (нет станций/координат): профиль подбирается хуже - пользователю нужна подсказка. */
    var legUnverified: Boolean = false

    /** Сообщение пользователю (например, ограничение не поставлено: километраж неизвестен); показывается диалогом и сбрасывается. */
    var notice by mutableStateOf<RejectNotice?>(null)

    /** Сервер отклонил загрузку профиля по лимиту (429): сообщения «проверьте интернет» были бы неверными. */
    val rateLimited: Boolean get() = repo.rateLimited

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
        if (section == null || position?.offRoute == true) checkRails(fix, timeMs) else offRails = false
        if (sections.isEmpty()) return
        if (manualId == null) autoPick(fix)
        ensureLive()
        val l = live ?: return
        val p = l.onFix(timeMs, fix.latitude, fix.longitude, speedKmh, fix.accuracyMeters)
        position = p
        detectMotionDirection(p)
        updateMeasure(fix, p)
        syncWarnings()
        announceWarnings(p, timeMs)
        // Участок выбран автоматически, а мы вне его оси - возможно, поезд уехал на другой участок: подберём заново.
        if (manualId == null && p?.offRoute == true) autoPick(fix, force = true)
    }

    /**
     * Проверка «на путях ли я» (когда участка рядом нет или мы ушли с его оси): запрос к серверу не чаще раза в 60 с и при сдвиге
     * не меньше 100 м; результат - [offRails]. Положение уходит только в этом запросе и не логируется.
     */
    private fun checkRails(fix: GeoFix, nowMs: Long) {
        val lookup = railLookup ?: return
        val prev = railCheckedFix
        if (prev != null) {
            val dLat = (fix.latitude - prev.latitude) * com.z_company.domain.track.FlatMetric.M_PER_DEG_LAT
            val dLon = (fix.longitude - prev.longitude) * com.z_company.domain.track.FlatMetric.kx(fix.latitude)
            val moved = dLat * dLat + dLon * dLon >= RAIL_RECHECK_M * RAIL_RECHECK_M
            if (!moved && nowMs - railCheckedAtMs < RAIL_RECHECK_MS) return
            if (nowMs - railCheckedAtMs < RAIL_RECHECK_MS) return
        }
        railCheckedAtMs = nowMs
        railCheckedFix = fix
        scope.launch {
            val r = runCatching { lookup.nearestAlongRail(fix.latitude, fix.longitude, 500.0, emptyList()) }.getOrNull()
            offRails = when (r) {
                is com.z_company.domain.util.RailLookupResult.NoStation -> if (r.reason == "off_rails") true else false
                is com.z_company.domain.util.RailLookupResult.Found -> false
                else -> null
            }
        }
    }

    /** Пользователь выбрал участок: автоподбор отключается, пока не вызван [useAuto]. Ось незагруженного участка подгружается. */
    fun choose(id: String) {
        chooseError = null
        if (sections.any { it.id == id }) { applyChoice(id); return }
        scope.launch {
            val loadedSection = runCatching { withContext(Dispatchers.Default) { repo.section(id, refresh = false) } }.getOrNull()
            if (loadedSection == null) {
                chooseError = if (repo.rateLimited) "Сервер временно ограничил загрузку профилей путей. Повторите через несколько минут."
                else "Не удалось загрузить участок. Проверьте интернет."
            } else {
                merge(listOf(loadedSection))
                applyChoice(id)
            }
        }
    }

    private fun applyChoice(id: String) {
        manualId = id
        wrongTrack = false
        manualReversed = null
        motionReversed = null
        trainFlipped = false
        resetMotion()
        rebuild()
    }

    fun useAuto() {
        chooseError = null
        wrongTrack = false
        manualId = null
        manualReversed = null
        motionReversed = null
        trainFlipped = false
        resetMotion()
        lastFix?.let { autoPick(it, force = true) }
        rebuild()
    }

    /** Окно ленты: сзади/спереди по умолчанию, но с запасом на длину поезда со стороны хвоста. */
    fun followWindow(live: LiveProfile, behindKm: Double, aheadKm: Double): ProfileWindow {
        val room = (trainLengthKm ?: 0.0) + 0.15
        return if (trainFlipped) live.window(behindKm, maxOf(aheadKm, room)) else live.window(maxOf(behindKm, room), aheadKm)
    }

    /** Мини-профиль: весь поезд у правого края ленты (при развороте хвост справа от головы - край за хвостом). */
    fun stripWindow(live: LiveProfile, widthKm: Double): ProfileWindow {
        val margin = 0.1
        val ahead = if (trainFlipped) (trainLengthKm ?: 0.0) + margin else margin
        return live.window(widthKm - ahead, ahead)
    }

    fun toggleDirection() {
        manualReversed = !reversed
        trainFlipped = !trainFlipped
        rebuild()
    }

    /** Направление по чётности: выбор пользователя («Чётное»/«Нечётное»), поверх плеча и движения. */
    fun chooseParity(target: TravelParity) {
        val s = section ?: return
        val dir = SignalRules.axisDir(s.signals, signalsKm, target) ?: return
        val wantReversed = dir < 0
        if (wantReversed != reversed) trainFlipped = !trainFlipped
        manualReversed = wantReversed
        rebuild()
    }

    /** Вернуть автоматическое направление (по плечу поезда, а без плеча - по движению). */
    fun useAutoDirection() {
        if (manualReversed == null) return
        val before = reversed
        manualReversed = null
        motionReversed = null
        resetMotion()
        rebuild()
        if (reversed != before) trainFlipped = !trainFlipped
    }

    fun chooseWrongTrack(wrong: Boolean) { wrongTrack = wrong }

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
            if (cur?.section?.id != s.id) wrongTrack = false
            val keep = cur?.takeIf { it.section.id == s.id }?.position
            live = LiveProfile(s, rev).also { it.applyCalibration(calibrations.get(s.id, s.axisHash)) }
            position = keep ?: live?.position
            calibrationKm = live?.calibrationKm ?: 0.0
            syncWarnings()
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
        /** Ограничения, о которых уже сообщили «километраж неизвестен» (раз за запуск приложения, чтобы не повторять при каждом открытии). */
        val noticedRejections = HashSet<String>()

        /** Станция плеча попадает на профиль, если её координаты не дальше этого от оси участка. */
        const val LEG_STATION_MAX_OFFSET_M = 500.0
        const val MOTION_KM = 0.3
        const val MAX_STEP_KM = 1.0
        const val NEAR_RELOAD_M = 2_000.0
        const val RAIL_RECHECK_M = 100.0
        const val RAIL_RECHECK_MS = 60_000L
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
    val railLookup: com.z_company.domain.util.RailStationLookup = koinInject()
    val voiceSettings = remember { WarningVoiceSettings(context) }
    val voice = remember { WarningVoice(context, voiceSettings) }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { voice.shutdown() } }
    val journal: com.z_company.domain.track.RestrictionJournalStore = koinInject()
    val model = remember {
        ProfileModel(repo, ProfileCalibrationStore(context), scope, railLookup, TrackWarningStore(context), voice, voiceSettings, journal)
    }
    model.stopNames = remember(stops) { stops.map { it.name }.filter { it.isNotBlank() } }
    model.stopPoints = stops
    LaunchedEffect(trains, trainId) { model.initTrains(trains, trainId) }
    LaunchedEffect(Unit) { model.load() }
    LaunchedEffect(fix, fixAtMs, model.loaded) { model.onFix(fix, speedKmh, if (fixAtMs > 0) fixAtMs else System.currentTimeMillis()) }
    return model
}

/** Ограничения, не поставленные на профиль из-за неизвестного километража: места для заголовка и текста диалога. */
internal class RejectNotice(val places: List<String>) {
    val title: String get() = if (places.size == 1) "Не поставлено: ${places.first()}" else "Не поставлено ограничений: ${places.size}"
    val text: String
        get() = TrackWarnings.UNKNOWN_KM_MESSAGE + if (places.size > 1) "\n\n" + places.joinToString("\n") { "• $it" } else ""
}
