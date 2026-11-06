package com.z_company.route.viewmodel

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.autorecord.TrainOrdering
import com.z_company.domain.brake.AxleRow
import com.z_company.domain.brake.storage.BrakeCertificateUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import com.z_company.domain.brake.AxleRowData
import com.z_company.domain.brake.BrakeCertificateData
import com.z_company.domain.brake.DensityMeasurementData
import com.z_company.domain.brake.StopClass
import com.z_company.domain.brake.TrainGroup
import com.z_company.domain.brake.TurnaroundEntry
import com.z_company.domain.brake.TurnaroundPick
import com.z_company.domain.brake.TurnaroundTarget
import com.z_company.domain.brake.TurnaroundPlanner
import com.z_company.domain.use_cases.RouteUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.coroutines.flow.first
import com.z_company.domain.brake.BrakeCertificateInput
import com.z_company.domain.brake.LocoPassport
import com.z_company.domain.brake.LocoSeriesDefaults
import com.z_company.domain.brake.SeriesPassportField
import com.z_company.domain.brake.BrakeCategory
import com.z_company.domain.brake.BrakeNorms
import com.z_company.domain.brake.BrakePrefill
import com.z_company.domain.brake.CompositeShare
import com.z_company.domain.brake.BrakeDensity
import com.z_company.domain.brake.BrakeFillResolver
import com.z_company.domain.brake.DensityLocoGroup
import com.z_company.domain.brake.GradeClass
import com.z_company.domain.brake.SlopeStep
import com.z_company.domain.brake.LocoBrakeInput
import com.z_company.domain.util.toFiniteDoubleOrNull
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Строка таблицы нажатий: нажатие на ось (тс, значение первой графы бланка или своё) и число осей. */
data class BrakeRowUi(
    val id: Int,
    val pressureText: String = "",
    val axlesText: String = "",
)

enum class TimerKind { II, IV, ROUTE }

/**
 * Секундомер замера плотности. Время считается по монотонным часам ([SystemClock.elapsedRealtime]):
 * поворот экрана и сворачивание отсчёт не сбрасывают (состояние живёт во ViewModel).
 */
data class DensityTimerState(
    val running: Boolean = false,
    val startedAtMs: Long = 0L,
    val accumulatedMs: Long = 0L,
    val recordedSec: Double? = null,
    val dropText: String = "",
    /** Значение введено вручную (а не замером). */
    val manual: Boolean = false,
    /** Давление в начале и в конце замера, кгс/см2. */
    val grStartText: String = "",
    val grEndText: String = "",
) {
    fun elapsedMs(nowMs: Long): Long = accumulatedMs + if (running) (nowMs - startedAtMs).coerceAtLeast(0) else 0L
}

data class BrakeCertificateUiState(
    // Параметры расчёта (не входят в бланк)
    val categoryId: String = "FREIGHT_LOADED",
    val slope: SlopeStep = SlopeStep.S006,
    val autoblock3: Boolean = false,
    /** Ручное переопределение нормы ручных осей (запоминается); null - «Авто» по уклону (табл. 4 прил. 2, не менее 0,6). */
    val manualOverride: BrakeNorms.ManualBrakeNorm? = null,
    // Шапка бланка
    val stationText: String = "",
    val issueTimeText: String = "",
    val dateText: String = "",
    val locoSeriesText: String = "",
    val locoNumberText: String = "",
    val trainNumberText: String = "",
    val weightText: String = "",
    val totalAxlesText: String = "",
    // Нажатие: быстрый ввод (среднее на ось x всего осей) или таблица «оси по типам»
    val avgPressureText: String = "",
    val rowsMode: Boolean = false,
    val rows: List<BrakeRowUi> = listOf(BrakeRowUi(id = 0)),
    val nextRowId: Int = 1,
    // Локомотив в подсчёте
    val locoEnabled: Boolean = false,
    val locoWeightText: String = "",
    val locoAxlesText: String = "",
    val locoPressureText: String = "",
    val locoAuto: Boolean = false,
    /** Свой объём ГР подставлен из справочника серий (а не введён) — при смене серии заменяется. */
    val volumeAuto: Boolean = false,
    val locoNote: String? = null,
    // Другие данные
    val compositeShare: CompositeShare = CompositeShare.P100,
    val flags: Set<String> = emptySet(),
    val tcpvText: String = "",
    val vstrText: String = "",
    val dpvText: String = "",
    val vo2hvText: String = "",
    // Подвал бланка
    val manualHaveText: String = "",
    val feedDensityText: String = "",
    val tailVoltageText: String = "",
    val tailCarText: String = "",
    val inspectorNameText: String = "",
    // Плотность
    val chargePressureText: String = "",
    val densityGroup: DensityLocoGroup? = null,
    /** Свой объём ГР, л (текст); пусто - не задан. Имеет приоритет над [densityGroup] и автоподбором. */
    val customVolumeText: String = "",
    val locoCountText: String = "1",
    val timerII: DensityTimerState = DensityTimerState(),
    val timerIV: DensityTimerState = DensityTimerState(),
    val timerRoute: DensityTimerState = DensityTimerState(),
    // Оборот (опробования в пути)
    val turnaround: List<TurnaroundEntry> = emptyList(),
    /** Запись оборота, в которую попадёт результат ближайшего замера «В пути». */
    val routeTargetEntryId: String? = null,
    /** Запись оборота, в которую записан последний замер «В пути» (для «Записано в оборот: …»). */
    val routeWrittenEntryId: String? = null,
    /** Одноразовое уведомление «Записано в оборот» (снекбар), [nonce] отличает повторные записи. */
    val writtenNotice: WrittenNotice? = null,
    /** Запрос подтверждения замены плотности в записи оборота при повторном замере. */
    val replaceConfirm: ReplaceConfirm? = null,
    // Источник данных для «Заполнить из маршрута»
    val routeId: String? = null,
    val trainId: String? = null,
    val fillReport: FillReport? = null,
    /** Пояснение к подбору категории по номеру поезда (после «Заполнить из маршрута»). */
    val categoryNote: String? = null,
    /** Категория выбрана вручную (запоминается в prefs): подбор по нагрузке на ось не применяется. */
    val categoryManual: Boolean = false,
    /** Пометка «Определено по нагрузке…» под выбором категории. */
    val loadNote: String? = null,
    /** Выбор поезда/локомотива маршрута при нескольких (шторка как на главном экране). */
    val fillPick: FillPick? = null,
    /** Сообщение «нет текущего маршрута / нет поезда» (диалог). */
    val fillDialog: String? = null,
    val filling: Boolean = false,
    /** Названия станций раздела «Поезд» (для выбора станции в отметке оборота). */
    val trainStationNames: List<String> = emptyList(),
    /** Все известные станции пользователя (список настроек + справочник станций) для подсказок. */
    val knownStationNames: List<String> = emptyList(),
    /** Серии локомотивов пользователя (справочник серий + список настроек) для подсказок в поле серии. */
    val knownSeriesNames: List<String> = emptyList(),
)

val BrakeCertificateUiState.grade: GradeClass get() = slope.grade
val BrakeCertificateUiState.manualInfo: BrakeNorms.ManualInfo get() = BrakeNorms.manualInfo(slope, manualOverride)

/** «Заполнить из маршрута»: итог подстановки не показывается, только короткая ошибка ([note]). */
/** Запрос выбора при «Заполнить из маршрута»: поезда или локомотивы маршрута [routeId]. */
data class ReplaceConfirm(val label: String, val oldSec: Double, val newSec: Double)

data class WrittenNotice(val label: String, val nonce: Long)

data class FillPick(
    val kind: Kind,
    val trains: List<com.z_company.domain.entities.route.Train> = emptyList(),
    val locos: List<com.z_company.domain.entities.route.Locomotive> = emptyList(),
) {
    enum class Kind { TRAIN, LOCO }
}

