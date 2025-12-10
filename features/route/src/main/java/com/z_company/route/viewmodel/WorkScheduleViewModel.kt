package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.core.util.ConverterLongToTime
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar
import com.z_company.core.ui.snackbar.ISnackbarManager
import androidx.compose.material3.SnackbarDuration
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.util.plus
import com.z_company.route.viewmodel.home_view_model.AlertBeforePurchasesEvent
import com.z_company.use_case.SubscriptionHelper
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.collect
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.collections.sorted


/**
 * ViewModel for WorkSchedule screen.
 *
 * Responsibilities:
 * - load current month from settingsUseCase.getUserSettingFlow() before showing screen
 * - load available months/years (calendarUseCase)
 * - load routes for selected month via routeUseCase.listRoutesByMonth(...)
 * - keep selection state for time buttons and for selected calendar days (for creating / removing routes)
 * - saving: create routes for selected days and call routeUseCase.saveRoute(route) for each
 *
 * NOTE: Building a valid domain Route instance depends on your domain model (BasicData, Route constructors).
 * Below there is a placeholder createRouteForStartTime() where you should construct a Route with required fields.
 * Replace
 */
class WorkScheduleViewModel() : ViewModel(), KoinComponent {
    private val settingsUseCase: SettingsUseCase by inject()
    private val calendarUseCase: CalendarUseCase by inject()
    private val routeUseCase: RouteUseCase by inject()
    private val routeHelper: RouteActionsHelper by inject()
    private val subscriptionHelper: SubscriptionHelper by inject()

    private val _isDeleteMode = MutableStateFlow(false)
    val isDeleteMode: StateFlow<Boolean> = _isDeleteMode.asStateFlow()

    private val _selectedRoutesToDelete = MutableStateFlow<Map<Int, Set<String>>>(emptyMap())
    val selectedRoutesToDelete: StateFlow<Map<Int, Set<String>>> =
        _selectedRoutesToDelete.asStateFlow()

    data class DeleteDialogRequest(val day: Int, val routes: List<Route>)

    private val _deleteDialogRequests =
        MutableSharedFlow<DeleteDialogRequest>(extraBufferCapacity = 5)
    val deleteDialogRequests = _deleteDialogRequests.asSharedFlow()

    private val snackbarManager: ISnackbarManager by inject()

    // UI state flows exposed to compose
    private val _currentMonth = MutableStateFlow<MonthOfYear?>(null)
    val currentMonth: StateFlow<MonthOfYear?> = _currentMonth

    private val _userSettings = MutableStateFlow<UserSettings?>(null)
    val userSettings: StateFlow<UserSettings?> = _userSettings

    private val _totalTimeWork = MutableStateFlow<Long?>(null)
    val totalTimeWork: StateFlow<Long?> = _totalTimeWork


    private val _dateAndTimeConverter = MutableStateFlow<DateAndTimeConverter?>(null)
    val dateAndTimeConverter: StateFlow<DateAndTimeConverter?> = _dateAndTimeConverter

    private val _monthList = MutableStateFlow<List<Int>>(emptyList())
    val monthList: StateFlow<List<Int>> = _monthList

    private val _yearList = MutableStateFlow<List<Int>>(emptyList())
    val yearList: StateFlow<List<Int>> = _yearList

    // map day-of-month (1..31) -> list of routes that start that day
    private val _routesByDay =
        MutableStateFlow<Map<Int, List<Route>>>(emptyMap())
    val routesByDay: StateFlow<Map<Int, List<Route>>> =
        _routesByDay

    // Time buttons (strings "08:00", "20:00", plus custom)
    private val _timeButtons = MutableStateFlow(listOf<Long>())
    val timeButtons: StateFlow<List<Long>> = _timeButtons

    // Active time selection (the button that is currently "active" for creating schedules)
    private val _activeTime = MutableStateFlow<Long?>(null)
    val activeTime: StateFlow<Long?> = _activeTime

    private val _selectedDays = MutableStateFlow<Map<Int, List<Long>>>(emptyMap())
    val selectedDays: StateFlow<Map<Int, List<Long>>> = _selectedDays.asStateFlow()

    private val _alertBeforePurchasesEvent = MutableSharedFlow<AlertBeforePurchasesEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val alertBeforePurchasesEvent = _alertBeforePurchasesEvent.asSharedFlow()


    /**
     * Toggle single planned time for a day (add if missing, remove if exists).
     * timeInMillis is time-from-midnight (hour*3600000 + minute*60000), same format as your chips.
     */
    fun togglePlannedTimeForDay(day: Int, timeInMillis: Long) {
        val mutable = _selectedDays.value.toMutableMap()
        val list = mutable[day]?.toMutableList() ?: mutableListOf()
        val idx = list.indexOf(timeInMillis)
        if (idx >= 0) {
            // remove that time occurrence (if duplicates allowed remove one)
            list.removeAt(idx)
        } else {
            list.add(timeInMillis)
        }
        if (list.isEmpty()) mutable.remove(day) else mutable[day] = list.toList()
        _selectedDays.value = mutable
    }

