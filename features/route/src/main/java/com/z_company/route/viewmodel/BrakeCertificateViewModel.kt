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
import com.z_company.domain.brake.TurnaroundEntry
import com.z_company.domain.brake.TurnaroundPlanner
import com.z_company.domain.use_cases.RouteUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.coroutines.flow.first
import com.z_company.domain.brake.BrakeCertificateInput
import com.z_company.domain.brake.BrakeLocoCatalog
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
) {
    fun elapsedMs(nowMs: Long): Long = accumulatedMs + if (running) (nowMs - startedAtMs).coerceAtLeast(0) else 0L
}

data class BrakeCertificateUiState(
    // Параметры расчёта (не входят в бланк)
    val categoryId: String = "FREIGHT_LOADED",
    val slope: SlopeStep = SlopeStep.S006,
    val autoblock3: Boolean = false,
    /** Ручное переопределение нормы ручных осей (запоминается); null - «Авто» по уклону (табл. III.4, не менее 0,6). */
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
    // Источник данных для «Заполнить из маршрута»
    val routeId: String? = null,
    val trainId: String? = null,
    val fillReport: FillReport? = null,
    val filling: Boolean = false,
    /** Названия станций раздела «Поезд» (для выбора станции в отметке оборота). */
    val trainStationNames: List<String> = emptyList(),
)

val BrakeCertificateUiState.grade: GradeClass get() = slope.grade
val BrakeCertificateUiState.manualInfo: BrakeNorms.ManualInfo get() = BrakeNorms.manualInfo(slope, manualOverride)

/** «Заполнить из маршрута»: итог подстановки не показывается, только короткая ошибка ([note]). */
data class FillReport(val note: String? = null) {
    val text: String get() = note.orEmpty()
}