data class FillReport(val note: String? = null) {
    val text: String get() = note.orEmpty()
}

/** Экран «Справка ВУ-45»: справка поезда сохраняется на устройстве (BrakeCertificate.db) и синхронизируется с сервером в составе рейса; запоминаются также норма ручных тормозов, автоблокировка и композиция. */
class BrakeCertificateViewModel(application: Application) : AndroidViewModel(application), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    private val certificates: BrakeCertificateUseCase by inject()
    private val settingsUseCase: com.z_company.domain.use_cases.SettingsUseCase by inject()
    private val stationDirectory: com.z_company.domain.use_cases.StationDirectoryUseCase by inject()
    private val seriesRepository: com.z_company.domain.repositories.LocomotiveSeriesRepository by inject()
    /** Запись справки после выхода с экрана не должна отменяться вместе с viewModelScope. */
    private val persistScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var storedId: String = ""
    private var lastSavedKey: BrakeCertificateData? = null
    private var persistStarted = false
    /** Справка удалена пользователем: автосохранение отключено (иначе при выходе она сохранилась бы заново). */
    private var deleted = false
    private val prefs = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    /** Кеш справочника серий: подстановка данных при вводе серии не должна ходить в БД на каждый символ. */
    @Volatile
    private var seriesRecordsCache: List<com.z_company.domain.entities.norma_time.LocomotiveSeries> = emptyList()

    private val _state = MutableStateFlow(
        BrakeCertificateUiState(
            manualOverride = loadManualOverride(),
            categoryManual = false,
            autoblock3 = prefs.getBoolean(KEY_AUTOBLOCK, DEFAULT_AUTOBLOCK),
            compositeShare = loadCompositeShare(),
            categoryId = loadCategoryId(),
            locoEnabled = BrakeNorms.category(loadCategoryId())?.locoCountedByDefault ?: false,
            dateText = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
        ),
    )
    val state: StateFlow<BrakeCertificateUiState> = _state.asStateFlow()

    init {
        // Кеш серий грузим сразу: пока подписка ниже не пришла, ввод серии всё равно должен подставлять данные.
        viewModelScope.launch(Dispatchers.IO) {
            if (seriesRecordsCache.isEmpty()) {
                seriesRecordsCache = runCatching { seriesRepository.getAll() }.getOrDefault(emptyList())
            }
        }
        // станции пользователя для подсказок: список из настроек (то, что вводилось в формах), справочник станций и станции всех маршрутов (история).
        // Подписка на потоки, а не разовое чтение: первое значение настроек/маршрутов может прийти пустым и затем обновиться.
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                settingsUseCase.getUserSettingFlow().map { it.stationList to it.locomotiveSeriesList },
                routeUseCase.getListRoutesAsFlow(),
            ) { saved, routes -> saved to routes }
                .debounce(300)
                .collect { (savedLists, routes) ->
                    val saved = savedLists.first
                    val series = withContext(Dispatchers.Default) {
                        val dir = runCatching { seriesRepository.getAll() }.getOrDefault(emptyList())
                        seriesRecordsCache = dir
                        (dir.map { it.name } + savedLists.second).map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }.sortedBy { it.lowercase() }
                    }
                    val names = withContext(Dispatchers.Default) {
                        val dir = runCatching { stationDirectory.stations().map { it.name } }.getOrDefault(emptyList())
                        val history = routes.flatMap { r -> r.trains.flatMap { t -> t.stations.mapNotNull { it.stationName } } }
                        saved + dir + history
                    }
                    // Списки не изменились - не эмитим новый state и не пересобираем экран (сохранение справки
                    // помечает маршрут несинхронизированным и триггерит этот поток во время ввода).
                    val cur = _state.value
                    if (cur.knownStationNames != names || cur.knownSeriesNames != series) {
                        _state.update { it.copy(knownStationNames = names, knownSeriesNames = series) }
                    }
                }
        }
    }

    /** Монотонное «сейчас» для отображения идущих секундомеров (обновляется, пока хоть один идёт). */
    private val _nowMs = MutableStateFlow(SystemClock.elapsedRealtime())
    val nowMs: StateFlow<Long> = _nowMs.asStateFlow()
    private var tickJob: Job? = null

    private var prefilled = false

    /**
     * Предзаполнение при входе: сразу значениями из аргументов, затем (если известен маршрут) - актуальными
     * данными из БД, как «Заполнить из маршрута». Один раз за жизнь VM.
     */
    fun prefill(p: BrakePrefill?) {
        if (prefilled) return
        prefilled = true
        if (p == null) return
        val q = p.sanitized()
        _state.update {
            it.copy(
                weightText = q.weight.orEmpty(),
                totalAxlesText = q.axles.orEmpty(),
                trainNumberText = q.trainNumber.orEmpty(),
                locoNumberText = q.locoNumber.orEmpty(),
                routeId = q.routeId,
                trainId = q.trainId,
            )
        }
        setLocoSeries(q.locoSeries.orEmpty())
        viewModelScope.launch {
            loadStored()
            if (q.routeId != null) fillFromRoute(onOpen = true)
            autoCategory()
            startPersisting()
        }
    }

    private var pendingRoute: com.z_company.domain.entities.route.Route? = null
    private var pendingTrain: com.z_company.domain.entities.route.Train? = null

    /**
     * Кнопка «Заполнить из маршрута». Открыта из формы «Поезд» (известен маршрут) - тот же маршрут и поезд; из «Инструментов» - текущий маршрут
     * (как на главном экране): нет маршрута - «Нет текущего маршрута», нет поезда - «В текущем маршруте нет поезда», несколько поездов - выбор поезда,
     * затем при нескольких локомотивах - выбор локомотива (шторки как в «Текущем маршруте»).
     */
    fun onFillClicked() {
        val rid = _state.value.routeId
        viewModelScope.launch {
            _state.update { it.copy(filling = true, fillReport = null, fillDialog = null, fillPick = null) }
            val route = if (rid != null) loadRoute(rid) else currentRoute()
            if (route == null) {
                _state.update {
                    if (rid != null) it.copy(filling = false, fillReport = FillReport("Маршрут не найден"))
                    else it.copy(filling = false, fillDialog = "Нет текущего маршрута")
                }
                return@launch
            }
            val train: com.z_company.domain.entities.route.Train?
            if (rid == null) {
                when {
                    route.trains.isEmpty() -> { _state.update { it.copy(filling = false, fillDialog = "В текущем маршруте нет поезда") }; return@launch }
                    route.trains.size > 1 -> {
                        pendingRoute = route
                        _state.update { it.copy(filling = false, fillPick = FillPick(FillPick.Kind.TRAIN, trains = route.trains)) }
                        return@launch
                    }
                    else -> train = route.trains.first()
                }
            } else {
                val wanted = _state.value.trainId
                train = route.trains.firstOrNull { it.trainId == wanted } ?: TrainOrdering.pickHomeMapTarget(route.trains)
            }
            continueFill(route, train)
        }
    }

    /** Выбран поезд в шторке. */
    fun pickTrain(train: com.z_company.domain.entities.route.Train) {
        val route = pendingRoute ?: return
        _state.update { it.copy(fillPick = null, filling = true) }
        viewModelScope.launch { continueFill(route, train) }
    }

    /** Выбран локомотив в шторке. */
    fun pickLoco(loco: com.z_company.domain.entities.route.Locomotive) {
        val route = pendingRoute ?: return
        val train = pendingTrain
        _state.update { it.copy(fillPick = null, filling = true) }
        viewModelScope.launch { performFill(route, train, loco, onOpen = false) }
    }

    fun dismissFillPick() {
        pendingRoute = null; pendingTrain = null
        _state.update { it.copy(fillPick = null, filling = false) }
    }

    fun dismissFillDialog() = _state.update { it.copy(fillDialog = null) }

    private suspend fun continueFill(route: com.z_company.domain.entities.route.Route, train: com.z_company.domain.entities.route.Train?) {
        if (route.locomotives.size > 1) {
            pendingRoute = route; pendingTrain = train
            _state.update { it.copy(filling = false, fillPick = FillPick(FillPick.Kind.LOCO, locos = route.locomotives)) }
        } else performFill(route, train, route.locomotives.firstOrNull(), onOpen = false)
    }

    /** Текущий маршрут - как на главном экране (`findCurrentRoute`). */
    private suspend fun currentRoute(): com.z_company.domain.entities.route.Route? = withContext(Dispatchers.IO) {
        runCatching {
            val routes = routeUseCase.getListRoutesAsFlow().first()
            val settings = settingsUseCase.getUserSettingFlow().first()
            with(com.z_company.domain.entities.route.UtilsForEntities) { routes.findCurrentRoute(System.currentTimeMillis(), settings) }
        }.getOrNull()
    }

    /**
     * «Заполнить из маршрута»: вес, оси, номер поезда - из раздела «Поезд»; серия и номер - из последнего локомотива маршрута. Нажатие СРАЗУ заменяет значения
     * актуальными данными маршрута (в т.ч. введённые вручную); пустые и нулевые поля маршрута введённое не затирают. Объём ГР определяется заново
     * по серии (ВЛ10 - по номеру); ручная группа и свой объём сбрасываются, только если маршрут содержит серию, по которой группа определяется.
     * [onOpen] - автозаполнение при открытии экрана: только пустые поля, ничего не заменяется.
     */
    fun fillFromRoute(onOpen: Boolean = false) {
        val rid = _state.value.routeId
        if (rid == null) {
            if (!onOpen) _state.update { it.copy(fillReport = FillReport("Нет данных маршрута")) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(filling = true, fillReport = null) }
            val route = loadRoute(rid)
            if (route == null) {
                _state.update { it.copy(filling = false, fillReport = if (onOpen) null else FillReport("Маршрут не найден")) }
                return@launch
            }
            val wantedTrain = _state.value.trainId
            val train = route.trains.firstOrNull { it.trainId == wantedTrain } ?: TrainOrdering.pickHomeMapTarget(route.trains)
            performFill(route, train, route.locomotives.lastOrNull(), onOpen)
        }
    }

    /** Подстановка данных выбранных поезда и локомотива маршрута (см. [fillFromRoute]). */
    private suspend fun performFill(
        route: com.z_company.domain.entities.route.Route,
        train: com.z_company.domain.entities.route.Train?,
        loco: com.z_company.domain.entities.route.Locomotive?,
        onOpen: Boolean,
    ) {
        pendingRoute = null; pendingTrain = null
        val q = BrakePrefill(
            weight = train?.weight, axles = train?.axle, trainNumber = train?.number,
            locoSeries = loco?.series, locoNumber = loco?.number,
        ).sanitized()
        val cur = _state.value
        val pick: (String, String?) -> String = if (onOpen) BrakeFillResolver::pickOnOpen else BrakeFillResolver::pickRouteValue
        val lf = if (onOpen) {
            // при открытии: серия/номер/группа не перетираются, только пустые подставляются
            BrakeFillResolver.resolveLoco(null, null, cur.locoSeriesText.ifBlank { q.locoSeries.orEmpty() }, cur.locoNumberText.ifBlank { q.locoNumber.orEmpty() }, cur.densityGroup, cur.customVolumeText.takeUnless { cur.volumeAuto }.orEmpty())
        } else {
            BrakeFillResolver.resolveLoco(q.locoSeries, q.locoNumber, cur.locoSeriesText, cur.locoNumberText, cur.densityGroup, cur.customVolumeText.takeUnless { cur.volumeAuto }.orEmpty())
        }
        _state.update {
            it.copy(
                weightText = pick(cur.weightText, q.weight),
                totalAxlesText = pick(cur.totalAxlesText, q.axles),
                trainNumberText = pick(cur.trainNumberText, q.trainNumber),
                locoNumberText = lf.number,
                densityGroup = lf.manualGroup,
                customVolumeText = lf.customVolume,
                trainId = train?.trainId ?: it.trainId,
                routeId = route.basicData.id,
                filling = false,
                fillReport = null,
            )
        }
        setLocoSeries(lf.series)
        if (!onOpen) {
            // категория по номеру поезда из маршрута - только если номер определяет её однозначно (иначе текущая остаётся)
            val st = _state.value
            val sug = BrakeCategory.suggestByTrainNumber(q.trainNumber, st.totalAxlesText.trim().toIntOrNull(), st.categoryId)
            sug.categoryId?.let { applyCategory(it) }
            _state.update { it.copy(categoryNote = sug.note) }
            // «Заполнить из маршрута» сбрасывает ручной выбор категории; номер (однозначный) приоритетнее нагрузки
            _state.update { it.copy(categoryManual = false, loadNote = null) }
            autoCategory()
            // «Учитывать локомотив» следует категории: в пассажирских и грузопассажирских включено, в грузовых выключено (ручная правка действует до следующего заполнения)
            val cat = BrakeNorms.category(_state.value.categoryId)
            if (cat != null) _state.update { it.copy(locoEnabled = cat.locoCountedByDefault) }
        }
        applyStopCandidates(train?.stations.orEmpty())
        if (!persistStarted) startPersisting()
    }

    // ── Хранение (BrakeCertificate.db; docs/VU45_STORAGE_DESIGN.md) ──

    /** Подставляет сохранённую справку поезда: сохранённое приоритетнее предзаполнения, пустые поля берутся из предзаполнения. */
    private suspend fun loadStored() {
        val tid = _state.value.trainId ?: return
        val stored = withContext(Dispatchers.IO) { runCatching { certificates.loadForTrain(tid) }.getOrNull() } ?: return
        storedId = stored.id
        _state.update { cur ->
            val ui = stored.toUiState(cur)
            ui.copy(
                weightText = ui.weightText.ifBlank { cur.weightText },
                totalAxlesText = ui.totalAxlesText.ifBlank { cur.totalAxlesText },
                trainNumberText = ui.trainNumberText.ifBlank { cur.trainNumberText },
                locoNumberText = ui.locoNumberText.ifBlank { cur.locoNumberText },
                locoSeriesText = ui.locoSeriesText.ifBlank { cur.locoSeriesText },
                routeId = cur.routeId, trainId = cur.trainId, manualOverride = cur.manualOverride,
            )
        }
        lastSavedKey = _state.value.toData().copy(id = "", updatedAt = 0L)
    }

    /** Автосохранение: дебаунс 1 с после последнего изменения; при выходе - немедленно (onCleared). */
    @OptIn(FlowPreview::class)
    private fun startPersisting() {
        if (persistStarted) return
        persistStarted = true
        viewModelScope.launch {
            // debounce до map: toData() собирает весь бланк и не должно выполняться на каждый символ ввода.
            _state.debounce(1000).map { it.toData().copy(id = "", updatedAt = 0L) }.collect { persist(it) }
        }
    }

    private fun persist(key: BrakeCertificateData) {
        if (deleted) return
        val st = _state.value
        val rid = st.routeId ?: return
        val tid = st.trainId ?: return
        if (!hasContent(key) || key == lastSavedKey) return
        lastSavedKey = key
        persistScope.launch {
            runCatching {
                val saved = certificates.save(key.copy(id = storedId), rid, tid) { java.util.UUID.randomUUID().toString() }
                storedId = saved.id
                // Справка уходит на сервер в составе рейса: без пометки рейс остаётся «синхронизированным»
                // и правка справки не отправится до следующего изменения самого рейса.
                if (com.z_company.repository.remote_rest.RemoteFeatureFlags.remoteCertificatesEnabled) {
                    routeUseCase.markUnsynchronized(rid).first { it !is ResultState.Loading }
                }
            }
        }
    }

    private fun hasContent(d: BrakeCertificateData): Boolean =
        d.weight.isNotBlank() || d.totalAxles.isNotBlank() || d.avgPressure.isNotBlank() || d.rows.any { it.pressure.isNotBlank() || it.axles.isNotBlank() } ||
            d.measurements.any { it.seconds != null } || d.turnaround.isNotEmpty() || d.stationOrOtherFilled()

    private fun BrakeCertificateData.stationOrOtherFilled() =
        station.isNotBlank() || tailCar.isNotBlank() || inspectorName.isNotBlank() || manualHave.isNotBlank() || flags.isNotEmpty() || tcpv.isNotBlank() || dpv.isNotBlank()

    override fun onCleared() {
        if (persistStarted && !deleted) persist(_state.value.toData().copy(id = "", updatedAt = 0L))
        super.onCleared()
    }

    /**
     * Маршрут из БД. Поток `routeDetails` первым значением отдаёт `Loading` - раньше брали `first()` и принимали его за
     * «маршрут не найден»; ждём первое значение, не являющееся `Loading`.
     */
    private suspend fun loadRoute(rid: String): com.z_company.domain.entities.route.Route? =
        runCatching {
            (routeUseCase.routeDetails(rid).first { it !is ResultState.Loading } as? ResultState.Success)?.data
        }.getOrNull()

    /** Обновить отметки оборота по стоянкам поезда (времена прибытия/отправления станций раздела «Поезд»). */
    fun refreshStops() {
        val rid = _state.value.routeId ?: return
        viewModelScope.launch {
            val route = loadRoute(rid) ?: return@launch
            val wantedTrain = _state.value.trainId
            val train = route.trains.firstOrNull { it.trainId == wantedTrain } ?: TrainOrdering.pickHomeMapTarget(route.trains)
            applyStopCandidates(train?.stations.orEmpty())
        }
    }

    // ── Оборот ──
    private fun applyStopCandidates(stations: List<com.z_company.domain.entities.route.Station>) {
        val cands = TurnaroundPlanner.candidates(stations, System.currentTimeMillis())
        val names = stations.sortedBy { it.orderIndex }.mapNotNull { it.stationName?.trim()?.takeIf { n -> n.isNotEmpty() } }.distinct()
        _state.update { st0 ->
            val st = st0.copy(trainStationNames = names)
            val byStation = st.turnaround.filter { it.stationId != null }.associateBy { it.stationId }
            val merged = mutableListOf<TurnaroundEntry>()
            // ручные и введённые пользователем записи сохраняются
            val keep = st.turnaround.filter { e ->
                e.stationId == null || !e.auto || e.densitySec != null || e.readinessTime.isNotBlank() || e.inspectorTab.isNotBlank() ||
                    cands.any { it.stationId == e.stationId }
            }
            merged += keep.map { e ->
                val c = cands.firstOrNull { it.stationId == e.stationId }
                if (c == null) e else e.copy(
                    stationName = c.stationName.orEmpty().ifBlank { e.stationName },
                    stopClass = c.stopClass, arrivalMs = c.arrivalMs, departureMs = c.departureMs,
                )
            }
            cands.filter { c -> byStation[c.stationId] == null }.forEach { c ->
                merged += TurnaroundEntry(
                    id = "t-" + c.stationId, stationId = c.stationId, stationName = c.stationName.orEmpty(),
                    stopClass = c.stopClass, arrivalMs = c.arrivalMs, departureMs = c.departureMs, auto = true,
                )
            }
            st.copy(turnaround = merged)
        }
    }

    fun newTurnaroundId(): String = "m-" + System.nanoTime()

    fun addTurnaround(stopClass: StopClass, station: String, id: String = newTurnaroundId()) = _state.update {
        it.copy(turnaround = it.turnaround + TurnaroundEntry(id = id, stationName = station, stopClass = stopClass))
    }

    /** Правка веса/осей новой строки состава: потребное и фактическое пересчитываются калькулятором, если не правились вручную. */
    fun updateTurnaroundComposition(id: String, f: (TurnaroundEntry) -> TurnaroundEntry) = _state.update { st ->
        val base = st.toInput(requireWeight = false)
        fun calc(x: TurnaroundEntry) =
            com.z_company.domain.brake.TurnaroundComposition.compute(base, x.newWeight.toFiniteDoubleOrNull(), x.newAxles.trim().toIntOrNull(), x.changes)
        st.copy(turnaround = st.turnaround.map { e ->
            if (e.id != id) e else {
                val n = f(e)
                com.z_company.domain.brake.TurnaroundComposition.autoFill(e, n, calc(e), calc(n))
            }
        })
    }

    fun updateTurnaround(id: String, f: (TurnaroundEntry) -> TurnaroundEntry) = _state.update {
        it.copy(turnaround = it.turnaround.map { e -> if (e.id == id) f(e) else e })
    }

    fun removeTurnaround(id: String) = _state.update {
        it.copy(turnaround = it.turnaround.filterNot { e -> e.id == id }, routeTargetEntryId = it.routeTargetEntryId.takeIf { t -> t != id })
    }

    /** Следующий замер «В пути» запишется в эту отметку оборота. */
    fun setRouteTarget(id: String?) = _state.update { it.copy(routeTargetEntryId = id) }

    /** null - «Авто» (по умолчанию); старые сохранённые значения до введения «Авто» также читаются как ручные. */
    private fun loadManualOverride(): BrakeNorms.ManualBrakeNorm? =
        runCatching { BrakeNorms.ManualBrakeNorm.valueOf(prefs.getString(KEY_MANUAL_NORM, null) ?: "") }.getOrNull()

    // ── Параметры расчёта ──
    /** Выбор категории пользователем запоминается и становится значением по умолчанию для новой справки (у сохранённой справки своя категория). */
    fun setCategory(id: String) {
        if (BrakeNorms.category(id) == null) return
        prefs.edit().putString(KEY_CATEGORY, id).apply()
        applyCategory(id)
        _state.update { it.copy(categoryManual = true, loadNote = null) }
    }

    private fun axlesForCategory(st: BrakeCertificateUiState): Int? =
        st.totalAxlesText.trim().toIntOrNull() ?: st.effectiveRows().sumOf { it.axlesText.trim().toIntOrNull() ?: 0 }.takeIf { it > 0 }

    /**
     * Автоподбор категории (пока она не выбрана вручную): однозначный номер поезда, иначе нагрузка на ось.
     * Вызывается при открытии справки и при изменении номера, веса, осей и строк таблицы.
     */
    private fun autoCategory() {
        val st = _state.value
        if (st.categoryManual) return
        val byNum = BrakeCategory.byTrainNumber(st.trainNumberText, axlesForCategory(st))
        if (byNum != null) {
            if (byNum.id != st.categoryId) applyCategory(byNum.id)
            _state.update { it.copy(loadNote = null) }
            return
        }
        recomputeByAxleLoad()
    }

    /** Подбор груженый/порожний по нагрузке на ось (пока категория не выбрана вручную и не определена номером поезда). */
    private fun recomputeByAxleLoad() {
        val st = _state.value
        val axles = axlesForCategory(st)
        val byNumber = BrakeCategory.byTrainNumber(st.trainNumberText, axles) != null
        val sug = BrakeCategory.suggestByAxleLoad(st.categoryId, st.categoryManual, byNumber, st.weightText.toFiniteDoubleOrNull(), axles)
        if (sug == null) {
            if (st.loadNote != null && (st.categoryManual || st.categoryId !in BrakeCategory.axleLoadCategoryIds)) _state.update { it.copy(loadNote = null) }
            return
        }
        if (sug.categoryId != st.categoryId) applyCategory(sug.categoryId)
        _state.update { it.copy(loadNote = sug.note) }
    }

    /** Категория без запоминания (подбор по номеру поезда при «Заполнить из маршрута»). */
    private fun applyCategory(id: String) = _state.update {
        val cat = BrakeNorms.category(id) ?: return@update it
        it.copy(categoryId = id, locoEnabled = cat.locoCountedByDefault)
    }

    private fun loadCategoryId(): String =
        prefs.getString(KEY_CATEGORY, null)?.takeIf { BrakeNorms.category(it) != null } ?: "FREIGHT_LOADED"

    fun setSlope(step: SlopeStep) = _state.update { it.copy(slope = step) }

    /** Запоминается (SharedPreferences), по умолчанию выключено; на результат влияет только при спуске до 0,010 (калькулятор). */
    fun setAutoblock(v: Boolean) {
        prefs.edit().putBoolean(KEY_AUTOBLOCK, v).apply()
        _state.update { it.copy(autoblock3 = v) }
    }

    /** null - вернуться на «Авто (по уклону)». */
    fun setManualOverride(norm: BrakeNorms.ManualBrakeNorm?) {
        prefs.edit().apply { if (norm == null) remove(KEY_MANUAL_NORM) else putString(KEY_MANUAL_NORM, norm.name) }.apply()
        _state.update { it.copy(manualOverride = norm) }
    }

    // ── Шапка ──
    fun setStation(v: String) = _state.update { it.copy(stationText = v) }
    fun setIssueTime(v: String) = _state.update { it.copy(issueTimeText = v.take(5)) }
    fun setIssueTimeNow() = _state.update {
        it.copy(issueTimeText = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")))
    }
    fun setDate(v: String) = _state.update { it.copy(dateText = com.z_company.domain.brake.DateInput.mask(it.dateText, v)) }
    fun setLocoNumber(v: String) = _state.update { it.copy(locoNumberText = v) }
    fun setTrainNumber(v: String) { _state.update { it.copy(trainNumberText = v) }; autoCategory() }
    fun setWeight(v: String) { _state.update { it.copy(weightText = v) }; autoCategory() }
    fun setTotalAxles(v: String) { _state.update { it.copy(totalAxlesText = v) }; autoCategory() }

    /**
     * Серия локомотива: вес, оси, нажатие и объём ГР подставляются (и правятся вручную). Приоритет: запись справочника
     * серий пользователя -> каталог (табл. 4-5, группы ГР) -> пусто. Введённое вручную не затирается.
     */
    fun setLocoSeries(v: String) {
        // Поиск по кешу справочника серий: запрос БД на каждый символ ввода недопустим (фризы ввода).
        val records = seriesRecordsCache.ifEmpty {
            seriesRepository.getAll().also { seriesRecordsCache = it }
        }
        val userRecord = records.firstOrNull { it.name.trim().equals(v.trim(), ignoreCase = true) }
        val data = LocoSeriesDefaults.passportFor(v, userRecord)
        val hasData = data.weightT != null || data.axleCount != null || data.axleLoadTf != null
        _state.update {
            var next = it.copy(locoSeriesText = v)
            if (hasData && (it.locoAuto || it.locoWeightText.isBlank() && it.locoAxlesText.isBlank() && it.locoPressureText.isBlank())) {
                next = next.copy(
                    locoWeightText = data.weightT?.let { w -> fmtNum(w) } ?: "",
                    locoAxlesText = data.axleCount?.toString() ?: "",
                    locoPressureText = data.axleLoadTf?.let { p -> fmtNum(p) } ?: "",
                    locoAuto = true,
                    locoNote = data.note,
                )
            } else if (!hasData && it.locoAuto) {
                next = next.copy(locoWeightText = "", locoAxlesText = "", locoPressureText = "", locoAuto = false, locoNote = null)
            }
            // Объём ГР: подставляется, пока не выбран вручную (ни своё значение, ни строка таблицы) — как и вес с осями.
            val volumeFree = next.densityGroup == null && (next.customVolumeText.isBlank() || next.volumeAuto)
            if (volumeFree) {
                next = if (data.brakeVolumeL != null) next.copy(customVolumeText = data.brakeVolumeL.toString(), volumeAuto = true)
                else next.copy(customVolumeText = "", volumeAuto = false)
            }
            next
        }
    }

    private val pendingSeriesEdits = mutableSetOf<SeriesPassportField>()
    private var seriesWriteBackJob: Job? = null

    /**
     * Пользователь изменил вес/оси/нажатие/объём ГР: через паузу значения записываются в справочник серий
     * (запись создаётся, если серии там не было) и в следующей справке подставляются уже они.
     */
    private fun scheduleSeriesWriteBack(field: SeriesPassportField) {
        pendingSeriesEdits += field
        seriesWriteBackJob?.cancel()
        seriesWriteBackJob = persistScope.launch {
            delay(SERIES_WRITE_BACK_DELAY_MS)
            val fields = synchronized(pendingSeriesEdits) { pendingSeriesEdits.toSet().also { pendingSeriesEdits.clear() } }
            val st = _state.value
            val name = st.locoSeriesText.trim()
            if (name.isEmpty()) return@launch
            runCatching {
                val all = seriesRepository.getAll()
                val existing = all.firstOrNull { it.name.trim().equals(name, ignoreCase = true) }
                val edited = LocoPassport(
                    weightT = st.locoWeightText.toFiniteDoubleOrNull()?.takeIf { it > 0 },
                    axleCount = st.locoAxlesText.trim().toIntOrNull()?.takeIf { it > 0 },
                    axleLoadTf = st.locoPressureText.toFiniteDoubleOrNull()?.takeIf { it > 0 },
                    brakeVolumeL = st.customVolumeText.toFiniteDoubleOrNull()?.takeIf { it > 0 }?.toInt(),
                )
                val updated = LocoSeriesDefaults.recordWithEdits(
                    name, existing, edited, fields, System.currentTimeMillis(),
                ) ?: return@runCatching
                seriesRepository.replaceAll(all.filter { it.seriesId != updated.seriesId } + updated).collect {}
            }
        }
    }

    // ── Нажатие ──
    fun setAvgPressure(v: String) = _state.update { it.copy(avgPressureText = v) }

    /**
     * Ввод в поле «Нажатие на ось» на «Расчёте»: одно значение в таблице - обновляет все заполненные строки (число осей не меняется);
     * разные значения - ввод недоступен; пустая таблица - обычное поле (если строки таблицы полностью пусты, возвращаемся к режиму «одно среднее»).
     */
    fun setPressureField(v: String) = _state.update { st ->
        when (st.pressureField()) {
            PressureField.Multiple -> st
            is PressureField.Single -> st.copy(avgPressureText = v, rows = st.rows.map { r -> if (r.isFilled()) r.copy(pressureText = v) else r })
            PressureField.Empty ->
                if (st.rowsMode && st.rows.all { it.pressureText.isBlank() && it.axlesText.isBlank() }) st.copy(avgPressureText = v, rowsMode = false)
                else if (st.rowsMode) st.copy(
                    avgPressureText = v,
                    // строки с числом осей, но без нажатия, получают введённое значение - таблица и поле остаются согласованными
                    rows = st.rows.map { r -> if ((r.axlesText.trim().toIntOrNull() ?: 0) > 0 && (r.pressureText.toFiniteDoubleOrNull() ?: 0.0) <= 0) r.copy(pressureText = v) else r },
                )
                else st.copy(avgPressureText = v)
        }
    }

    /** Переключает ввод «одно среднее нажатие» / «оси по типам»; при переходе в таблицу первая строка берётся из среднего. */
    fun setRowsMode(on: Boolean) {
        _state.update {
            if (on == it.rowsMode) it
            else if (on) it.copy(
                rowsMode = true,
                rows = listOf(BrakeRowUi(id = it.nextRowId, pressureText = it.avgPressureText, axlesText = it.totalAxlesText)),
                nextRowId = it.nextRowId + 1,
            ) else it.copy(rowsMode = false)
        }
        autoCategory()
    }

    /** Режим таблицы с заполнением первой строки из среднего нажатия и «Всего осей». */
    private fun seedRows(it: BrakeCertificateUiState): BrakeCertificateUiState {
        if (it.rowsMode) return it
        val p = it.avgPressureText.toFiniteDoubleOrNull()
        val n = it.totalAxlesText.trim().toIntOrNull()
        val seed = if (p != null && p > 0 && n != null && n > 0) {
            listOf(BrakeRowUi(id = it.nextRowId, pressureText = it.avgPressureText, axlesText = it.totalAxlesText))
        } else emptyList()
        return it.copy(rowsMode = true, rows = seed, nextRowId = it.nextRowId + 1)
    }

    /** Число осей для значения нажатия в таблице бланка (пусто - строка удаляется). */
    fun setAxlesForPressure(pressure: Double, axles: String) {
        updateAxlesForPressure(pressure, axles)
        autoCategory()
    }

    private fun updateAxlesForPressure(pressure: Double, axles: String) = _state.update { s0 ->
        val st = seedRows(s0)
        val idx = st.rows.indexOfFirst { it.pressureText.toFiniteDoubleOrNull() == pressure }
        val clean = axles.filter { it.isDigit() }.take(5)
        when {
            idx >= 0 && clean.isBlank() -> st.copy(rows = st.rows.filterIndexed { i, _ -> i != idx })
            idx >= 0 -> st.copy(rows = st.rows.mapIndexed { i, r -> if (i == idx) r.copy(axlesText = clean) else r })
            clean.isBlank() -> st
            else -> st.copy(
                rows = st.rows + BrakeRowUi(id = st.nextRowId, pressureText = fmtNum(pressure), axlesText = clean),
                nextRowId = st.nextRowId + 1,
            )
        }
    }

    // ── Таблица ──
    fun addRow() = _state.update { it.copy(rows = it.rows + BrakeRowUi(id = it.nextRowId), nextRowId = it.nextRowId + 1) }

    fun removeRow(id: Int) {
        _state.update {
            val left = it.rows.filterNot { r -> r.id == id }
            it.copy(rows = left.ifEmpty { listOf(BrakeRowUi(id = it.nextRowId)) }, nextRowId = it.nextRowId + 1)
        }
        autoCategory()
    }

    fun setRowPressure(id: Int, v: String) = updateRow(id) { it.copy(pressureText = v) }
    fun setRowAxles(id: Int, v: String) { updateRow(id) { it.copy(axlesText = v) }; autoCategory() }

    private fun updateRow(id: Int, f: (BrakeRowUi) -> BrakeRowUi) = _state.update {
        it.copy(rows = it.rows.map { r -> if (r.id == id) f(r) else r })
    }

    // ── Локомотив ──
    fun setLocoEnabled(v: Boolean) = _state.update { it.copy(locoEnabled = v) }
    fun setLocoWeight(v: String) {
        _state.update { it.copy(locoWeightText = v, locoAuto = false, locoNote = null) }
        scheduleSeriesWriteBack(SeriesPassportField.WEIGHT)
    }
    fun setLocoAxles(v: String) {
        _state.update { it.copy(locoAxlesText = v, locoAuto = false, locoNote = null) }
        scheduleSeriesWriteBack(SeriesPassportField.AXLES)
    }
    fun setLocoPressure(v: String) {
        _state.update { it.copy(locoPressureText = v, locoAuto = false, locoNote = null) }
        scheduleSeriesWriteBack(SeriesPassportField.AXLE_LOAD)
    }

    // ── Другие данные ──
    /** Запоминается (SharedPreferences), по умолчанию К-100; общее состояние «Расчёта» и «Бланка». */
    fun setCompositeShare(v: CompositeShare) {
        prefs.edit().putString(KEY_COMPOSITE, v.name).apply()
        _state.update { it.copy(compositeShare = v) }
    }

    private fun loadCompositeShare(): CompositeShare =
        runCatching { CompositeShare.valueOf(prefs.getString(KEY_COMPOSITE, null) ?: "") }.getOrDefault(DEFAULT_COMPOSITE)
    fun toggleFlag(f: String) = _state.update { it.copy(flags = if (f in it.flags) it.flags - f else it.flags + f) }
    fun setTcpv(v: String) = _state.update { it.copy(tcpvText = v) }
    fun setVstr(v: String) = _state.update { it.copy(vstrText = v) }
    fun setDpv(v: String) = _state.update { it.copy(dpvText = v) }
    fun setVo2hv(v: String) = _state.update { it.copy(vo2hvText = v) }

    // ── Подвал ──
    fun setManualHave(v: String) = _state.update { it.copy(manualHaveText = v) }
    fun setFeedDensity(v: String) = _state.update { it.copy(feedDensityText = v) }
    fun setTailVoltage(v: String) = _state.update { it.copy(tailVoltageText = v) }
    fun setTailCar(v: String) = _state.update { it.copy(tailCarText = v) }
    fun setInspectorName(v: String) = _state.update { it.copy(inspectorNameText = v) }

    // ── Плотность ──
    fun setChargePressure(v: String) = _state.update { it.copy(chargePressureText = v) }
    /** Выбор группы по табл. 4 п. 9.3.3 вручную сбрасывает свой объём. */
    fun setDensityGroup(g: DensityLocoGroup?) = _state.update { it.copy(densityGroup = g, customVolumeText = "", volumeAuto = false) }

    /** Свой объём ГР, л: группа определяется по прим. 4 к табл. 4 п. 9.3.3 (ближайший наименьший объём). Пусто - свой объём не задан. */
    fun setCustomVolume(v: String) {
        _state.update { it.copy(customVolumeText = v, volumeAuto = false, densityGroup = if (v.isBlank()) it.densityGroup else null) }
        scheduleSeriesWriteBack(SeriesPassportField.VOLUME)
    }
    fun setLocoCount(v: String) = _state.update { it.copy(locoCountText = v) }

    private fun timerOf(s: BrakeCertificateUiState, k: TimerKind) = when (k) {
        TimerKind.II -> s.timerII
        TimerKind.IV -> s.timerIV
        TimerKind.ROUTE -> s.timerRoute
    }

    private fun withTimer(s: BrakeCertificateUiState, k: TimerKind, t: DensityTimerState) = when (k) {
        TimerKind.II -> s.copy(timerII = t)
        TimerKind.IV -> s.copy(timerIV = t)
        TimerKind.ROUTE -> s.copy(timerRoute = t)
    }

    private fun updateTimer(k: TimerKind, f: (DensityTimerState) -> DensityTimerState) =
        _state.update { withTimer(it, k, f(timerOf(it, k))) }

    fun startTimer(k: TimerKind) {
        val now = SystemClock.elapsedRealtime()
        updateTimer(k) { it.copy(running = true, startedAtMs = now, accumulatedMs = 0L, recordedSec = null, manual = false) }
        if (k == TimerKind.ROUTE) _state.update { it.copy(routeWrittenEntryId = null) }
        ensureTicker()
    }

    /** Останов: фиксирует время замера (с, до 0,1 с). */
    fun stopTimer(k: TimerKind) {
        val now = SystemClock.elapsedRealtime()
        updateTimer(k) {
            if (!it.running) it else {
                val total = it.elapsedMs(now)
                it.copy(running = false, accumulatedMs = total, recordedSec = Math.round(total / 100.0) / 10.0)
            }
        }
        _nowMs.value = now
    }

    fun resetTimer(k: TimerKind) {
        updateTimer(k) { DensityTimerState(dropText = it.dropText) }
        if (k == TimerKind.ROUTE) _state.update { it.copy(routeWrittenEntryId = null) }
    }

    /** Запись замера «В пути» в оборот (по кнопке «Записать» после «Стоп»): выбранная запись, иначе ближайшая без плотности, иначе новая; пассажирские - без записи. */
    fun dismissReplaceConfirm() = _state.update { it.copy(replaceConfirm = null) }

    fun writeRouteMeasurement(confirmed: Boolean = false) {
        val st = _state.value
        val sec = st.timerRoute.recordedSec ?: return
        val applies = BrakeNorms.category(st.categoryId)?.let { TurnaroundPlanner.applies(it) } ?: false
        val now = System.currentTimeMillis()
        val hhmm = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
        val pick = TurnaroundTarget.pick(st.turnaround, st.routeTargetEntryId, now, applies)
        if (pick == TurnaroundPick.NotApplicable) return
        if (pick is TurnaroundPick.Existing) {
            val target = st.turnaround.firstOrNull { it.id == pick.id }
            val old = target?.densitySec
            if (old != null && !confirmed) {
                _state.update { it.copy(replaceConfirm = ReplaceConfirm(entryLabel(target), old, sec)) }
                return
            }
        }
        _state.update { it.copy(replaceConfirm = null) }
        val newId = newTurnaroundId()
        var label = ""
        _state.update { s0 ->
            val list = when (pick) {
                is TurnaroundPick.Existing -> s0.turnaround.map { e ->
                    if (e.id == pick.id) e.copy(densitySec = sec, readinessTime = if (e.densitySec != null) hhmm else e.readinessTime.ifBlank { hhmm }).also { label = BrakeCertificateViewModel.entryLabel(it) } else e
                }
                else -> TurnaroundEntry(id = newId, stationName = "", stopClass = StopClass.OVER_5, densitySec = sec, readinessTime = hhmm)
                    .also { label = BrakeCertificateViewModel.entryLabel(it) }.let { s0.turnaround + it }
            }
            // после записи замер «В пути» возвращается к началу («Старт»); запись в оборот остаётся
            s0.copy(
                turnaround = list, routeTargetEntryId = null, routeWrittenEntryId = null,
                timerRoute = DensityTimerState(dropText = s0.timerRoute.dropText),
                writtenNotice = WrittenNotice(label, System.nanoTime()),
            )
        }
    }

    /**
     * Удаление справки из топ-бара: надгробие в локальном хранилище + пометка рейса несинхронизированным (на сервер уходит список без неё,
     * `docs/VU45_STORAGE_DESIGN.md`); автосохранение отключается; тост «Справка удалена»; [onDone] закрывает экран.
     */
    fun deleteCertificate(onDone: () -> Unit) {
        deleted = true
        val id = storedId
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (id.isNotBlank()) {
                    val routes = certificates.deleteByIds(listOf(id))
                    if (com.z_company.repository.remote_rest.RemoteFeatureFlags.remoteCertificatesEnabled) {
                        routes.forEach { runCatching { routeUseCase.markUnsynchronized(it).first { r -> r !is ResultState.Loading } } }
                    }
                }
            }
            android.widget.Toast.makeText(getApplication(), "Справка удалена", android.widget.Toast.LENGTH_SHORT).show()
            onDone()
        }
    }

    fun consumeWrittenNotice() = _state.update { it.copy(writtenNotice = null) }

    /** Ручной ввод плотности, с (дробные). Некорректное значение игнорируется; true - принято. Участвует в оценке как замер. */
    fun setManualDensity(k: TimerKind, text: String): Boolean {
        val v = BrakeDensity.parseManualSeconds(text) ?: return false
        updateTimer(k) { it.copy(running = false, accumulatedMs = 0L, startedAtMs = 0L, recordedSec = v, manual = true) }
        if (k == TimerKind.ROUTE) _state.update { it.copy(routeWrittenEntryId = null) }
        return true
    }

    /** Применяет решение шторки плотности бланка к замеру [k] (см. [BrakeDensity.commitDensity]). */
    fun applyDensityCommit(k: TimerKind, c: BrakeDensity.DensityCommit) {
        when (c) {
            BrakeDensity.DensityCommit.Keep -> Unit
            is BrakeDensity.DensityCommit.Set -> setManualDensity(k, c.seconds.toString())
            BrakeDensity.DensityCommit.Clear -> {
                updateTimer(k) { it.copy(running = false, accumulatedMs = 0L, startedAtMs = 0L, recordedSec = null, manual = false) }
                if (k == TimerKind.ROUTE) _state.update { it.copy(routeWrittenEntryId = null) }
            }
        }
    }

    /** Давление в начале/в конце замера (кгс/см2). Для пассажирского II разность переносится в «падение давления в ТМ». */
    fun setPressures(k: TimerKind, start: String, end: String) {
        updateTimer(k) { it.copy(grStartText = start, grEndText = end) }
        val passenger = BrakeNorms.category(_state.value.categoryId)?.group == TrainGroup.PASSENGER
        if (k == TimerKind.II && passenger) {
            BrakeDensity.checkPressure("II", start, end, true)?.diff?.takeIf { it >= 0 }?.let { d -> updateTimer(k) { it.copy(dropText = BrakeCertificateViewModel.formatPressure(d)) } }
        }
    }

    fun setDrop(k: TimerKind, v: String) = updateTimer(k) { it.copy(dropText = v) }

    private fun ensureTicker() {
        if (tickJob?.isActive == true) return
        tickJob = viewModelScope.launch {
            while (isActive) {
                _nowMs.value = SystemClock.elapsedRealtime()
                val s = _state.value
                if (!s.timerII.running && !s.timerIV.running && !s.timerRoute.running) break
                delay(100)
            }
        }
    }

    companion object {
        /** Подпись записи оборота: «станция, время» (станция не указана - так и пишется). */
        fun entryLabel(e: TurnaroundEntry): String {
            val name = e.stationName.ifBlank { "станция не указана" }
            val time = com.z_company.domain.brake.TimeOfDayInput.display(e.readinessTime)
            return if (time.isNotBlank()) "$name, $time" else name
        }

        private const val PREFS = "brake_certificate_prefs"
        private const val SERIES_WRITE_BACK_DELAY_MS = 800L
        private const val KEY_MANUAL_NORM = "manual_norm"
        private const val KEY_AUTOBLOCK = "autoblock3"
        const val DEFAULT_AUTOBLOCK = false
        private const val KEY_COMPOSITE = "composite_share"
        private const val KEY_CATEGORY = "last_category"
        val DEFAULT_COMPOSITE = CompositeShare.P100

        fun fmtNum(p: Double): String =
            if (p % 1.0 == 0.0) p.toLong().toString() else p.toString().replace('.', ',')

        fun formatPressure(p: Double): String = fmtNum(p)
    }
}

