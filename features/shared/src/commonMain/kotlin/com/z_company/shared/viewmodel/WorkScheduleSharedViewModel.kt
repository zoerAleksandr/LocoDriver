@file:Suppress("DEPRECATION")
package com.z_company.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.shared.util.ConverterLongToTime
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.ReleasePeriod
import com.z_company.domain.entities.ReleaseType
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.UtilForMonthOfYear.getDayoffHoursExcludingWeekends
import com.z_company.domain.entities.UtilForMonthOfYear.getDayoffHoursIncludingWeekends
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.util.plus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.coroutines.cancellation.CancellationException

/**
 * Day cell data for the calendar grid.
 */
data class DayCell(
    val dayOfMonth: Int,
    val hasRoute: Boolean,
    val routeCount: Int,
)

/**
 * Shared ViewModel for the work schedule calendar screen.
 * Based on Android WorkScheduleViewModel with full route creation/deletion,
 * time buttons, release periods, and subscription checks.
 */
class WorkScheduleSharedViewModel(
    private val routeUseCase: RouteUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val calendarUseCase: CalendarUseCase,
) : ViewModel() {

    data class DeleteDialogRequest(val day: Int, val routes: List<Route>)

    // --- Current month / settings ---
    private val _currentMonthFull = MutableStateFlow<MonthOfYear?>(null)
    val currentMonthFull: StateFlow<MonthOfYear?> = _currentMonthFull

    private val _currentMonth = MutableStateFlow<MonthOfYear?>(null)
    val currentMonth: StateFlow<MonthOfYear?> = _currentMonth

    private val _userSettings = MutableStateFlow<UserSettings?>(null)
    val userSettings: StateFlow<UserSettings?> = _userSettings

    // --- Release/time-off ---
    private val _releasePeriods = MutableStateFlow<List<ReleasePeriod>>(emptyList())
    val releasePeriods: StateFlow<List<ReleasePeriod>> = _releasePeriods

    private val _releaseByDay = MutableStateFlow<Map<Int, Pair<ReleaseType?, Int>>>(emptyMap())
    val releaseByDay: StateFlow<Map<Int, Pair<ReleaseType?, Int>>> = _releaseByDay

    private val _totalReleaseHours = MutableStateFlow<Long>(0L)
    val totalReleaseHours: StateFlow<Long> = _totalReleaseHours

    // --- Delete mode ---
    private val _isDeleteMode = MutableStateFlow(false)
    val isDeleteMode: StateFlow<Boolean> = _isDeleteMode.asStateFlow()

    private val _selectedRoutesToDelete = MutableStateFlow<Map<Int, Set<String>>>(emptyMap())
    val selectedRoutesToDelete: StateFlow<Map<Int, Set<String>>> = _selectedRoutesToDelete.asStateFlow()

    private val _deleteDialogRequests =
        MutableSharedFlow<DeleteDialogRequest>(extraBufferCapacity = 5)
    val deleteDialogRequests = _deleteDialogRequests.asSharedFlow()

    // --- Total time work ---
    private val _totalTimeWork = MutableStateFlow<Long?>(null)
    val totalTimeWork: StateFlow<Long?> = _totalTimeWork

    // --- Month/year lists ---
    private val _monthList = MutableStateFlow<List<Int>>(emptyList())
    val monthList: StateFlow<List<Int>> = _monthList

    private val _yearList = MutableStateFlow<List<Int>>(emptyList())
    val yearList: StateFlow<List<Int>> = _yearList

    // --- Routes by day ---
    private val _routesByDay = MutableStateFlow<Map<Int, List<Route>>>(emptyMap())
    val routesByDay: StateFlow<Map<Int, List<Route>>> = _routesByDay

    // --- Time buttons ---
    private val _timeButtons = MutableStateFlow(listOf<Long>())
    val timeButtons: StateFlow<List<Long>> = _timeButtons

    private val _activeTime = MutableStateFlow<Long?>(null)
    val activeTime: StateFlow<Long?> = _activeTime

    // --- Selected days for creating routes ---
    private val _selectedDays = MutableStateFlow<Map<Int, List<Long>>>(emptyMap())
    val selectedDays: StateFlow<Map<Int, List<Long>>> = _selectedDays.asStateFlow()

    // --- Loading ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // --- Error/info messages ---
    private val _messageEvent = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messageEvent = _messageEvent.asSharedFlow()

    /**
     * Toggle single planned time for a day (add if missing, remove if exists).
     */
    fun togglePlannedTimeForDay(day: Int, timeInMillis: Long) {
        val mutable = _selectedDays.value.toMutableMap()
        val list = mutable[day]?.toMutableList() ?: mutableListOf()
        val idx = list.indexOf(timeInMillis)
        if (idx >= 0) {
            list.removeAt(idx)
        } else {
            list.add(timeInMillis)
        }
        if (list.isEmpty()) mutable.remove(day) else mutable[day] = list.toList()
        _selectedDays.value = mutable
    }

    /**
     * Call before showing the screen: fetch current month from settings
     * and load months/year lists and routes.
     */
    fun prepareScreen() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val setting = settingsUseCase.getUserSettingFlow().first()
                val month = setting.selectMonthOfYear
                _userSettings.value = setting
                _timeButtons.value = setting.standardTimesStartWork
                _currentMonth.value = month
                loadMonthYearLists()
                loadRoutesForCurrentMonth(setting.timeZone)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _messageEvent.tryEmit("Ошибка загрузки: ${t.message ?: "неизвестная ошибка"}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadMonthYearLists() {
        withContext(Dispatchers.Default) {
            val list = calendarUseCase.loadFlowMonthOfYearListState().first()
            _monthList.value = list.map { it.month }.distinct().sorted()
            _yearList.value = list.map { it.year }.distinct().sorted()
        }
    }

    private suspend fun loadRoutesForCurrentMonth(timeZone: Long) {
        val month = _currentMonth.value ?: return
        withContext(Dispatchers.Default) {
            routeUseCase.listRoutesByMonth(month, timeZone).collect { result ->
                when (result) {
                    is ResultState.Success -> {
                        val routes = result.data
                        val map = mutableMapOf<Int, MutableList<Route>>()
                        val tz = TimeZone.currentSystemDefault()
                        routes.forEach { route ->
                            val start = route.basicData.timeStartWork
                            if (start != null) {
                                val ldt = Instant.fromEpochMilliseconds(start + timeZone)
                                    .toLocalDateTime(tz)
                                val day = ldt.dayOfMonth
                                if (ldt.monthNumber - 1 == currentMonth.value?.month) {
                                    map.getOrPut(day) { mutableListOf() }.add(route)
                                }
                            }
                        }

                        userSettings.value?.let { settings ->
                            val totalWork = routes.getWorkTime(
                                settings.selectMonthOfYear, settings.timeZone
                            )
                            _totalTimeWork.value = totalWork
                        }
                        _routesByDay.value = map

                        // Compute release data
                        withContext(Dispatchers.Default) {
                            val fullMonth = currentMonth.value
                            if (fullMonth != null) {
                                val excluding = fullMonth.getDayoffHoursExcludingWeekends().times(3_600_000L)
                                val including = fullMonth.getDayoffHoursIncludingWeekends().times(3_600_000L)
                                _totalReleaseHours.value = excluding + including

                                val periods = mutableListOf<ReleasePeriod>()
                                val listReleasePeriod = mutableListOf<LocalDate>()
                                var isBegunCounting = false
                                var currentType: ReleaseType? = null
                                fullMonth.days.forEachIndexed { index, day ->
                                    if (day.isReleaseDay) {
                                        if (!isBegunCounting) {
                                            isBegunCounting = true
                                            currentType = day.releaseType
                                        }
                                        listReleasePeriod.add(
                                            LocalDate(fullMonth.year, fullMonth.month + 1, day.dayOfMonth)
                                        )
                                        if ((index + 1 == fullMonth.days.size)) {
                                            isBegunCounting = false
                                            val copyList = mutableListOf<LocalDate>().apply {
                                                addAll(listReleasePeriod)
                                            }
                                            periods.add(ReleasePeriod(days = copyList, type = currentType))
                                            listReleasePeriod.clear()
                                        }
                                    } else {
                                        if (isBegunCounting) {
                                            isBegunCounting = false
                                            val copyList = mutableListOf<LocalDate>().apply {
                                                addAll(listReleasePeriod)
                                            }
                                            periods.add(ReleasePeriod(days = copyList, type = currentType))
                                            currentType = null
                                            listReleasePeriod.clear()
                                        }
                                    }
                                }
                                _releasePeriods.value = periods

                                val releaseMap = mutableMapOf<Int, Pair<ReleaseType?, Int>>()
                                fullMonth.days.forEach { day ->
                                    if (day.isReleaseDay) {
                                        val hours = if (day.releaseType == ReleaseType.ChildCare) {
                                            when (day.tag) {
                                                TagForDay.WORKING_DAY -> 8
                                                TagForDay.SHORTENED_DAY -> 7
                                                TagForDay.NON_WORKING_DAY -> 8
                                                TagForDay.HOLIDAY -> 8
                                            }
                                        } else {
                                            when (day.tag) {
                                                TagForDay.WORKING_DAY -> 8
                                                TagForDay.SHORTENED_DAY -> 7
                                                TagForDay.NON_WORKING_DAY -> 0
                                                TagForDay.HOLIDAY -> 0
                                            }
                                        }
                                        releaseMap[day.dayOfMonth] = Pair(day.releaseType, hours)
                                    }
                                }
                                _releaseByDay.value = releaseMap
                            }
                        }

                        _isLoading.value = false
                    }
                    is ResultState.Error -> {
                        _messageEvent.tryEmit(result.entity.message ?: "Ошибка загрузки маршрутов")
                        _routesByDay.value = emptyMap()
                        _isLoading.value = false
                    }
                    else -> {
                        _routesByDay.value = emptyMap()
                    }
                }
            }
        }
    }

    /**
     * Save selected month and reload routes.
     */
    fun setCurrentMonth(yearAndMonth: Pair<Int, Int>) {
        viewModelScope.launch {
            calendarUseCase.loadFlowMonthOfYearListState().collect { list ->
                val found = list.find { it.year == yearAndMonth.first && it.month == yearAndMonth.second }
                found?.let { month ->
                    settingsUseCase.setCurrentMonthOfYear(month).collect { result ->
                        if (result is ResultState.Success) {
                            _currentMonth.value = month
                            try {
                                val userSettings = settingsUseCase.getUserSettingFlow().first()
                                _selectedDays.value = emptyMap()
                                loadRoutesForCurrentMonth(userSettings.timeZone)
                            } catch (t: Throwable) {
                                if (t is CancellationException) throw t
                                _messageEvent.tryEmit("Ошибка при перезагрузке маршрутов: ${t.message ?: "неизвестная ошибка"}")
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Activate a time button (so subsequent calendar clicks create routes with this time).
     */
    fun setActiveTime(time: Long?) {
        _activeTime.value = time
        if (time == null) {
            _selectedDays.value = emptyMap()
        }
    }

    fun saveSelectedSchedules(workDuration: Long? = null) {
        val month = _currentMonth.value ?: run {
            _messageEvent.tryEmit("Месяц не выбран")
            return
        }

        val selectedMap = _selectedDays.value
        if (selectedMap.isEmpty()) {
            _messageEvent.tryEmit("Не выбрано ни одного дня")
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                for ((day, timesInLong) in selectedMap) {
                    timesInLong.forEach { time ->
                        val hour = ConverterLongToTime.getHour(time)
                        val minute = ConverterLongToTime.getRemainingMinuteFromHour(time)

                        // Use kotlinx.datetime instead of java.util.Calendar
                        val ld = LocalDate(month.year, month.month + 1, day)
                        val startMs = ldtToMillis(ld, hour, minute)
                        val endMillis = workDuration.plus(startMs)
                        val routeToSave = createRouteForStartTime(startMs, endMillis)

                        val res = routeUseCase.saveRoute(routeToSave)
                            .first { it is ResultState.Success || it is ResultState.Error }
                        if (res is ResultState.Error) {
                            _messageEvent.tryEmit("Ошибка при сохранении для дня $day: ${res.entity.message}")
                        }
                    }
                }
                _selectedDays.value = emptyMap()
                _activeTime.value = null
                _messageEvent.tryEmit("Маршруты сохранены")
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _messageEvent.tryEmit("Ошибка сохранения: ${t.message ?: "неизвестная ошибка"}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun ldtToMillis(date: LocalDate, hour: Int, minute: Int): Long {
        val tz = TimeZone.currentSystemDefault()
        val ldt = kotlinx.datetime.LocalDateTime(
            date.year, date.monthNumber, date.dayOfMonth, hour, minute, 0, 0
        )
        return ldt.toInstant(tz).toEpochMilliseconds()
    }

    private fun createRouteForStartTime(timeStartWork: Long, timeEndWork: Long?): Route {
        return Route(
            basicData = BasicData(
                timeStartWork = timeStartWork,
                timeEndWork = timeEndWork
            )
        )
    }

    private fun kotlinx.datetime.LocalDateTime.toInstant(tz: TimeZone): Instant {
        return kotlinx.datetime.LocalDateTime(year, monthNumber, dayOfMonth, hour, minute, second, nanosecond)
            .let { ldt ->
                val instant = kotlinx.datetime.TimeZone.currentSystemDefault()
                ldt.toInstant(instant)
            }
    }

    fun addCustomTime(time: Long): Boolean {
        if (!_timeButtons.value.contains(time)) {
            _timeButtons.value = _timeButtons.value + time
            viewModelScope.launch {
                val currentSetting = settingsUseCase.getUserSettingFlow().first()
                settingsUseCase.saveSetting(
                    settings = currentSetting.copy(
                        standardTimesStartWork = _timeButtons.value
                    )
                ).collect { }
            }
        }
        return true
    }

    fun removeCustomTime(time: Long) {
        _timeButtons.value = _timeButtons.value - time
        viewModelScope.launch {
            val currentSetting = settingsUseCase.getUserSettingFlow().first()
            settingsUseCase.saveSetting(
                settings = currentSetting.copy(
                    standardTimesStartWork = _timeButtons.value
                )
            ).collect { }
        }
        _activeTime.value = null
    }

    fun resetSelectedDays() {
        _selectedDays.value = emptyMap()
    }

    fun toggleDeleteMode() {
        _selectedDays.value = emptyMap()
        _isDeleteMode.value = !_isDeleteMode.value
        if (!_isDeleteMode.value) {
            _selectedRoutesToDelete.value = emptyMap()
        }
    }

    fun onDayDeleteClicked(day: Int) {
        val routes = _routesByDay.value[day] ?: emptyList()
        if (routes.isEmpty()) {
            _messageEvent.tryEmit("Выбери дни в которых есть маршрут")
            return
        }
        if (routes.size == 1) {
            val routeId = routes.first().basicData.id
            toggleRouteSelection(day, routeId)
        } else {
            viewModelScope.launch {
                _deleteDialogRequests.emit(DeleteDialogRequest(day = day, routes = routes))
            }
        }
    }

    fun toggleRouteSelection(day: Int, routeId: String) {
        val mutable = _selectedRoutesToDelete.value.toMutableMap()
        val setForDay = mutable[day]?.toMutableSet() ?: mutableSetOf()
        if (setForDay.contains(routeId)) setForDay.remove(routeId) else setForDay.add(routeId)
        if (setForDay.isEmpty()) mutable.remove(day) else mutable[day] = setForDay
        _selectedRoutesToDelete.value = mutable
    }

    fun setSelectedRoutesForDay(day: Int, routeIds: Set<String>) {
        val mutable = _selectedRoutesToDelete.value.toMutableMap()
        if (routeIds.isEmpty()) mutable.remove(day) else mutable[day] = routeIds
        _selectedRoutesToDelete.value = mutable
    }

    fun deleteSelectedRoutes() {
        val mapToDelete = _selectedRoutesToDelete.value
        if (mapToDelete.isEmpty()) {
            _messageEvent.tryEmit("Не выбрано ни одного маршрута для удаления")
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val errors = mutableListOf<String>()
                val routeIndex = mutableMapOf<String, Route>()
                _routesByDay.value.values.flatten()
                    .forEach { r -> routeIndex[r.basicData.id] = r }

                for ((day, ids) in mapToDelete) {
                    for (id in ids) {
                        val route = routeIndex[id]
                        if (route != null) {
                            routeUseCase.removeRoute(route).collect { result ->
                                when (result) {
                                    is ResultState.Success -> { /* ok */ }
                                    is ResultState.Error -> errors.add(
                                        result.entity.message ?: "Ошибка при удалении"
                                    )
                                    else -> {}
                                }
                            }
                        } else {
                            errors.add("Маршрут $id не найден для дня $day")
                        }
                    }
                }

                _selectedRoutesToDelete.value = emptyMap()
                _isDeleteMode.value = false

                if (errors.isEmpty()) {
                    _messageEvent.tryEmit("Маршруты удалены")
                } else {
                    _messageEvent.tryEmit("Некоторые удаления завершились с ошибками: ${errors.joinToString("; ")}")
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _messageEvent.tryEmit("Ошибка при удалении: ${t.message ?: "неизвестная ошибка"}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    companion object {
        val monthNames = listOf(
            "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь",
        )

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

        fun getFirstDayOfWeekInMonth(year: Int, month: Int): Int {
            val y = if (month < 3) year - 1 else year
            val m = if (month < 3) month + 12 else month
            val k = y % 100
            val j = y / 100
            val h = (1 + (13 * (m + 1)) / 5 + k + k / 4 + j / 4 - 2 * j) % 7
            return ((h + 5) % 7) + 1
        }
    }
}
