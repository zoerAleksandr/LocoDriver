package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Day cell data for the calendar grid.
 * dayOfMonth: 1-based day number (or 0 for empty leading cells)
 * hasRoute: whether there is at least one route on this day
 * routeCount: number of routes on this day
 */
data class DayCell(
    val dayOfMonth: Int,
    val hasRoute: Boolean,
    val routeCount: Int,
)

class WorkScheduleIosViewModel(
    private val routeUseCase: RouteUseCase,
    private val settingsUseCase: SettingsUseCase,
) : ViewModel() {

    private val _settings = MutableStateFlow<UserSettings?>(null)
    val settings: StateFlow<UserSettings?> = _settings.asStateFlow()

    private val _routes = MutableStateFlow<List<Route>>(emptyList())
    val routes: StateFlow<List<Route>> = _routes.asStateFlow()

    private val _dayCells = MutableStateFlow<List<DayCell>>(emptyList())
    val dayCells: StateFlow<List<DayCell>> = _dayCells.asStateFlow()

    private val _totalRouteCount = MutableStateFlow(0)
    val totalRouteCount: StateFlow<Int> = _totalRouteCount.asStateFlow()

    private val _totalWorkMs = MutableStateFlow(0L)
    val totalWorkMs: StateFlow<Long> = _totalWorkMs.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var routesJob: Job? = null

    val monthLabel: String
        get() {
            val moy = _settings.value?.selectMonthOfYear ?: return ""
            return "${monthNames.getOrElse(moy.month) { "?" }} ${moy.year}"
        }

    init {
        viewModelScope.launch {
            settingsUseCase.getUserSettingFlow().collect { userSettings ->
                _settings.value = userSettings
                loadRoutesForMonth(userSettings)
            }
        }
    }

    private fun loadRoutesForMonth(userSettings: UserSettings) {
        routesJob?.cancel()
        routesJob = viewModelScope.launch {
            _isLoading.value = true
            routeUseCase.routeListByMonthFlow(
                monthOfYear = userSettings.selectMonthOfYear,
                offsetInMoscow = userSettings.timeZone,
            ).collect { routes ->
                _routes.value = routes
                buildCalendarGrid(routes, userSettings.selectMonthOfYear)
                _totalRouteCount.value = routes.size
                _totalWorkMs.value = routes.sumOf { it.getWorkTime() ?: 0L }
                _isLoading.value = false
            }
        }
    }

    private fun buildCalendarGrid(routes: List<Route>, moy: MonthOfYear) {
        val year = moy.year
        val month = moy.month + 1 // convert 0-based to 1-based

        // Get number of days in month
        val daysInMonth = getDaysInMonth(year, month)

        // Get day of week for first day of month (1=Monday ... 7=Sunday, ISO)
        val firstDayOfWeek = getFirstDayOfWeekInMonth(year, month)
        // Convert to 0-based index with Monday as 0
        val leadingEmpty = (firstDayOfWeek - 1).coerceAtLeast(0)

        // Group routes by day of month
        val routesByDay = mutableMapOf<Int, Int>()
        val tz = TimeZone.currentSystemDefault()
        routes.forEach { route ->
            val startMs = route.basicData.timeStartWork ?: return@forEach
            val ldt = Instant.fromEpochMilliseconds(startMs).toLocalDateTime(tz)
            if (ldt.year == year && ldt.monthNumber == month) {
                val day = ldt.dayOfMonth
                routesByDay[day] = (routesByDay[day] ?: 0) + 1
            }
        }

        val cells = mutableListOf<DayCell>()
        // Leading empty cells
        repeat(leadingEmpty) { cells.add(DayCell(dayOfMonth = 0, hasRoute = false, routeCount = 0)) }
        // Day cells
        for (day in 1..daysInMonth) {
            val count = routesByDay[day] ?: 0
            cells.add(DayCell(dayOfMonth = day, hasRoute = count > 0, routeCount = count))
        }
        _dayCells.value = cells
    }

    fun setNextMonth() {
        val current = _settings.value ?: return
        val moy = current.selectMonthOfYear
        val newMonth = if (moy.month >= 11) 0 else moy.month + 1
        val newYear = if (moy.month >= 11) moy.year + 1 else moy.year
        updateMonth(newYear, newMonth)
    }

    fun setPreviousMonth() {
        val current = _settings.value ?: return
        val moy = current.selectMonthOfYear
        val newMonth = if (moy.month <= 0) 11 else moy.month - 1
        val newYear = if (moy.month <= 0) moy.year - 1 else moy.year
        updateMonth(newYear, newMonth)
    }

    private fun updateMonth(year: Int, month: Int) {
        viewModelScope.launch {
            settingsUseCase.updateMonthOfYearInUserSetting(
                MonthOfYear(year = year, month = month)
            ).collect { /* result */ }
        }
    }

    companion object {
        val monthNames = listOf(
            "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь",
        )

        /** Returns the number of days in a given month (1-based month). */
        fun getDaysInMonth(year: Int, month: Int): Int {
            return when (month) {
                1, 3, 5, 7, 8, 10, 12 -> 31
                4, 6, 9, 11 -> 30
                2 -> if (isLeapYear(year)) 29 else 28
                else -> 30
            }
        }

        private fun isLeapYear(year: Int): Boolean =
            (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

        /**
         * Returns ISO day of week for the first day of the given month.
         * 1 = Monday, 7 = Sunday
         * Uses Zeller's congruence adapted for Gregorian calendar.
         */
        fun getFirstDayOfWeekInMonth(year: Int, month: Int): Int {
            // Use kotlinx-datetime approach: calculate first day of month
            val y = if (month < 3) year - 1 else year
            val m = if (month < 3) month + 12 else month
            val k = y % 100
            val j = y / 100
            // Zeller's formula (result: 0=Sat, 1=Sun, 2=Mon, ... 6=Fri)
            val h = (1 + (13 * (m + 1)) / 5 + k + k / 4 + j / 4 - 2 * j) % 7
            // Convert to ISO (1=Mon..7=Sun)
            return ((h + 5) % 7) + 1
        }
    }
}