/** Собирает вход калькулятора; null - данных недостаточно (нет веса или категории). */
fun BrakeCertificateUiState.toInput(requireWeight: Boolean = true): BrakeCertificateInput? {
    val cat = BrakeNorms.category(categoryId) ?: return null
    // Для оборота (изменение состава) вес лицевой не обязателен: число осей считается и без него.
    val weight = weightText.toFiniteDoubleOrNull()?.takeIf { it > 0 } ?: if (requireWeight) return null else 0.0
    val axleRows = if (rowsMode) rows.mapNotNull { r ->
        val p = r.pressureText.toFiniteDoubleOrNull() ?: return@mapNotNull null
        val n = r.axlesText.trim().toIntOrNull() ?: return@mapNotNull null
        if (p <= 0 || n <= 0) null else AxleRow(p, n)
    } else {
        val p = avgPressureText.toFiniteDoubleOrNull()
        val n = totalAxlesText.trim().toIntOrNull()
        if (p != null && n != null && p > 0 && n > 0) listOf(AxleRow(p, n)) else emptyList()
    }
    val loco = if (locoEnabled) {
        val w = locoWeightText.toFiniteDoubleOrNull()
        val a = locoAxlesText.trim().toIntOrNull()
        val p = locoPressureText.toFiniteDoubleOrNull()
        if (w != null && a != null && p != null && w > 0 && a > 0 && p > 0) LocoBrakeInput(w, a, p) else null
    } else null
    return BrakeCertificateInput(
        category = cat,
        compositionWeight = weight,
        rows = axleRows,
        loco = loco,
        grade = grade,
        slope = slope,
        autoblock3 = autoblock3,
        compositeShare = compositeShare,
        manualNorm = manualOverride,
        actualManualAxles = manualHaveText.trim().toIntOrNull(),
        declaredAxles = if (rowsMode) totalAxlesText.trim().toIntOrNull() else null,
        enteredAxles = totalAxlesText.trim().toIntOrNull(),
    )
}

