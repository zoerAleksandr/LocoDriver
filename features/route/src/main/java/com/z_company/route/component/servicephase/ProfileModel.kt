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
import com.z_company.domain.track.WarningType
import com.z_company.domain.track.latLonAt
import com.z_company.domain.track.TrackWarnings
import com.z_company.domain.track.RestrictionKind
import com.z_company.domain.track.RestrictionPass
import com.z_company.domain.track.SectionProfile
import com.z_company.domain.track.SignalAhead
import com.z_company.domain.track.SignalRules
import com.z_company.domain.track.SignalTrack
import com.z_company.domain.track.TrackSignal
import com.z_company.domain.track.TravelParity
import com.z_company.domain.util.GeoFix
import com.z_company.domain.util.TrainMapStop
import com.z_company.domain.track.LegWarnings
import com.z_company.repository.remote_rest.tracks.SectionProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import kotlin.math.abs

/** Длина поезда, введённая вручную на профиле (SharedPreferences `profile_train_length`, ключ - id поезда); на сервер не уходит. */
internal class TrainLengthStore(context: android.content.Context) {
    private val prefs = context.applicationContext.getSharedPreferences("profile_train_length", android.content.Context.MODE_PRIVATE)
    fun get(trainId: String): Int? = prefs.getInt(trainId, 0).takeIf { it > 0 }
    fun set(trainId: String, meters: Int?) {
        prefs.edit().apply { if (meters == null) remove(trainId) else putInt(trainId, meters) }.apply()
    }
}

