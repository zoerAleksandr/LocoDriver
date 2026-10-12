package com.z_company.route.viewmodel

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.brake.AxleRow
import com.z_company.domain.brake.BrakeCertificateInput
import com.z_company.domain.brake.BrakeLocoCatalog
import com.z_company.domain.brake.BrakeNorms
import com.z_company.domain.brake.BrakePrefill
import com.z_company.domain.brake.CompositeShare
import com.z_company.domain.brake.DensityLocoGroup
import com.z_company.domain.brake.GradeClass
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
    val grStartText: String = "",
    val grEndText: String = "",
    val dropText: String = "",
) {
    fun elapsedMs(nowMs: Long): Long = accumulatedMs + if (running) (nowMs - startedAtMs).coerceAtLeast(0) else 0L
}

data class BrakeCertificateUiState(
    // Параметры расчёта (не входят в бланк)
    val categoryId: String = "FREIGHT_LOADED",
    val grade: GradeClass = GradeClass.UP_TO_006,
    val autoblock3: Boolean = false,
    val manualNorm: BrakeNorms.ManualBrakeNorm = BrakeNorms.ManualBrakeNorm.UNIFIED_06,
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
    val compositeShare: CompositeShare = CompositeShare.NONE,
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
    val locoCountText: String = "1",
    val timerII: DensityTimerState = DensityTimerState(),
    val timerIV: DensityTimerState = DensityTimerState(),
    val timerRoute: DensityTimerState = DensityTimerState(),
)

/** Экран-калькулятор «Справка ВУ-45»: данные не сохраняются (кроме выбранной нормы ручных тормозов). */
class BrakeCertificateViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        BrakeCertificateUiState(
            manualNorm = loadManualNorm(),
            dateText = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
        ),
    )
    val state: StateFlow<BrakeCertificateUiState> = _state.asStateFlow()

    /** Монотонное «сейчас» для отображения идущих секундомеров (обновляется, пока хоть один идёт). */
    private val _nowMs = MutableStateFlow(SystemClock.elapsedRealtime())
    val nowMs: StateFlow<Long> = _nowMs.asStateFlow()
    private var tickJob: Job? = null

    private var prefilled = false

    /** Предзаполнение из поезда и локомотива маршрута (один раз за жизнь VM). */
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
            )
        }
        setLocoSeries(q.locoSeries.orEmpty())
    }
    private fun loadManualNorm(): BrakeNorms.ManualBrakeNorm =
        runCatching { BrakeNorms.ManualBrakeNorm.valueOf(prefs.getString(KEY_MANUAL_NORM, null) ?: "") }
            .getOrDefault(BrakeNorms.ManualBrakeNorm.UNIFIED_06)

    // ── Параметры расчёта ──
    fun setCategory(id: String) = _state.update {
        val cat = BrakeNorms.category(id) ?: return@update it
        it.copy(categoryId = id, locoEnabled = cat.locoCountedByDefault, compositeShare = CompositeShare.NONE)
    }

    fun setGrade(g: GradeClass) = _state.update { it.copy(grade = g) }
    fun setAutoblock(v: Boolean) = _state.update { it.copy(autoblock3 = v) }

    fun setManualNorm(norm: BrakeNorms.ManualBrakeNorm) {
        prefs.edit().putString(KEY_MANUAL_NORM, norm.name).apply()
        _state.update { it.copy(manualNorm = norm) }
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

    /** Переключает ввод «одно среднее нажатие» / «оси по типам»; при переходе в таблицу первая строка берётся из среднего. */
    fun setRowsMode(on: Boolean) = _state.update {
        if (on == it.rowsMode) it
        else if (on) it.copy(
            rowsMode = true,
            rows = listOf(BrakeRowUi(id = it.nextRowId, pressureText = it.avgPressureText, axlesText = it.totalAxlesText)),
            nextRowId = it.nextRowId + 1,
        ) else it.copy(rowsMode = false)
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
    fun setCompositeShare(v: CompositeShare) = _state.update { it.copy(compositeShare = v) }
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
    fun setDensityGroup(g: DensityLocoGroup?) = _state.update { it.copy(densityGroup = g) }
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
        _nowMs.value = now
    }

    fun resetTimer(k: TimerKind) = updateTimer(k) {
        DensityTimerState(grStartText = it.grStartText, grEndText = it.grEndText, dropText = it.dropText)
    }

    fun setGrStart(k: TimerKind, v: String) = updateTimer(k) { it.copy(grStartText = v) }
    fun setGrEnd(k: TimerKind, v: String) = updateTimer(k) { it.copy(grEndText = v) }
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
        autoblock3 = autoblock3,
        compositeShare = compositeShare,
        manualNorm = manualNorm,
        actualManualAxles = manualHaveText.trim().toIntOrNull(),
        declaredAxles = if (rowsMode) totalAxlesText.trim().toIntOrNull() else null,
    )
}