/** Состояние поля «Нажатие на ось» на «Расчёте» по таблице справки. */
sealed interface PressureField {
    /** Таблица пуста (или нет полностью заполненных строк): обычное поле ввода. */
    data object Empty : PressureField

    /** Одно значение нажатия на всех заполненных строках: показывается в поле, правка меняет все эти строки. */
    data class Single(val text: String) : PressureField

    /** Две и более разных значения: «см. в справке», поле недоступно для ввода. */
    data object Multiple : PressureField
}

private fun BrakeRowUi.isFilled(): Boolean = (pressureText.toFiniteDoubleOrNull() ?: 0.0) > 0 && (axlesText.trim().toIntOrNull() ?: 0) > 0

fun BrakeCertificateUiState.pressureField(): PressureField {
    if (!rowsMode) return PressureField.Empty
    val filled = rows.filter { it.isFilled() }
    val distinct = filled.map { it.pressureText.toFiniteDoubleOrNull() }.distinct()
    return when {
        filled.isEmpty() -> PressureField.Empty
        distinct.size == 1 -> PressureField.Single(filled.first().pressureText)
        else -> PressureField.Multiple
    }
}

/** Строки таблицы нажатий «как в бланке»: в режиме таблицы - введённые строки, иначе одна строка из среднего нажатия. */
fun BrakeCertificateUiState.effectiveRows(): List<BrakeRowUi> =
    if (rowsMode) rows.filter { (it.pressureText.toFiniteDoubleOrNull() ?: 0.0) > 0 && (it.axlesText.trim().toIntOrNull() ?: 0) > 0 }
    else {
        val p = avgPressureText.toFiniteDoubleOrNull()
        val n = totalAxlesText.trim().toIntOrNull()
        if (p != null && p > 0 && n != null && n > 0) listOf(BrakeRowUi(id = -1, pressureText = avgPressureText, axlesText = totalAxlesText)) else emptyList()
    }