/** Отказ от предложения прошлых ДУ-61 (SharedPreferences `profile_offer_declined`, ключ «поезд|участок»): пока поезд тот же, не спрашиваем снова. */
internal class OfferDeclineStore(context: android.content.Context) {
    private val prefs = context.applicationContext.getSharedPreferences("profile_offer_declined", android.content.Context.MODE_PRIVATE)
    fun all(): Set<String> = prefs.all.keys.toSet()
    fun add(key: String) { prefs.edit().putBoolean(key, true).apply() }
}

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
    /** Журнал проследования: «действующие» предупреждения привязаны к поезду и переживают закрытие карты. */
    private val journal: com.z_company.domain.track.RestrictionJournalStore? = null,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
    private val lengthStore: TrainLengthStore? = null,
    private val declineStore: OfferDeclineStore? = null,
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
    val trainLengthKm: Double? get() = train?.let { trainLengthM(it) }?.let { it / 1000.0 }

    private var lengthTick by mutableStateOf(0)

    /** Длина поезда, м: из данных поезда (приоритетна), иначе введённая вручную на профиле; null - не указана. */
    fun trainLengthM(t: ProfileTrain): Int? {
        t.lengthM?.takeIf { it > 0 }?.let { return it }
        lengthTick
        return lengthStore?.get(t.id)
    }

    /** Ввести длину поезда вручную (null - сбросить). Действует, пока длины нет в данных поезда. */
    fun setManualLength(trainId: String, meters: Int?) {
        lengthStore?.set(trainId, meters)
        lengthTick++
    }
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

    /**
     * Литер светофоров, выбранный переключателем 1П/2П (1П - нечётные, 2П - чётные); null - по умолчанию. Нужен для проезда
     * по неправильному пути (чётное направление по нечётному пути и наоборот): светофоры того пути стоят к поезду спиной.
     */
    var manualSignalTrack by mutableStateOf<SignalTrack?>(null)
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
    var hasFix by mutableStateOf(false)
        private set

    /** Точка положения не на железнодорожных путях (по серверу: дальше ~60 м от рельсов); null - неизвестно (нет сети, лимит). */
    var offRails by mutableStateOf<Boolean?>(null)
        private set
    private var railCheckedAtMs = 0L
    private var railCheckedFix: GeoFix? = null
    private var motionLastKm: Double? = null
    private var motionSum = 0.0

    // --- Предупреждения (ДУ-61) ---
    //
    // ДЕЙСТВУЮЩИЕ ограничения - один источник: журнал поезда ([journal]). Его же читают «Ограничения поезда», профиль, карта, озвучка и
    // фоновая запись «АВТО»; добавлять, править и убирать можно откуда угодно, везде видно одно и то же. ПРОШЛЫЕ ограничения участка -
    // отдельная история ([TrackWarningStore], по участку): из неё ограничение снова делают действующим, а снятие с поезда её не стирает.

    /** Записи журнала выбранного поезда (не удалённые): обновляются опросом [refreshJournal] и сразу после правок отсюда. */
    private var journalEntries by mutableStateOf<List<RestrictionPass>>(emptyList())

    /** История участка (все ключи плечо/направление): прошлые и действующие; для показа вычитаются действующие ([pastWarnings]). */
    private var history by mutableStateOf<List<TrackWarning>>(emptyList())
    private var historySectionId: String? = null
    private var offeredSections by mutableStateOf<Set<String>>(emptySet())

    /** Участки, от предложения на которых отказались для конкретного поезда («поезд|участок»): переживает перезапуск и повторную прокладку маршрута. */
    private var declinedOffers by mutableStateOf(declineStore?.all().orEmpty())
    private fun declineKey(): String? {
        val t = train?.id ?: return null
        val s = section ?: return null
        return "$t|${s.id}"
    }
    private val spokenAt = HashMap<String, Long>()
    private val passes = com.z_company.domain.track.WarningPassTracker()
    private var passDir = 0

    /** Ключ записи истории: участок + направление + плечо (первая и последняя станции плеча); читается история всех ключей участка. */
    private val warningsKey: String?
        get() {
            val s = section ?: return null
            fun norm(n: String) = n.lowercase().filter { it.isLetterOrDigit() }
            val leg = if (stopNames.size >= 2) "${norm(stopNames.first())}>${norm(stopNames.last())}" else ""
            return "${s.id}|${if (reversed) "-" else "+"}|$leg"
        }

    private fun warningOf(e: RestrictionPass) = TrackWarning(
        id = e.warningId, sectionId = e.sectionId, startAbsKm = e.startAbsKm, endAbsKm = e.endAbsKm,
        startRelKm = e.startRelKm, endRelKm = e.endRelKm,
        type = com.z_company.domain.track.WarningType.entries.firstOrNull { RestrictionKind.of(it) == e.kind } ?: com.z_company.domain.track.WarningType.OTHER,
        speedKmh = e.limitKmh, note = e.note, place = e.place,
    )

    /** Все действующие ограничения поезда (на любых участках плеча и без участка): тот же список, что в «Ограничениях поезда». */
    val trainRestrictions: List<RestrictionPass> get() = journalEntries

    /** Правка ограничения, лежащего не на выбранном участке (или без участка): так же, как в «Ограничениях поезда». */
    fun saveJournalEdit(e: RestrictionPass, w: TrackWarning) {
        val j = journal ?: return
        val store = warningStore ?: return
        runCatching { applyRestrictionEdit(j, store, e, w, nowMs()) }
        reloadJournal()
        scope.launch { refreshJournal() }
    }

    /** Действующие ограничения этого поезда на выбранном участке (то, что в журнале поезда с этим участком). */
    val activeList: List<TrackWarning>
        get() {
            val s = section ?: return emptyList()
            return journalEntries.filter { it.sectionId == s.id }.map(::warningOf)
        }

    /** Действующие ограничения, разложенные по оси выбранного участка (профиль, карта, озвучка). */
    val activeWarnings: List<ResolvedWarning>
        get() {
            val s = section ?: return emptyList()
            return activeList.mapNotNull { TrackWarnings.resolve(s, it, stopPoints) }
        }

    /**
     * Действующие ограничения поезда на осях СВОИХ участков (карта рисует все, а не только выбранный участок: плечо состоит из нескольких
     * участков). Участок ограничения должен быть загружен - [refreshJournal] подгружает недостающие.
     */
    val mapWarnings: List<Pair<SectionProfile, ResolvedWarning>>
        get() = journalEntries.mapNotNull { e ->
            if (!e.tracked) return@mapNotNull null
            val s = sections.firstOrNull { it.id == e.sectionId } ?: return@mapNotNull null
            val r = TrackWarnings.resolve(s, warningOf(e), stopPoints) ?: return@mapNotNull null
            if (TrackWarnings.placementIssue(s, r) != null) null else s to r
        }

    /** Прошлые ограничения участка: были добавлены раньше, сейчас на поезде не действуют. */
    val pastWarnings: List<TrackWarning>
        get() {
            val ids = activeList.map { it.id }.toSet()
            return history.filter { it.id !in ids }
        }

    fun isWarningActive(id: String): Boolean = journalEntries.any { it.warningId == id }

    /** Участок выбран и поддерживает предупреждения. */
    val canEditWarnings: Boolean get() = live != null && warningStore != null

    /**
     * Прошлые ограничения участка, которые ещё не применили и не отклонили в этом проезде (предложение при открытии участка).
     * Предлагаем только когда плечо указано в разделе «Поезд» (есть минимум две станции): при автоопределении участка
     * по положению направление ещё может быть неверным, и предложение подставило бы чужие ограничения. Из прошлых берём только
     * лежащие на станциях и перегонах плеча и в том же направлении ([LegWarnings]).
     */
    val warningOffer: List<TrackWarning>
        get() {
            if (stopNames.size < 2) return emptyList()
            if (declineKey() in declinedOffers) return emptyList()
            val k = warningsKey ?: return emptyList()
            if (k in offeredSections || activeList.isNotEmpty()) return emptyList()
            return pastWarnings.filter { LegWarnings.matches(it.place, stopNames) }
        }

    fun voices(): List<android.speech.tts.Voice> = voice?.voices().orEmpty()
    fun applyVoice() { voice?.apply() }
    fun testVoice() {
        voice?.apply()
        voice?.speak("Через 500 метров ограничение скорости на 40")
    }

    /** Синхронизация с хранилищами: история участка и журнал поезда; зовётся при смене участка и опросом (журнал меняют и другие экраны). */
    private fun syncWarnings() {
        val s = section ?: return
        if (historySectionId != s.id) {
            historySectionId = s.id
            spokenAt.clear()
            passes.reset()
            history = warningStore?.loadAll(s.id).orEmpty()
        }
        scope.launch { refreshJournal() }
    }

    private var refreshing = false

    /**
     * Перечитать журнал поезда и свести его с выбранным участком: ограничения без участка (добавлены из «Поезда») с известными км
     * подцепляются к участку, подходящему плечу, и попадают в историю участка; ограничение, которое на участке не помещается
     * (серая зона, за краем оси), снимается с участка и остаётся в журнале без участка. Зовётся опросом раз в пару секунд.
     */
    suspend fun refreshJournal() {
        val t = train ?: return
        val j = journal ?: return
        if (refreshing) return
        refreshing = true
        try {
            val s = section
            val result = withContext(Dispatchers.Default) { reconcile(s, t, j) }
            if (result.entries != journalEntries) journalEntries = result.entries
            loadWarningSections(result.entries)
            if (result.history != null) history = result.history
            if (result.rejected.isNotEmpty()) notice = RejectNotice(result.rejected)
        } finally {
            refreshing = false
        }
    }

    private val requestedSections = HashSet<String>()

    /** Участки ограничений поезда, которых ещё нет среди загруженных (другие участки плеча): подгружаются, чтобы карта показала все ограничения. */
    private fun loadWarningSections(entries: List<RestrictionPass>) {
        val need = entries.filter { it.tracked }.map { it.sectionId }.distinct()
            .filter { id -> sections.none { it.id == id } && requestedSections.add(id) }
        if (need.isEmpty()) return
        scope.launch {
            val found = need.mapNotNull { id ->
                runCatching { withContext(Dispatchers.Default) { repo.section(id, refresh = false) } }.getOrNull()
                    .also { if (it == null) requestedSections.remove(id) }
            }
            merge(found)
        }
    }

    private class Reconciled(val entries: List<RestrictionPass>, val history: List<TrackWarning>?, val rejected: List<String>)

    private fun reconcile(s: SectionProfile?, t: ProfileTrain, j: com.z_company.domain.track.RestrictionJournalStore): Reconciled {
        var entries = runCatching { j.forTrain(t.id) }.getOrDefault(emptyList())
        if (s == null) return Reconciled(entries, null, emptyList())
        val fitsLeg = stopNames.size < 2 || LegMatch.fits(s, stopNames) == true
        val rejected = mutableListOf<String>()
        val onSection = mutableListOf<TrackWarning>()
        var changed = false
        for (e in entries) {
            if (e.tracked && e.sectionId != s.id) continue
            if (!e.tracked && (!fitsLeg || s.kmMarks.isEmpty() || e.startAbsKm == null || e.endAbsKm == null)) continue
            val w = warningOf(e).copy(sectionId = s.id)
            val r = TrackWarnings.resolve(s, w, stopPoints)
            // Серая зона/за краем оси: километраж неизвестен - на профиль не ставим, остаётся в журнале без участка.
            if (r == null || TrackWarnings.placementIssue(s, r) != null) {
                if (e.tracked) {
                    runCatching { j.upsert(e.copy(sectionId = RestrictionPass.NO_SECTION, startRelKm = 0.0, endRelKm = 0.0, updatedAt = maxOf(nowMs(), e.updatedAt + 1))) }
                    changed = true
                }
                if (noticedRejections.add(e.warningId)) rejected += e.place.ifBlank { "ограничение" }
                continue
            }
            if (!e.tracked) {
                runCatching { j.upsert(e.copy(sectionId = s.id, startRelKm = r.fromKm, endRelKm = r.toKm, updatedAt = maxOf(nowMs(), e.updatedAt + 1))) }
                changed = true
            }
            onSection += w
        }
        if (changed) entries = runCatching { j.forTrain(t.id) }.getOrDefault(entries)
        // Действующие на участке всегда есть в истории участка: правка/снятие с поезда не должны их терять.
        val store = warningStore
        val key = warningsKey
        var hist: List<TrackWarning>? = null
        if (store != null && key != null) {
            val known = store.loadAll(s.id)
            val missing = onSection.filter { w -> known.none { it.id == w.id } }
            if (missing.isNotEmpty()) store.save(key, store.load(key, s.id) + missing)
            hist = if (missing.isEmpty()) known else store.loadAll(s.id)
        }
        return Reconciled(entries, hist, rejected)
    }

    /** Предупреждение уже проследовано этим поездом (есть записанные время и скорость): при снятии их надо предупредить об удалении. */
    fun hasPassData(id: String): Boolean = journalEntries.firstOrNull { it.warningId == id }?.hasData == true

    private fun reloadJournal() {
        val t = train ?: return
        val j = journal ?: return
        journalEntries = runCatching { j.forTrain(t.id) }.getOrDefault(journalEntries)
    }

    private fun attachToTrain(w: TrackWarning) {
        val t = train ?: return
        val j = journal ?: return
        runCatching {
            val old = j.peek(t.id, w.id)
            val now = maxOf(nowMs(), (old?.updatedAt ?: 0L) + 1)
            if (old != null && !old.isDeleted) return@runCatching
            j.upsert(RestrictionPass.planned(t.id, t.routeId, w, now, t.lengthM))
        }
        reloadJournal()
    }

    private fun detachFromTrain(id: String) {
        val t = train ?: return
        val j = journal ?: return
        runCatching {
            val cur = j.peek(t.id, id) ?: return@runCatching
            j.remove(t.id, id, maxOf(nowMs(), cur.updatedAt + 1))
        }
        reloadJournal()
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
        reloadJournal()
    }

    /** Предупреждение, открытое для редактирования (нажатие на карточку на карте или на зону на профиле). */
    var editingWarning by mutableStateOf<TrackWarning?>(null)
        private set
    /** Ограничение на другом участке плеча (или без участка), открытое для правки: форма та же, что в «Ограничениях поезда». */
    var editingEntry by mutableStateOf<RestrictionPass?>(null)
        private set

    fun editWarning(id: String) {
        val own = (activeList + pastWarnings).firstOrNull { it.id == id }
        if (own != null) { editingWarning = own; return }
        editingEntry = journalEntries.firstOrNull { it.warningId == id }
    }

    fun closeWarningEdit() { editingWarning = null; editingEntry = null }

    /** Убрать из «Ограничений поезда»-образного списка запись, открытую в [editingEntry]. */
    fun removeEntry(e: RestrictionPass) { if (editingEntry?.warningId == e.warningId) editingEntry = null; deleteWarning(e.warningId) }

    /** Правка: история участка обновляется во всех ключах, снимок в журнале поезда (если ограничение действует) - тоже. */
    fun updateWarning(w: TrackWarning) {
        val s = section ?: return
        warningStore?.updateEverywhere(w.copy(sectionId = s.id))
        history = warningStore?.loadAll(s.id).orEmpty()
        spokenAt.remove(w.id)
        passes.forget(w.id)
        refreshSnapshot(w)
    }

    /** Новое ограничение: попадает в историю участка и сразу действует на поезде. */
    fun addWarning(w: TrackWarning) {
        val k = warningsKey ?: return
        val s = section ?: return
        warningStore?.save(k, (warningStore.load(k, s.id) + w).distinctBy { it.id })
        history = warningStore?.loadAll(s.id).orEmpty()
        offeredSections = offeredSections + k
        attachToTrain(w)
    }

    /** Убрать ограничение с поезда (из «Ограничений поезда» его тоже не будет, записанные время и скорость - тоже); в истории участка оно остаётся. */
    fun deleteWarning(id: String) {
        if (editingWarning?.id == id) editingWarning = null
        passes.forget(id)
        detachFromTrain(id)
    }

    /** Стереть прошлое ограничение из истории участка. */
    fun forgetPast(id: String) {
        val s = section ?: return
        warningStore?.removeEverywhere(s.id, id)
        history = warningStore?.loadAll(s.id).orEmpty()
    }

    /** Сделать прошлое ограничение действующим на этом поезде. */
    fun activatePast(id: String) {
        val w = history.firstOrNull { it.id == id } ?: return
        passes.forget(id)
        attachToTrain(w.copy(sectionId = section?.id ?: w.sectionId))
        warningsKey?.let { offeredSections = offeredSections + it }
    }

    /** Применить выбранные из прошлых предупреждений (пустой набор - не нужны). */
    fun answerOffer(ids: Set<String>) {
        val k = warningsKey ?: return
        offeredSections = offeredSections + k
        declineKey()?.let { declineStore?.add(it); declinedOffers = declinedOffers + it }
        ids.forEach { activatePast(it) }
    }

    /** Озвучка на подходе: один раз за [WarningVoiceSettings.distanceM] до зоны, затем с периодом повтора, пока зона впереди. */
    private fun announceWarnings(p: LivePosition?, nowMs: Long) {
        val settings = voiceSettings ?: return
        val v = voice ?: return
        if (!settings.enabled || p == null || p.offRoute || !p.locked) return
        val dir = if (reversed) -1 else 1
        if (dir != passDir) { passDir = dir; passes.reset() }
        var progressed = false
        activeWarnings.forEach { r ->
            // Заход головы и (у ограничения скорости) выход всей длиной поезда; вход в зону озвучивается один раз.
            // Порядок речи задаёт очередь голоса: заход, затем впереди лежащее, затем выход, затем «больше предупреждений нет».
            passes.update(r, p.kmAxis, dir, trainLengthKm).forEach { e ->
                progressed = true
                when (e) {
                    com.z_company.domain.track.WarningEvent.ENTER -> v.speak(r.warning.spokenEnter(), com.z_company.domain.track.WarningSpeechPriority.ENTER)
                    com.z_company.domain.track.WarningEvent.EXIT -> r.warning.spokenExit()?.let { v.speak(it, com.z_company.domain.track.WarningSpeechPriority.EXIT) }
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
                v.speak(r.warning.spoken(TrackWarnings.roundedMeters(d)), com.z_company.domain.track.WarningSpeechPriority.AHEAD)
            }
        }
        // Последнее предупреждение проследовано (в этом такте что-то закончилось и больше впереди ничего нет): один раз сообщаем.
        if (progressed && activeWarnings.isNotEmpty() && activeWarnings.all { passes.isDone(it.warning.id) }) {
            v.speak(com.z_company.domain.track.WARNING_SPEECH_NO_MORE, com.z_company.domain.track.WarningSpeechPriority.NO_MORE)
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
            val t = signalTrack
            return s.signals.filter { it.track == t }
        }

    /** Ближайшие светофоры впереди по ходу (для строки «Сейчас»). */
    fun signalsAhead(limit: Int = 2): List<SignalAhead> {
        val s = section ?: return emptyList()
        val pos = position?.takeIf { !it.offRoute } ?: return emptyList()
        return SignalRules.aheadOfTrack(s.signals, pos.kmAxis, travelAxisDir, signalTrack, limit)
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
        if (System.currentTimeMillis() < nearRetryAtMs) return
        loadingNear = true
        scope.launch {
            // Сначала то, что уже сохранено на устройстве (мгновенно, без сети), потом - докачка/обновление: при слабой сети профиль
            // появляется сразу из кэша, а не после таймаута запроса.
            val cachedNear = runCatching { withContext(Dispatchers.Default) { repo.sectionsNear(fix.latitude, fix.longitude, cacheOnly = true) } }
                .getOrDefault(emptyList())
            if (cachedNear.isNotEmpty()) { merge(cachedNear); rebuild() }
            val found = runCatching { withContext(Dispatchers.Default) { repo.sectionsNear(fix.latitude, fix.longitude) } }.getOrDefault(emptyList())
            merge(found)
            // Пусто (сбой сети/сервера или правда нет участков рядом): центр не запоминаем - через 30 с попробуем снова, а не ждём
            // сдвига на 2 км (раньше сбой оставлял профиль пустым до повторного входа в карту).
            if (found.isNotEmpty() || cachedNear.isNotEmpty()) { nearCenter = fix; nearRetryAtMs = 0L }
            else nearRetryAtMs = System.currentTimeMillis() + NEAR_RETRY_MS
            loadingNear = false
            rebuild()
        }
    }

    private var nearRetryAtMs = 0L

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
        manualSignalTrack = null
        manualReversed = null
        motionReversed = null
        trainFlipped = false
        resetMotion()
        rebuild()
    }

    fun useAuto() {
        chooseError = null
        manualSignalTrack = null
        manualId = null
        manualReversed = null
        motionReversed = null
        trainFlipped = false
        resetMotion()
        lastFix?.let { autoPick(it, force = true) }
        rebuild()
    }

    /**
     * Расстояние до точки [target] вдоль режимной карты (оси выбранного участка), м: положение на участке определено, а точка лежит
     * на оси (до 500 м) впереди по ходу. Запасной путь, когда линия плеча с сервера не даёт расстояния (положение дальше 50 м от неё, линии нет):
     * по режимной карте станции и положение известны. null - посчитать нельзя или точка позади.
     */
    fun axisDistanceTo(target: com.z_company.domain.util.PlanTarget): Double? {
        val l = live ?: return null
        val p = position?.takeIf { !it.offRoute && it.locked } ?: return null
        val hit = l.section.axis.locator.project(target.latitude, target.longitude) ?: return null
        if (hit.offsetM > com.z_company.domain.track.TrackWarnings.STATION_MAX_OFFSET_M) return null
        // Положение показывается с ручной поправкой по оси, точка - по самой оси.
        val targetKm = (hit.kmM / 1000.0 + l.calibrationKm).coerceIn(0.0, l.lengthKm)
        val d = (l.toView(targetKm) - l.toView(p.kmAxis)) * 1000.0
        return d.takeIf { it > com.z_company.domain.util.ArrivalPlan.ARRIVED_M }
    }

    /**
     * Ограничения скорости впереди по ходу для расчёта прибытия: расстояние от головы поезда, конец зоны продлён на длину поезда
     * (ограничение действует, пока не проследует хвост). Зона позади (хвост прошёл) не попадает в список.
     *
     * Если положение [fix] на линии плеча [line], берутся ограничения ВСЕХ загруженных участков: границы зоны переводятся с оси своего
     * участка в координаты и проецируются на линию плеча, как и станции цели. Иначе (линии нет, вне путей) - запасной путь по оси
     * выбранного участка: только ограничения этого участка.
     */
    fun speedZonesAhead(
        line: List<Pair<Double, Double>>?,
        fix: com.z_company.domain.util.GeoFix?,
    ): SpeedZonesResult {
        val lenM = (trainLengthKm ?: 0.0) * 1000.0
        var reason: String? = when {
            line == null || line.size < 2 -> "линия пути плеча не загружена"
            fix == null -> "положение не определено"
            else -> null
        }
        if (line != null && line.size >= 2 && fix != null) {
            val cum = com.z_company.domain.util.TrainMapRoute.cumulativeM(line)
            val me = com.z_company.domain.util.TrainMapRoute.project(line, cum, fix.latitude, fix.longitude)
            if (me == null || me.offsetM > com.z_company.domain.util.TrainMapRoute.SNAP_TOLERANCE_M) {
                reason = "положение дальше 50 м от рельсового пути"
            } else {
                return SpeedZonesResult(mapWarnings.mapNotNull { (sec, r) ->
                    val v = r.warning.speedKmh?.takeIf { r.warning.type == WarningType.SPEED && it > 0 } ?: return@mapNotNull null
                    val pa = sec.axis.latLonAt(r.fromKm) ?: return@mapNotNull null
                    val pb = sec.axis.latLonAt(r.toKm) ?: return@mapNotNull null
                    val a = com.z_company.domain.util.TrainMapRoute.project(line, cum, pa.first, pa.second) ?: return@mapNotNull null
                    val b = com.z_company.domain.util.TrainMapRoute.project(line, cum, pb.first, pb.second) ?: return@mapNotNull null
                    // Ось участка редкая и местами уходит с путей; зона не на линии плеча - не наша.
                    if (a.offsetM > ZONE_ON_LINE_M || b.offsetM > ZONE_ON_LINE_M) return@mapNotNull null
                    val start = minOf(a.alongM, b.alongM) - me.alongM
                    val end = maxOf(a.alongM, b.alongM) - me.alongM + lenM
                    if (end <= 0) null else com.z_company.domain.util.ArrivalPlan.SpeedZone(start, end, v.toDouble())
                })
            }
        }
        // Запасной путь: ось выбранного участка. Предупреждаем, только если скоростные ограничения вообще есть.
        val hasSpeed = journalEntries.any { it.tracked && warningOf(it).let { w -> w.type == WarningType.SPEED && (w.speedKmh ?: 0) > 0 } }
        val caveat = if (hasSpeed) reason else null
        val l = live ?: return SpeedZonesResult(emptyList(), caveat?.let { noteFor(it, partial = false) })
        val p = position?.takeIf { !it.offRoute && it.locked } ?: return SpeedZonesResult(emptyList(), caveat?.let { noteFor(it, partial = false) })
        val head = l.toView(p.kmAxis)
        return SpeedZonesResult(activeWarnings.mapNotNull { r ->
            val v = r.warning.speedKmh?.takeIf { r.warning.type == WarningType.SPEED && it > 0 } ?: return@mapNotNull null
            val a = l.toView(r.fromKm)
            val b = l.toView(r.toKm)
            val start = (minOf(a, b) - head) * 1000.0
            val end = (maxOf(a, b) - head) * 1000.0 + lenM
            if (end <= 0) null else com.z_company.domain.util.ArrivalPlan.SpeedZone(start, end, v.toDouble())
        }, caveat?.let { noteFor(it, partial = true) })
    }

    private fun noteFor(reason: String, partial: Boolean): String =
        if (partial) "Ограничения учтены не полностью: $reason. Учтены только ограничения выбранного участка профиля, расчёт может быть неточным."
        else "Ограничения скорости не учтены: $reason. Расчёт может быть неточным."

    /** Станции плеча впереди по режимной карте ([axisDistanceTo]) с расстоянием по оси участка. */
    fun axisStopsAhead(stops: List<TrainMapStop>): List<com.z_company.domain.util.ArrivalPlan.StopAhead> =
        stops.mapNotNull { st ->
            val t = com.z_company.domain.util.PlanTarget(st.latitude, st.longitude, st.name.takeIf { it.isNotBlank() })
            axisDistanceTo(t)?.let { com.z_company.domain.util.ArrivalPlan.StopAhead(st, t, it) }
        }

    /** Окно ленты: сзади/спереди по умолчанию, но с запасом на длину поезда со стороны хвоста. */
    fun followWindow(live: LiveProfile, behindKm: Double, aheadKm: Double): ProfileWindow {
        val room = (trainLengthKm ?: 0.0) + 0.15
        return if (trainFlipped) live.window(behindKm, maxOf(aheadKm, room)) else live.window(maxOf(behindKm, room), aheadKm)
    }

    /**
     * Мини-профиль: поезд целиком у ближнего к хвосту края ленты, остальная ширина - профиль впереди по ходу
     * (при развороте хвост справа от головы - запас справа, впереди слева).
     */
    fun stripWindow(live: LiveProfile, widthKm: Double): ProfileWindow {
        val tailSide = ((trainLengthKm ?: 0.0) + 0.15).coerceIn(0.3, widthKm / 2)
        return if (trainFlipped) live.window(widthKm - tailSide, tailSide) else live.window(tailSide, widthKm - tailSide)
    }

    fun toggleDirection() {
        // Вид зеркалится сам (reversed): поезд идёт к +x вида, хвост слева - переворачивать хвост ещё раз нельзя (он вернётся на прежнюю сторону оси).
        manualReversed = !reversed
        rebuild()
    }

    /** Направление по чётности: выбор пользователя («Чётное»/«Нечётное»), поверх плеча и движения. */
    fun chooseParity(target: TravelParity) {
        val s = section ?: return
        val dir = SignalRules.axisDir(s.signals, signalsKm, target) ?: return
        val wantReversed = dir < 0
        manualReversed = wantReversed
        rebuild()
    }

    /** Вернуть автоматическое направление (по плечу поезда, а без плеча - по движению). */
    fun useAutoDirection() {
        if (manualReversed == null) return
        manualReversed = null
        motionReversed = null
        resetMotion()
        rebuild()
    }

    /** Начальное положение 1П/2П: чётность номера поезда, иначе чётность направления движения, иначе 1П. */
    val defaultSignalTrack: SignalTrack
        get() = SignalRules.parityOfTrainNumber(train?.number)?.track ?: parity?.track ?: SignalTrack.ODD

    /** Какие светофоры показаны: выбранные вручную или по умолчанию. */
    val signalTrack: SignalTrack get() = manualSignalTrack ?: defaultSignalTrack

    fun chooseSignalTrack(track: SignalTrack) { manualSignalTrack = track }

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

    private var legLoadKey: String? = null

    /** Участок плеча поезда среди загруженных; null - плеча нет или подходящего участка пока нет. */
    private fun legSectionId(): String? =
        if (stopNames.size < 2) null else LegMatch.sections(sections, stopNames).firstOrNull()?.id

    /**
     * Плечо известно, а положения на участке нет (вне путей, GPS ещё не поймал): участки подгружаются у станций плеча и берётся тот,
     * что подходит плечу, - профиль показывается сразу. Один раз на набор станций плеча.
     */
    fun ensureLeg() {
        if (!loaded || needLogin || manualId != null || stopPoints.isEmpty() || stopNames.size < 2) return
        // Участок уже выбран (по положению) - плечо ничего не добавляет, лишних загрузок не делаем (лимиты сервера на загрузки участков).
        if (live != null) return
        val key = stopPoints.joinToString(",") { it.name }
        if (key == legLoadKey) { pickLeg(); return }
        legLoadKey = key
        scope.launch {
            // Сначала участки у начала плеча; у конца - только если подходящего плечу участка так и не нашлось.
            for (p in listOf(stopPoints.first(), stopPoints.last())) {
                val found = runCatching { withContext(Dispatchers.Default) { repo.sectionsNear(p.latitude, p.longitude) } }.getOrDefault(emptyList())
                merge(found)
                pickLeg()
                if (live != null) break
            }
        }
    }

    private fun pickLeg() {
        if (manualId != null || live != null) return
        val id = legSectionId() ?: return
        autoId = id
        rebuild()
    }

    private fun autoPick(fix: GeoFix, force: Boolean = false) {
        if (!force && autoId != null && live?.position?.offRoute != true) return
        val near = SectionPicker.nearest(sections.map { it.axis }, fix.latitude, fix.longitude)
        if (near.isEmpty()) { autoId = legSectionId(); return }
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
            if (cur?.section?.id != s.id) manualSignalTrack = null
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

        /** Граница зоны ограничения дальше этого от линии плеча на ней «не лежит» (м): ось участка редкая, допуск шире, чем у цели. */
        const val ZONE_ON_LINE_M = 400.0
        const val MOTION_KM = 0.3
        const val MAX_STEP_KM = 1.0
        const val NEAR_RELOAD_M = 2_000.0
        const val NEAR_RETRY_MS = 30_000L
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
        ProfileModel(repo, ProfileCalibrationStore(context), scope, railLookup, TrackWarningStore(context), voice, voiceSettings, journal, lengthStore = TrainLengthStore(context), declineStore = OfferDeclineStore(context))
    }
    model.stopNames = remember(stops) { stops.map { it.name }.filter { it.isNotBlank() } }
    model.stopPoints = stops
    LaunchedEffect(trains, trainId) { model.initTrains(trains, trainId) }
    LaunchedEffect(Unit) { model.load() }
    // Журнал поезда меняют и другие экраны (раздел «Поезд», фоновая запись): профиль и карта читают его заново.
    LaunchedEffect(model, model.train?.id) {
        while (true) {
            model.refreshJournal()
            kotlinx.coroutines.delay(2_000)
        }
    }
    LaunchedEffect(model.loaded, model.stopNames) { model.ensureLeg() }
    LaunchedEffect(fix, fixAtMs, model.loaded) { model.onFix(fix, speedKmh, if (fixAtMs > 0) fixAtMs else System.currentTimeMillis()) }
    return model
}

/** Ограничения, не поставленные на профиль из-за неизвестного километража: места для заголовка и текста диалога. */
internal class RejectNotice(val places: List<String>) {
    val title: String get() = if (places.size == 1) "Не поставлено: ${places.first()}" else "Не поставлено ограничений: ${places.size}"
    val text: String
        get() = TrackWarnings.UNKNOWN_KM_MESSAGE + if (places.size > 1) "\n\n" + places.joinToString("\n") { "• $it" } else ""
}

/** Зоны ограничений для расчёта прибытия; [note] - предупреждение пользователю, когда расчёт неполный (null - всё учтено). */
internal class SpeedZonesResult(val zones: List<com.z_company.domain.util.ArrivalPlan.SpeedZone>, val note: String? = null)