    // Loading state for save operation or initial load
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    /**
     * Call before showing the screen: fetch current month from settings and load months/year lists and routes.
     */
    fun prepareScreen() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // get user settings (this returns a Flow<UserSettings> in your app).
                settingsUseCase.getUserSettingFlow().collect { setting ->
                    // userSettings.selectMonthOfYear is expected to be MonthOfYear
                    val month = setting.selectMonthOfYear
                    _userSettings.value = setting
                    _timeButtons.value = setting.standardTimesStartWork
                    _currentMonth.value = month
                    _dateAndTimeConverter.value = DateAndTimeConverter(setting)

                    // load month/year list to populate bottom sheet
                    loadMonthYearLists()

                    // load routes for the month
                    loadRoutesForCurrentMonth(setting.timeZone)
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                snackbarManager.show(
                    message = "Ошибка загрузки: ${t.message ?: t.javaClass.simpleName}",
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadMonthYearLists() {
        // get month list from calendarUseCase
        withContext(Dispatchers.IO) {
            val list = calendarUseCase.loadFlowMonthOfYearListState().first()
            // list is List<MonthOfYear> - transform
            _monthList.value = list.map { it.month }.distinct().sorted()
            _yearList.value = list.map { it.year }.distinct().sorted()
        }
    }

    private suspend fun loadRoutesForCurrentMonth(timeZone: Long) {
        val month = _currentMonth.value ?: return
        // collect first ResultState from routeUseCase.listRoutesByMonth
        withContext(Dispatchers.IO) {
            routeUseCase.listRoutesByMonth(month, timeZone).collect { result ->
                when (result) {
                    is ResultState.Success -> {
                        val routes = result.data
                        // Map routes by day-of-month: use timezone offset provided
                        val map =
                            mutableMapOf<Int, MutableList<Route>>()
                        routes.forEach { route ->
                            val start = route.basicData.timeStartWork
                            if (start != null) {
                                val cal = Calendar.getInstance()
                                    .also { it.timeInMillis = start + timeZone }
                                val day = cal.get(Calendar.DAY_OF_MONTH)
                                map.getOrPut(day) { mutableListOf() }.add(route)
                            }
                        }

                        userSettings.value?.let { settings ->
                            val totalTimeWork =
                                routes.getWorkTime(settings.selectMonthOfYear, settings.timeZone)
                            _totalTimeWork.value = totalTimeWork
                        }
                        _routesByDay.value = map
                    }

                    is ResultState.Error -> {
                        snackbarManager.show(
                            message = result.entity.message ?: "Ошибка загрузки маршрутов",
                        )
                        _routesByDay.value = emptyMap()
                    }

                    else -> {
                        _routesByDay.value = emptyMap()
                    }
                }
            }
        }
    }

    fun checkPurchasesAvailability() {
        viewModelScope.launch(Dispatchers.IO) {
            when (val checkResult = subscriptionHelper.checkPurchasesAvailabilitySuspend()) {
                is ResultState.Success -> {
                    snackbarManager.show(message = "Подписки доступны")
                }

                is ResultState.Error -> {
                    snackbarManager.show(
                        message = checkResult.entity.message
                            ?: "Ошибка. Подписки пока недоступны"
                    )
                }

                else -> {}
            }
        }
    }

    fun restorePurchases() {
        viewModelScope.launch(Dispatchers.IO) {
            subscriptionHelper.restorePurchases(snackbarManager)
        }
    }

    suspend fun newRouteClick(workDuration: Long? = null) {
        when (val decision = routeHelper.newRouteClick()) {
            is RouteActionsHelper.NewRouteResult.NeedSubscribeDialog -> {
                _alertBeforePurchasesEvent.tryEmit(AlertBeforePurchasesEvent.ShowDialogNeedSubscribe)
            }

            is RouteActionsHelper.NewRouteResult.AlertSubscribeDialog -> {
                saveSelectedSchedules(workDuration)
            }

            is RouteActionsHelper.NewRouteResult.ShowNewRouteScreen -> {
                saveSelectedSchedules(workDuration)
            }

            is RouteActionsHelper.NewRouteResult.Error -> {
                snackbarManager.show(
                    message = decision.throwable?.message ?: "Ошибка",
                )
            }
        }
    }

    /**
     * Save selected month into local settings (as provided in the prompt)
     * and reload routes on success.
     *
     * The given snippet in the prompt has collection on settingsUseCase.setCurrentMonthOfYear(month).
     * We reproduce the same logic: iterate calendar list, find, save, and reload routes after success.
     */
    fun setCurrentMonth(yearAndMonth: Pair<Int, Int>) {
        viewModelScope.launch {
            calendarUseCase.loadFlowMonthOfYearListState().collect { list ->
                val found =
                    list.find { it.year == yearAndMonth.first && it.month == yearAndMonth.second }
                found?.let { month ->
                    // save in local settings
                    settingsUseCase.setCurrentMonthOfYear(month).collect { result ->
                        if (result is ResultState.Success) {
                            // update local month and reload routes
                            _currentMonth.value = month
                            // fetch userSettings again to get tz; we will use settingsUseCase.getUserSettingFlow().first()
                            try {
                                val userSettings = settingsUseCase.getUserSettingFlow().first()
                                _selectedDays.value = emptyMap()
                                loadRoutesForCurrentMonth(userSettings.timeZone)
                            } catch (t: Throwable) {
                                if (t is CancellationException) throw t
                                snackbarManager.show(
                                    message = "Ошибка при перезагрузке маршрутов: ${t.message ?: t.javaClass.simpleName}",
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Activate a time button (so subsequent calendar clicks create routes with this time).
     * Passing null deactivates create mode.
     */
    fun setActiveTime(time: Long?) {
        _activeTime.value = time
        if (time == null) {
            _selectedDays.value = emptyMap()
        }
    }

    suspend fun saveSelectedSchedules(workDuration: Long? = null) {
        val month = _currentMonth.value ?: run {
            snackbarManager.show(
                message = "Месяц не выбран",
                actionLabel = null,
                duration = SnackbarDuration.Short,
                onAction = null,
                showOnceKey = null
            )
            return
        }

        val selectedMap = _selectedDays.value
        if (selectedMap.isEmpty()) {
            snackbarManager.show(
                message = "Не выбрано ни одного дня",
                actionLabel = null,
                duration = SnackbarDuration.Short,
                onAction = null,
                showOnceKey = null
            )
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                for ((day, timesInLong) in selectedMap) {
                    timesInLong.forEach { time ->
                        val hour = ConverterLongToTime.getHour(time)
                        val minute = ConverterLongToTime.getRemainingMinuteFromHour(time)

                        val cal = Calendar.getInstance().also {
                            it.set(Calendar.YEAR, month.year)
                            it.set(Calendar.MONTH, month.month)
                            it.set(Calendar.DAY_OF_MONTH, day)
                            it.set(Calendar.HOUR_OF_DAY, hour)
                            it.set(Calendar.MINUTE, minute)
                            it.set(Calendar.SECOND, 0)
                            it.set(Calendar.MILLISECOND, 0)
                        }
                        val startMillis =
                            cal.timeInMillis // or adjust with tzOffset if your codebase needs (startMillis - tzOffset or +tz)

                        val endMillis = workDuration.plus(startMillis)
//                            ?.let { duration ->
//                            // если workDuration передаётся как millis from midnight (или как длина?) — адаптируйте
//                            // Предположим workDuration — millis from midnight (как в вашем TimePicker)
//                            val endHour = ConverterLongToTime.getHour(duration)
//
//                            val endMinute =
//                                ConverterLongToTime.getRemainingMinuteFromHour(duration)
//                            val calEnd = Calendar.getInstance().also {
//                                it.set(Calendar.YEAR, month.year)
//                                it.set(Calendar.MONTH, month.month)
//                                it.set(Calendar.DAY_OF_MONTH, day)
//                                it.set(Calendar.HOUR_OF_DAY, endHour)
//                                it.set(Calendar.MINUTE, endMinute)
//                                it.set(Calendar.SECOND, 0)
//                                it.set(Calendar.MILLISECOND, 0)
//                            }
//                            var rawEnd = calEnd.timeInMillis
//                            if (rawEnd <= startMillis) rawEnd += 24 * 3_600_000L // перенос на следующий день
//                            rawEnd
//                        }

                        val routeToSave =
                            createRouteForStartTime(startMillis, endMillis)


                        // используем first { Success || Error } чтобы не застрять на незавершающем collect
                        val res = routeUseCase.saveRoute(routeToSave)
                            .first { it is ResultState.Success || it is ResultState.Error }
                        if (res is ResultState.Error) {
                            snackbarManager.show(
                                message = "Ошибка при сохранении для дня $day: ${res.entity.message}",
                            )
                        }
                    }
                }
                _selectedDays.value = emptyMap()
                _activeTime.value = null
                snackbarManager.show(
                    message = "Маршруты сохранены",
                )
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                snackbarManager.show(
                    message = "Ошибка сохранения: ${t.message ?: t.javaClass.simpleName}",
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Placeholder: создайте маршрут с минимальным количеством обязательных полей и заданным временем начала.
     *
     * return Route(basicData = BasicData(timeStartWork = startMillis), locomotives = mutableListOf(), trains = mutableListOf(), passengers = mutableListOf(), photos = mutableListOf())
     */
    private fun createRouteForStartTime(timeStartWork: Long, timeEndWork: Long?): Route {
        return Route(
            basicData = BasicData(
                timeStartWork = timeStartWork,
                timeEndWork = timeEndWork
            )
        )
    }

    /**
     * Add a custom time button.
     * Returns true if added ok.
     */
    fun addCustomTime(time: Long): Boolean {
        if (!_timeButtons.value.contains(time)) {
            _timeButtons.value = _timeButtons.value + time
            viewModelScope.launch {
                val currentSetting = settingsUseCase.getUserSettingFlow().first()
                settingsUseCase.saveSetting(
                    settings = currentSetting.copy(
                        standardTimesStartWork = _timeButtons.value
                    )
                ).collect()
            }
        }
        return true
    }

    /**
     * Remove custom time button (if user wants). Keep built-ins.
     */

    fun removeCustomTime(time: Long) {
        _timeButtons.value = _timeButtons.value - time
        viewModelScope.launch {
            val currentSetting = settingsUseCase.getUserSettingFlow().first()
            settingsUseCase.saveSetting(
                settings = currentSetting.copy(
                    standardTimesStartWork = _timeButtons.value
                )
            ).collect()
        }
        _activeTime.value = null
    }

    fun resetSelectedDays() {
        _selectedDays.value = emptyMap()
    }

    /** Включить/выключить режим удаления
     */
    fun toggleDeleteMode() {
        _selectedDays.value = emptyMap()
        _isDeleteMode.value = !_isDeleteMode.value
        if (!_isDeleteMode.value) {
            // очистка при выходе
            _selectedRoutesToDelete.value = emptyMap()
        }
    }

    /** Обработчик клика по дню в режиме удаления
     */
    fun onDayDeleteClicked(day: Int) {
        val routes = _routesByDay.value[day] ?: emptyList()
        if (routes.isEmpty()) {
            snackbarManager.show(
                message = "Выбери дни в которых есть маршрут",
            )
            return
        }

        if (routes.size == 1) {
            // одна запись — просто переключаем пометку для этого маршрута
            val routeId = routes.first().basicData.id
            toggleRouteSelection(day, routeId)
        } else {
            // несколько маршрутов — попросим UI показать диалог выбора
            viewModelScope.launch {
                _deleteDialogRequests.emit(DeleteDialogRequest(day = day, routes = routes))
            }
        }
    }

    // Переключение routeId в списке удаления для конкретного дня
    fun toggleRouteSelection(day: Int, routeId: String) {
        val mutable = _selectedRoutesToDelete.value.toMutableMap()
        val setForDay = mutable[day]?.toMutableSet() ?: mutableSetOf()
        if (setForDay.contains(routeId)) setForDay.remove(routeId) else setForDay.add(routeId)
        if (setForDay.isEmpty()) mutable.remove(day) else mutable[day] = setForDay
        _selectedRoutesToDelete.value = mutable
    }

    // Устанавливаем конкретный набор выбранных routeId для дня (используется из UI после подтверждения диалога)
    fun setSelectedRoutesForDay(day: Int, routeIds: Set<String>) {
        val mutable = _selectedRoutesToDelete.value.toMutableMap()
        if (routeIds.isEmpty()) mutable.remove(day) else mutable[day] = routeIds
        _selectedRoutesToDelete.value = mutable
    }

    // Удаление выбранных маршрутов
    fun deleteSelectedRoutes() {
        val mapToDelete = _selectedRoutesToDelete.value
        if (mapToDelete.isEmpty()) {
            snackbarManager.show(
                message = "Не выбрано ни одного маршрута для удаления",
            )
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val errors = mutableListOf<String>()

                // For convenience, build a map of routeId -> Route from current loaded routesByDay
                val routeIndex = mutableMapOf<String, Route>()
                _routesByDay.value.values.flatten()
                    .forEach { r -> routeIndex[r.basicData.id] = r }

                for ((day, ids) in mapToDelete) {
                    for (id in ids) {
                        val route = routeIndex[id]
                        if (route != null) {
                            routeUseCase.removeRoute(route).collect { result ->
                                when (result) {
                                    is ResultState.Success -> { /* ok */
                                    }

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

                // сбрасываем состояние
                _selectedRoutesToDelete.value = emptyMap()
                _isDeleteMode.value = false

                if (errors.isEmpty()) {
                    snackbarManager.show(
                        message = "Маршруты удалены",
                    )
                } else {
                    snackbarManager.show(
                        message = "Некоторые удаления завершились с ошибками: ${
                            errors.joinToString(
                                "; "
                            )
                        }",
                    )
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                snackbarManager.show(
                    message = "Ошибка при удалении: ${t.message ?: t.javaClass.simpleName}",
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
}