/** Единое сериализуемое состояние справки (без идущих секундомеров и UI-состояния). */
fun BrakeCertificateUiState.toData(updatedAt: Long = 0L): BrakeCertificateData {
    fun m(kind: TimerKind, t: DensityTimerState) = DensityMeasurementData(kind.name, t.recordedSec, drop = t.dropText, manual = t.manual, grStart = t.grStartText, grEnd = t.grEndText)
    return BrakeCertificateData(
        routeId = routeId, trainId = trainId, categoryId = categoryId, categoryManual = categoryManual, grade = grade, slope = slope, autoblock3 = autoblock3, manualNorm = manualOverride,
        station = stationText, issueTime = issueTimeText, date = dateText, locoSeries = locoSeriesText, locoNumber = locoNumberText,
        trainNumber = trainNumberText, weight = weightText, totalAxles = totalAxlesText,
        avgPressure = avgPressureText, rowsMode = rowsMode, rows = rows.map { AxleRowData(it.pressureText, it.axlesText) },
        locoEnabled = locoEnabled, locoWeight = locoWeightText, locoAxles = locoAxlesText, locoPressure = locoPressureText,
        compositeShare = compositeShare, flags = flags.toList(), tcpv = tcpvText, vstr = vstrText, dpv = dpvText, vo2hv = vo2hvText,
        manualHave = manualHaveText, feedDensity = feedDensityText, tailVoltage = tailVoltageText, tailCar = tailCarText, inspectorName = inspectorNameText,
        chargePressure = chargePressureText, densityGroup = densityGroup, customVolume = customVolumeText, locoCount = locoCountText,
        measurements = listOf(m(TimerKind.II, timerII), m(TimerKind.IV, timerIV), m(TimerKind.ROUTE, timerRoute)),
        turnaround = turnaround, updatedAt = updatedAt,
    )
}