/** Экран-калькулятор «Справка ВУ-45»: данные не сохраняются (кроме выбранной нормы ручных тормозов). */
class BrakeCertificateViewModel(application: Application) : AndroidViewModel(application), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    private val certificates: BrakeCertificateUseCase by inject()
    /** Запись справки после выхода с экрана не должна отменяться вместе с viewModelScope. */
    private val persistScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var storedId: String = ""
    private var lastSavedKey: BrakeCertificateData? = null
    private var persistStarted = false
    private val prefs = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        BrakeCertificateUiState(
            manualOverride = loadManualOverride(),
            autoblock3 = prefs.getBoolean(KEY_AUTOBLOCK, DEFAULT_AUTOBLOCK),
            compositeShare = loadCompositeShare(),
            dateText = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
        ),
    )
    val state: StateFlow<BrakeCertificateUiState> = _state.asStateFlow()

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
            startPersisting()
        }
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
                _state.update { it.copy(filling = false, fillReport = FillReport("Маршрут не найден")) }
                return@launch
            }
            val wantedTrain = _state.value.trainId
            val train = route.trains.firstOrNull { it.trainId == wantedTrain } ?: TrainOrdering.pickHomeMapTarget(route.trains)
            val loco = route.locomotives.lastOrNull()
            val q = BrakePrefill(
                weight = train?.weight, axles = train?.axle, trainNumber = train?.number,
                locoSeries = loco?.series, locoNumber = loco?.number,
            ).sanitized()
            val cur = _state.value
            val pick: (String, String?) -> String = if (onOpen) BrakeFillResolver::pickOnOpen else BrakeFillResolver::pickRouteValue
            val lf = if (onOpen) {
                // при открытии: серия/номер/группа не перетираются, только пустые подставляются
                BrakeFillResolver.resolveLoco(null, null, cur.locoSeriesText.ifBlank { q.locoSeries.orEmpty() }, cur.locoNumberText.ifBlank { q.locoNumber.orEmpty() }, cur.densityGroup, cur.customVolumeText)
            } else {
                BrakeFillResolver.resolveLoco(q.locoSeries, q.locoNumber, cur.locoSeriesText, cur.locoNumberText, cur.densityGroup, cur.customVolumeText)
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
                    filling = false,
                    fillReport = null,
                )
            }
            setLocoSeries(lf.series)
            applyStopCandidates(train?.stations.orEmpty())
        }
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
            _state.map { it.toData().copy(id = "", updatedAt = 0L) }.debounce(1000).collect { persist(it) }
        }
    }

    private fun persist(key: BrakeCertificateData) {
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
        station.isNotBlank() || tailCar.isNotBlank() || manualHave.isNotBlank() || flags.isNotEmpty() || tcpv.isNotBlank() || dpv.isNotBlank()

    override fun onCleared() {
        if (persistStarted) persist(_state.value.toData().copy(id = "", updatedAt = 0L))
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
    fun setCategory(id: String) = _state.update {
        val cat = BrakeNorms.category(id) ?: return@update it
        it.copy(categoryId = id, locoEnabled = cat.locoCountedByDefault)
    }

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
    fun setDate(v: String) = _state.update { it.copy(dateText = v.take(10)) }
    fun setLocoNumber(v: String) = _state.update { it.copy(locoNumberText = v) }
    fun setTrainNumber(v: String) = _state.update { it.copy(trainNumberText = v) }
    fun setWeight(v: String) = _state.update { it.copy(weightText = v) }
    fun setTotalAxles(v: String) = _state.update { it.copy(totalAxlesText = v) }

    /** Серия локомотива; если серия есть в справочнике таблиц 4-5, подставляются вес, оси и нажатие (можно править). */
    fun setLocoSeries(v: String) = _state.update {
        var next = it.copy(locoSeriesText = v)
        val data = BrakeLocoCatalog.find(v)
        if (data != null && (it.locoAuto || it.locoWeightText.isBlank() && it.locoAxlesText.isBlank() && it.locoPressureText.isBlank())) {
            next = next.copy(
                locoWeightText = fmtNum(data.weight),
                locoAxlesText = data.axles.toString(),
                locoPressureText = fmtNum(data.pressurePerAxle),
                locoAuto = true,
                locoNote = data.note,
            )
        } else if (data == null && it.locoAuto) {
            next = next.copy(locoWeightText = "", locoAxlesText = "", locoPressureText = "", locoAuto = false, locoNote = null)
        }
        next
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
    fun setRowsMode(on: Boolean) = _state.update {
        if (on == it.rowsMode) it
        else if (on) it.copy(
            rowsMode = true,
            rows = listOf(BrakeRowUi(id = it.nextRowId, pressureText = it.avgPressureText, axlesText = it.totalAxlesText)),
            nextRowId = it.nextRowId + 1,
        ) else it.copy(rowsMode = false)
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
    fun setAxlesForPressure(pressure: Double, axles: String) = _state.update { s0 ->
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

    fun removeRow(id: Int) = _state.update {
        val left = it.rows.filterNot { r -> r.id == id }
        it.copy(rows = left.ifEmpty { listOf(BrakeRowUi(id = it.nextRowId)) }, nextRowId = it.nextRowId + 1)
    }

    fun setRowPressure(id: Int, v: String) = updateRow(id) { it.copy(pressureText = v) }
    fun setRowAxles(id: Int, v: String) = updateRow(id) { it.copy(axlesText = v) }

    private fun updateRow(id: Int, f: (BrakeRowUi) -> BrakeRowUi) = _state.update {
        it.copy(rows = it.rows.map { r -> if (r.id == id) f(r) else r })
    }

    // ── Локомотив ──
    fun setLocoEnabled(v: Boolean) = _state.update { it.copy(locoEnabled = v) }
    fun setLocoWeight(v: String) = _state.update { it.copy(locoWeightText = v, locoAuto = false, locoNote = null) }
    fun setLocoAxles(v: String) = _state.update { it.copy(locoAxlesText = v, locoAuto = false, locoNote = null) }
    fun setLocoPressure(v: String) = _state.update { it.copy(locoPressureText = v, locoAuto = false, locoNote = null) }

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

    // ── Плотность ──
    fun setChargePressure(v: String) = _state.update { it.copy(chargePressureText = v) }
    /** Выбор группы по табл. IV.1 вручную сбрасывает свой объём. */
    fun setDensityGroup(g: DensityLocoGroup?) = _state.update { it.copy(densityGroup = g, customVolumeText = "") }

    /** Свой объём ГР, л: группа определяется по прим. 3 к табл. IV.1 (ближайший наименьший объём). Пусто - свой объём не задан. */
    fun setCustomVolume(v: String) = _state.update { it.copy(customVolumeText = v, densityGroup = if (v.isBlank()) it.densityGroup else null) }
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
        updateTimer(k) { it.copy(running = true, startedAtMs = now, accumulatedMs = 0L, recordedSec = null) }
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
        if (k == TimerKind.ROUTE) {
            val st = _state.value
            val target = st.routeTargetEntryId
            val sec = st.timerRoute.recordedSec
            if (target != null && sec != null) {
                _state.update { s0 ->
                    s0.copy(
                        turnaround = s0.turnaround.map { e -> if (e.id == target) e.copy(densitySec = sec) else e },
                        routeTargetEntryId = null,
                    )
                }
            }
        }
        _nowMs.value = now
    }

    fun resetTimer(k: TimerKind) = updateTimer(k) {
        DensityTimerState(dropText = it.dropText)
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
        private const val PREFS = "brake_certificate_prefs"
        private const val KEY_MANUAL_NORM = "manual_norm"
        private const val KEY_AUTOBLOCK = "autoblock3"
        const val DEFAULT_AUTOBLOCK = false
        private const val KEY_COMPOSITE = "composite_share"
        val DEFAULT_COMPOSITE = CompositeShare.P100

        fun fmtNum(p: Double): String =
            if (p % 1.0 == 0.0) p.toLong().toString() else p.toString().replace('.', ',')

        fun formatPressure(p: Double): String = fmtNum(p)
    }
}

/** Собирает вход калькулятора; null - данных недостаточно (нет веса или категории). */
fun BrakeCertificateUiState.toInput(): BrakeCertificateInput? {
    val cat = BrakeNorms.category(categoryId) ?: return null
    val weight = weightText.toFiniteDoubleOrNull()?.takeIf { it > 0 } ?: return null
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
    fun m(kind: TimerKind, t: DensityTimerState) = DensityMeasurementData(kind.name, t.recordedSec, drop = t.dropText)
    return BrakeCertificateData(
        routeId = routeId, trainId = trainId, categoryId = categoryId, grade = grade, slope = slope, autoblock3 = autoblock3, manualNorm = manualOverride,
        station = stationText, issueTime = issueTimeText, date = dateText, locoSeries = locoSeriesText, locoNumber = locoNumberText,
        trainNumber = trainNumberText, weight = weightText, totalAxles = totalAxlesText,
        avgPressure = avgPressureText, rowsMode = rowsMode, rows = rows.map { AxleRowData(it.pressureText, it.axlesText) },
        locoEnabled = locoEnabled, locoWeight = locoWeightText, locoAxles = locoAxlesText, locoPressure = locoPressureText,
        compositeShare = compositeShare, flags = flags.toList(), tcpv = tcpvText, vstr = vstrText, dpv = dpvText, vo2hv = vo2hvText,
        manualHave = manualHaveText, feedDensity = feedDensityText, tailVoltage = tailVoltageText, tailCar = tailCarText,
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
            accumulatedMs = ((m.seconds ?: 0.0) * 1000).toLong(), recordedSec = m.seconds,
            dropText = m.drop,
        )
    }
    var next = 0
    val rowUi = rows.map { BrakeRowUi(id = next++, pressureText = it.pressure, axlesText = it.axles) }.ifEmpty { listOf(BrakeRowUi(id = next++)) }
    return base.copy(
        categoryId = categoryId, slope = slope, autoblock3 = base.autoblock3, manualOverride = manualNorm ?: base.manualOverride,
        stationText = station, issueTimeText = issueTime, dateText = date.ifBlank { base.dateText },
        locoSeriesText = locoSeries, locoNumberText = locoNumber, trainNumberText = trainNumber,
        weightText = weight, totalAxlesText = totalAxles, avgPressureText = avgPressure, rowsMode = rowsMode,
        rows = rowUi, nextRowId = next,
        locoEnabled = locoEnabled, locoWeightText = locoWeight, locoAxlesText = locoAxles, locoPressureText = locoPressure,
        compositeShare = compositeShare, flags = flags.toSet(), tcpvText = tcpv, vstrText = vstr, dpvText = dpv, vo2hvText = vo2hv,
        manualHaveText = manualHave, feedDensityText = feedDensity, tailVoltageText = tailVoltage, tailCarText = tailCar,
        chargePressureText = chargePressure, densityGroup = densityGroup, customVolumeText = customVolume, locoCountText = locoCount.ifBlank { "1" },
        timerII = timer(TimerKind.II), timerIV = timer(TimerKind.IV), timerRoute = timer(TimerKind.ROUTE),
        turnaround = turnaround,
    )
}

/** Итоговая группа плотности: свой объём ГР (прим. 3 к табл. IV.1) > выбранная вручную > автоподбор по серии и номеру локомотива из маршрута. */
fun BrakeCertificateUiState.effectiveDensityGroup(): DensityLocoGroup? =
    if (customVolumeText.isNotBlank()) (BrakeDensity.groupByVolume(customVolumeText) as? BrakeDensity.VolumeGroup.Found)?.group
    else densityGroup ?: BrakeDensity.groupForLoco(locoSeriesText, locoNumberText)
