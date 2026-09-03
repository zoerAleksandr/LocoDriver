@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.salary.SalaryCalculationHelper
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.NormaUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * KMP ViewModel для экрана «Расчёт зарплаты».
 *
 * Использует RouteUseCase + SettingsUseCase для загрузки маршрутов текущего месяца
 * и расчёта суммарного рабочего времени, ночных часов и переработки.
 *
 * Навигация по месяцам сохраняется в UserSettings через SettingsUseCase.setCurrentMonthOfYear().
 */
class SalaryCalculationIosViewModel(
    private val routeUseCase: RouteUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val salarySettingUseCase: SalarySettingUseCase,
    private val normaUseCase: NormaUseCase,
) : ViewModel() {

    /** Сводка за месяц — передаётся в Swift через watchSummary. */
    data class MonthlySummary(
        val month: String,
        val routeCount: Int,
        val totalWorkMs: Long,
        val nightTimeMs: Long,
        val overtimeMs: Long,
        val normalNormaMs: Long,
        val totalCharged: Double,
        val totalRetained: Double,
        val toBeCredited: Double,
    ) {
        val totalWorkHours: Long get() = totalWorkMs / (1_000L * 60 * 60)
        val totalWorkMinutes: Long get() = (totalWorkMs / (1_000L * 60)) % 60
        val nightTimeHours: Long get() = nightTimeMs / (1_000L * 60 * 60)
        val nightTimeMinutes: Long get() = (nightTimeMs / (1_000L * 60)) % 60
        val overtimeHours: Long get() = overtimeMs / (1_000L * 60 * 60)
        val overtimeMinutes: Long get() = (overtimeMs / (1_000L * 60)) % 60
        val normalNormaHours: Long get() = normalNormaMs / (1_000L * 60 * 60)
    }

    /** Упрощённое представление маршрута для списка в Swift. */
    data class RouteRow(
        val routeId: String,
        val number: String,
        val timeStartWorkMs: Long,
        val workDurationMs: Long,
    ) {
        val workHours: Long get() = workDurationMs / (1_000L * 60 * 60)
        val workMinutes: Long get() = (workDurationMs / (1_000L * 60)) % 60
    }

    // ── State flows ───────────────────────────────────────────────────────────

    private val _summary = MutableStateFlow<MonthlySummary?>(null)
    val summary: StateFlow<MonthlySummary?> = _summary.asStateFlow()

    private val _routes = MutableStateFlow<List<RouteRow>>(emptyList())
    val routes: StateFlow<List<RouteRow>> = _routes.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentMonth = MutableStateFlow(0)
    val currentMonth: StateFlow<Int> = _currentMonth.asStateFlow()

    private val _currentYear = MutableStateFlow(0)
    val currentYear: StateFlow<Int> = _currentYear.asStateFlow()

    // ── Internal state ────────────────────────────────────────────────────────

    private var _latestSettings: UserSettings? = null
    private var routesJob: Job? = null

    // ── Initialization ────────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            settingsUseCase.getUserSettingFlow().collect { settings ->
                _latestSettings = settings
                _currentMonth.value = settings.selectMonthOfYear.month
                _currentYear.value = settings.selectMonthOfYear.year
                loadData(settings)
            }
        }
    }

    // ── Month navigation ──────────────────────────────────────────────────────

    /** Перейти на следующий месяц. */
    fun nextMonth() {
        val settings = _latestSettings ?: return
        val moy = settings.selectMonthOfYear
        val newMonth = if (moy.month == 11) 0 else moy.month + 1
        val newYear = if (moy.month == 11) moy.year + 1 else moy.year
        applyMonthChange(settings, newMonth, newYear)
    }

    /** Перейти на предыдущий месяц. */
    fun previousMonth() {
        val settings = _latestSettings ?: return
        val moy = settings.selectMonthOfYear
        val newMonth = if (moy.month == 0) 11 else moy.month - 1
        val newYear = if (moy.month == 0) moy.year - 1 else moy.year
        applyMonthChange(settings, newMonth, newYear)
    }

    /** Перейти к конкретному месяцу/году (month: 0-based). */
    fun setMonth(month: Int, year: Int) {
        val settings = _latestSettings ?: return
        applyMonthChange(settings, month, year)
    }

    private fun applyMonthChange(settings: UserSettings, newMonth: Int, newYear: Int) {
        val updatedMoy = settings.selectMonthOfYear.copy(
            month = newMonth,
            year = newYear,
            days = emptyList(),
        )
        viewModelScope.launch {
            settingsUseCase.setCurrentMonthOfYear(updatedMoy).collect {}
        }
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private fun loadData(settings: UserSettings) {
        routesJob?.cancel()
        routesJob = viewModelScope.launch {
            _isLoading.value = true
            routeUseCase.routeListByMonthFlow(
                monthOfYear = settings.selectMonthOfYear,
                offsetInMoscow = settings.timeZone,
            ).collect { routes ->
                val normaHours = normaUseCase.normaHoursFlow(
                    settings.selectMonthOfYear.year,
                    settings.selectMonthOfYear.month,
                ).first()
                val normaMs = normaHours * 3_600_000L
                val effectiveNormaHours = effectiveNormaHours(settings.selectMonthOfYear)
                val calculator = SalaryCalculationHelper(
                    userSettings = settings,
                    salarySetting = salarySettingUseCase.getSalarySetting(),
                    allRoutes = routes,
                    effectiveNormaHoursForUnderwork = effectiveNormaHours,
                )
                val totalWorkMs = calculator.getTotalWorkTimeWithCommute().first()
                val nightTimeMs = calculator.getNightTimeFlow().first()
                val overtimeMs = calculator.getTimeOvertimeFlow().first()

                _summary.value = MonthlySummary(
                    month = formatMonth(settings),
                    routeCount = routes.size,
                    totalWorkMs = totalWorkMs,
                    nightTimeMs = nightTimeMs,
                    overtimeMs = overtimeMs,
                    normalNormaMs = normaMs,
                    totalCharged = calculator.getMoneyTotalChargedFlow().first(),
                    totalRetained = calculator.getMoneyTotalRetentionFlow().first(),
                    toBeCredited = calculator.getMoneyToBeCredited().first(),
                )
                _routes.value = routes.map { route ->
                    val workMs = route.getWorkTime() ?: 0L
                    RouteRow(
                        routeId = route.basicData.id,
                        number = route.basicData.number ?: "—",
                        timeStartWorkMs = route.basicData.timeStartWork ?: 0L,
                        workDurationMs = if (workMs > 0L) workMs else 0L,
                    )
                }.sortedByDescending { it.timeStartWorkMs }
                _isLoading.value = false
            }
        }
    }

    // ── Calculations ──────────────────────────────────────────────────────────

    private suspend fun effectiveNormaHours(monthOfYear: MonthOfYear): Int {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val selectedIndex = monthOfYear.year * 12 + monthOfYear.month
        val currentIndex = today.year * 12 + today.monthNumber - 1
        return when {
            selectedIndex > currentIndex -> 0
            selectedIndex < currentIndex -> normaUseCase
                .normaHoursFlow(monthOfYear.year, monthOfYear.month).first()
            else -> normaUseCase.normaHoursToDateFlow(
                monthOfYear.year,
                monthOfYear.month,
                today.dayOfMonth,
            ).first()
        }
    }

    // ── Formatting ────────────────────────────────────────────────────────────

    private fun formatMonth(settings: UserSettings): String {
        val monthNames = listOf(
            "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь",
        )
        val moy = settings.selectMonthOfYear
        val name = monthNames.getOrElse(moy.month) { "?" }
        return "$name ${moy.year}"
    }

    // ── Watch callbacks (called from Swift) ───────────────────────────────────

    fun watchSummary(callback: (MonthlySummary?) -> Unit) {
        viewModelScope.launch { summary.collect { callback(it) } }
    }

    fun watchRoutes(callback: (List<RouteRow>) -> Unit) {
        viewModelScope.launch { routes.collect { callback(it) } }
    }

    fun watchIsLoading(callback: (Boolean) -> Unit) {
        viewModelScope.launch { isLoading.collect { callback(it) } }
    }

    fun watchCurrentMonth(callback: (Int) -> Unit) {
        viewModelScope.launch { currentMonth.collect { callback(it) } }
    }

    fun watchCurrentYear(callback: (Int) -> Unit) {
        viewModelScope.launch { currentYear.collect { callback(it) } }
    }
}