/** Обратное преобразование сохранённой справки в состояние экрана (идущие секундомеры и UI-состояние не восстанавливаются). */
fun BrakeCertificateData.toUiState(base: BrakeCertificateUiState): BrakeCertificateUiState {
    fun timer(kind: TimerKind): DensityTimerState {
        val m = measurements.firstOrNull { it.kind == kind.name } ?: return DensityTimerState()
        return DensityTimerState(
            accumulatedMs = if (m.manual) 0L else ((m.seconds ?: 0.0) * 1000).toLong(), recordedSec = m.seconds,
            dropText = m.drop, manual = m.manual, grStartText = m.grStart, grEndText = m.grEnd,
        )
    }
    var next = 0
    val rowUi = rows.map { BrakeRowUi(id = next++, pressureText = it.pressure, axlesText = it.axles) }.ifEmpty { listOf(BrakeRowUi(id = next++)) }
    return base.copy(
        categoryId = categoryId, categoryManual = categoryManual ?: true, slope = slope, autoblock3 = base.autoblock3, manualOverride = manualNorm ?: base.manualOverride,
        stationText = station, issueTimeText = issueTime, dateText = date.ifBlank { base.dateText },
        locoSeriesText = locoSeries, locoNumberText = locoNumber, trainNumberText = trainNumber,
        weightText = weight, totalAxlesText = totalAxles, avgPressureText = avgPressure, rowsMode = rowsMode,
        rows = rowUi, nextRowId = next,
        locoEnabled = locoEnabled, locoWeightText = locoWeight, locoAxlesText = locoAxles, locoPressureText = locoPressure,
        compositeShare = compositeShare, flags = flags.toSet(), tcpvText = tcpv, vstrText = vstr, dpvText = dpv, vo2hvText = vo2hv,
        manualHaveText = manualHave, feedDensityText = feedDensity, tailVoltageText = tailVoltage, tailCarText = tailCar, inspectorNameText = inspectorName,
        chargePressureText = chargePressure, densityGroup = densityGroup, customVolumeText = customVolume, locoCountText = locoCount.ifBlank { "1" },
        timerII = timer(TimerKind.II), timerIV = timer(TimerKind.IV), timerRoute = timer(TimerKind.ROUTE),
        turnaround = turnaround,
    )
}

/** Итоговая группа плотности: свой объём ГР (прим. 4 к табл. 4 п. 9.3.3) > выбранная вручную > автоподбор по серии и номеру локомотива из маршрута. */
fun BrakeCertificateUiState.effectiveDensityGroup(): DensityLocoGroup? =
    if (customVolumeText.isNotBlank()) (BrakeDensity.groupByVolume(customVolumeText) as? BrakeDensity.VolumeGroup.Found)?.group
    else densityGroup ?: BrakeDensity.groupForLoco(locoSeriesText, locoNumberText)
