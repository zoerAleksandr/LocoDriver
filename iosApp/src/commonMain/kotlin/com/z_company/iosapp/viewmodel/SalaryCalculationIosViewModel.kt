package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.AppError
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHours
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.getNightTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
) : ViewModel() {

    /** Сводка за месяц — передаётся в Swift через watchSummary. */
    data class MonthlySummary(
        val month: String,
        val routeCount: Int,
        val totalWorkMs: Long,
        val nightTimeMs: Long,
        val overtimeMs: Long,
        val normalNormaMs: Long,
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

    // Пустой _error для consistency с другими VM (Шаг 5 Wrapper-паттерн).
    // Этот VM не делает сетевых запросов напрямую — только passive collect
    // маршрутов через RouteUseCase. Публиковать сюда нечего.
    private val _error = MutableStateFlow<AppError?>(null)
    val error: StateFlow<AppError?> = _error.asStateFlow()

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
                val totalWorkMs = calculateTotalWorkMs(routes)
                val nightTimeMs = calculateNightTimeMs(routes, settings)
                val normaMs = calculateNormaMs(settings.selectMonthOfYear)
                val overtimeMs = if (totalWorkMs > normaMs) totalWorkMs - normaMs else 0L

                _summary.value = MonthlySummary(
                    month = formatMonth(settings),
                    routeCount = routes.size,
                    totalWorkMs = totalWorkMs,
                    nightTimeMs = nightTimeMs,
                    overtimeMs = overtimeMs,
                    normalNormaMs = normaMs,
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

    private fun calculateTotalWorkMs(routes: List<Route>): Long {
        var total = 0L
        routes.forEach { route ->
            val wt = route.getWorkTime()
            if (wt != null && wt > 0L) total += wt
        }
        return total
    }

    private suspend fun calculateNightTimeMs(routes: List<Route>, settings: UserSettings): Long {
        if (routes.isEmpty()) return 0L
        return routes.getNightTime(settings)
    }

    private fun calculateNormaMs(monthOfYear: MonthOfYear): Long {
        val normaHours = monthOfYear.getPersonalNormaHours()
        return normaHours * 3_600_000L
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

    fun watchSummary(callback: (MonthlySummary?) -> Unit): WatchHandle =
        WatchHandle(viewModelScope.launch { summary.collect { callback(it) } })

    fun watchRoutes(callback: (List<RouteRow>) -> Unit): WatchHandle =
        WatchHandle(viewModelScope.launch { routes.collect { callback(it) } })

    fun watchIsLoading(callback: (Boolean) -> Unit): WatchHandle =
        WatchHandle(viewModelScope.launch { isLoading.collect { callback(it) } })

    fun watchCurrentMonth(callback: (Int) -> Unit): WatchHandle =
        WatchHandle(viewModelScope.launch { currentMonth.collect { callback(it) } })

    fun watchCurrentYear(callback: (Int) -> Unit): WatchHandle =
        WatchHandle(viewModelScope.launch { currentYear.collect { callback(it) } })

    fun watchError(callback: (AppError?) -> Unit): WatchHandle =
        WatchHandle(viewModelScope.launch { error.collect { callback(it) } })

    fun clearError() { _error.value = null }
}
