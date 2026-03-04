package com.z_company.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.UtilForMonthOfYear.getDayoffHours
import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHours
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.findCurrentRoute
import com.z_company.domain.entities.route.UtilsForEntities.findNextFutureRoute
import com.z_company.domain.entities.route.UtilsForEntities.getNightTime
import com.z_company.domain.entities.route.UtilsForEntities.getOnePersonOperationTime
import com.z_company.domain.entities.route.UtilsForEntities.getOnePersonOperationTimePassengerTrain
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getSingleLocomotiveTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTimeWithoutHoliday
import com.z_company.domain.entities.route.UtilsForEntities.getWorkingTimeOnAHoliday
import com.z_company.domain.entities.route.UtilsForEntities.isExtendedServicePhaseTrains
import com.z_company.domain.entities.route.UtilsForEntities.isHeavyTrains
import com.z_company.domain.entities.route.UtilsForEntities.isHolidayTimeInRoute
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.shared.util.ConverterLongToTime
import com.z_company.shared.util.DateAndTimeConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

// region Data classes (mirrors Android HomeUiState, ItemState)

data class HomeUiState(
    val routeListState: ResultState<List<Route>> = ResultState.Loading(),
    val settingState: ResultState<UserSettings?> = ResultState.Loading(),
    val monthSelected: ResultState<MonthOfYear?> = ResultState.Loading(),
    val minTimeRest: Long? = null,
    val minTimeHomeRest: Long? = null,
    val totalTimeWithHoliday: ResultState<Long>? = ResultState.Loading(),
    val nightTimeInRouteList: ResultState<Long>? = ResultState.Loading(),
    val singleLocomotiveTimeState: ResultState<Long>? = ResultState.Loading(),
    val passengerTimeInRouteList: ResultState<Long>? = ResultState.Loading(),
    val extendedServicePhaseTime: ResultState<Long>? = ResultState.Loading(),
    val longDistanceTrainsTime: ResultState<Long>? = ResultState.Loading(),
    val heavyTrainsTime: ResultState<Long>? = ResultState.Loading(),
    val onePersonOperationTime: ResultState<Long>? = ResultState.Loading(),
    val dayOffHours: ResultState<Int>? = ResultState.Loading(),
    val holidayHours: ResultState<Long>? = ResultState.Loading(),
    val offsetInMoscow: Long = 0L,
    val listItemState: List<ItemState> = emptyList(),
    val removeRouteState: ResultState<Unit>? = null,
    val dateAndTimeConverter: DateAndTimeConverter? = null,
)

data class ItemState(
    val route: Route,
    val isHoliday: Boolean = false,
    val isExtendedServicePhaseTrains: Boolean = false,
    val isHeavyTrains: Boolean = false,
    val isTransition: Boolean = false,
    val isFuture: Boolean = false,
)

data class PreviewRouteUiState(
    val homeRest: Long? = null,
)

// endregion

/**
 * Shared KMP ViewModel for the Home screen — full port of Android HomeViewModel.
 *
 * Supports: routes by month, all work-time calculations, timers,
 * current/next route tracking, month navigation, remove/favorite.
 *
 * Android-only features NOT ported: RuStore update, WidgetUpdater, cloud sync, onGoClicked.
 */
class HomeSharedViewModel(
    private val routeUseCase: RouteUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val calendarUseCase: CalendarUseCase,
    private val salarySettingUseCase: SalarySettingUseCase,
) : ViewModel() {

    // region State

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _previewRouteUiState = MutableStateFlow(PreviewRouteUiState())
    val previewRouteUiState: StateFlow<PreviewRouteUiState> = _previewRouteUiState.asStateFlow()

    private val _monthList = MutableStateFlow<List<Int>>(emptyList())
    val monthList: StateFlow<List<Int>> = _monthList.asStateFlow()

    private val _yearList = MutableStateFlow<List<Int>>(emptyList())
    val yearList: StateFlow<List<Int>> = _yearList.asStateFlow()

    // Timers
    private val _workTimeInCurrentRoute = MutableSharedFlow<Long>(replay = 1)
    val workTimeInCurrentRoute: SharedFlow<Long> = _workTimeInCurrentRoute.asSharedFlow()

    private val _countdownToNextRoute = MutableSharedFlow<Long>(replay = 1)
    val countdownToNextRoute: SharedFlow<Long> = _countdownToNextRoute.asSharedFlow()

    // Current/next route tracking
    private val _currentRoute = MutableStateFlow<Route?>(null)
    val currentRoute: StateFlow<Route?> = _currentRoute.asStateFlow()

    private val _nextFutureRoute = MutableStateFlow<Route?>(null)
    val nextFutureRoute: StateFlow<Route?> = _nextFutureRoute.asStateFlow()

    // Work time without holiday (for display on page 0)
    private val _timeWithoutHoliday = MutableStateFlow(0L)
    val timeWithoutHoliday: StateFlow<Long> = _timeWithoutHoliday.asStateFlow()

    // endregion

    // region Internal state

    private val routeParams = MutableStateFlow<Pair<MonthOfYear, Long>?>(null)
    private var currentSalarySetting: SalarySetting? = null
    private var currentUserSetting: UserSettings? = null
    private var currentMonthOfYear: MonthOfYear? = null

    private var timerJob: Job? = null
    private var countdownTimerJob: Job? = null
    private var removeRouteJob: Job? = null

    // endregion

    // region Routes flow (reactive to routeParams changes)

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private val routesFlow = routeParams
        .filterNotNull()
        .debounce(300)
        .flatMapLatest { (month, tz) ->
            routeUseCase.listRoutesByMonth(month, tz)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ResultState.Loading()
        )

    // endregion

    init {
        // Calendar months/years
        viewModelScope.launch {
            calendarUseCase.loadFlowMonthOfYearListState().collect { list ->
                _monthList.value = list.map { it.month }.distinct().sorted()
                _yearList.value = list.map { it.year }.distinct().sorted()
            }
        }

        // Combined settings + salary → triggers route loading
        val combinedData = combine(
            salarySettingUseCase.salarySettingFlow().map { it as SalarySetting? }.onStart { emit(null) },
            settingsUseCase.getUserSettingFlow().map { it as UserSettings? }.onStart { emit(null) },
        ) { salary, user -> Pair(user, salary) }

        viewModelScope.launch {
            combinedData.collectLatest { (user, salary) ->
                currentUserSetting = user
                currentSalarySetting = salary
                if (user == null || salary == null) return@collectLatest

                val converter = DateAndTimeConverter(user)
                currentMonthOfYear = user.selectMonthOfYear
                _uiState.update {
                    it.copy(
                        settingState = ResultState.Success(user),
                        monthSelected = ResultState.Success(user.selectMonthOfYear),
                        offsetInMoscow = user.timeZone,
                        minTimeRest = user.minTimeRestPointOfTurnover,
                        minTimeHomeRest = user.minTimeHomeRest,
                        dateAndTimeConverter = converter,
                    )
                }
                getDayOffTime(user.selectMonthOfYear)
                routeParams.value = Pair(user.selectMonthOfYear, user.timeZone)
            }
        }

        // Process routes when they arrive
        viewModelScope.launch {
            routesFlow.collectLatest { result ->
                when (result) {
                    is ResultState.Loading -> {
                        _uiState.update { it.copy(routeListState = ResultState.Loading()) }
                    }

                    is ResultState.Success -> {
                        val routes = result.data
                        val user = currentUserSetting ?: return@collectLatest
                        val salary = currentSalarySetting ?: return@collectLatest
                        val month = currentMonthOfYear ?: return@collectLatest

                        // Build ItemState list
                        val items = routes.map { route ->
                            ItemState(
                                route = route,
                                isHoliday = runCatching {
                                    isHolidayTimeInRoute(month, user, route)
                                }.getOrDefault(false),
                                isHeavyTrains = runCatching {
                                    isHeavyTrains(salary, route)
                                }.getOrDefault(false),
                                isExtendedServicePhaseTrains = runCatching {
                                    isExtendedServicePhaseTrains(salary, route)
                                }.getOrDefault(false),
                            )
                        }

                        _uiState.update {
                            it.copy(
                                routeListState = ResultState.Success(routes),
                                listItemState = items,
                            )
                        }

                        // Track current and next future route
                        val now = Clock.System.now().toEpochMilliseconds()
                        _currentRoute.value = routes.findCurrentRoute(now, user)
                        _nextFutureRoute.value = routes.findNextFutureRoute(now)

                        // Start timer if current route exists
                        _currentRoute.value?.basicData?.timeStartWork?.let { startWork ->
                            workTimer(startWork)
                        }
                        // Start countdown if next route exists
                        if (_currentRoute.value == null) {
                            _nextFutureRoute.value?.basicData?.timeStartWork?.let { startWork ->
                                countdownTimer(startWork)
                            }
                        }

                        // Run all calculations
                        launchCalculations(routes, user)
                    }

                    is ResultState.Error -> {
                        _uiState.update {
                            it.copy(routeListState = ResultState.Error(result.entity))
                        }
                    }
                }
            }
        }
    }

    // region Calculations

    private fun launchCalculations(routes: List<Route>, user: UserSettings) {
        val offset = user.timeZone
        calculationTotalTime(routes, offset)
        calculationOfNightTime(routes, user)
        calculationPassengerTime(routes, offset)
        calculationOfSingleLocomotiveTime(routes)
        calculationOfOnePersonOperationTime(routes, user)
        calculationHolidayTime(routes, offset)
        calculationOfTimeWithoutHoliday(routes, offset)
    }

    private fun calculationTotalTime(routes: List<Route>, offsetInMoscow: Long) {
        _uiState.update { it.copy(totalTimeWithHoliday = ResultState.Loading()) }
        try {
            currentMonthOfYear?.let { month ->
                val totalTime = routes.getWorkTime(month, offsetInMoscow)
                _uiState.update { it.copy(totalTimeWithHoliday = ResultState.Success(totalTime)) }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(totalTimeWithHoliday = ResultState.Error(ErrorEntity(e))) }
        }
    }

    private fun calculationOfNightTime(routes: List<Route>, settings: UserSettings) {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(nightTimeInRouteList = ResultState.Loading()) }
            try {
                val nightTime = routes.getNightTime(settings)
                _uiState.update { it.copy(nightTimeInRouteList = ResultState.Success(nightTime)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(nightTimeInRouteList = ResultState.Error(ErrorEntity(e))) }
            }
        }
    }

    private fun calculationPassengerTime(routes: List<Route>, offsetInMoscow: Long) {
        _uiState.update { it.copy(passengerTimeInRouteList = ResultState.Loading()) }
        try {
            currentMonthOfYear?.let { month ->
                val passengerTime = routes.getPassengerTime(month, offsetInMoscow)
                _uiState.update {
                    it.copy(passengerTimeInRouteList = ResultState.Success(passengerTime))
                }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(passengerTimeInRouteList = ResultState.Error(ErrorEntity(e))) }
        }
    }

    private fun calculationOfSingleLocomotiveTime(routes: List<Route>) {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(singleLocomotiveTimeState = ResultState.Loading()) }
            try {
                val time = routes.getSingleLocomotiveTime()
                _uiState.update { it.copy(singleLocomotiveTimeState = ResultState.Success(time)) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(singleLocomotiveTimeState = ResultState.Error(ErrorEntity(e)))
                }
            }
        }
    }

    private fun calculationOfOnePersonOperationTime(
        routes: List<Route>,
        userSettings: UserSettings,
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(onePersonOperationTime = ResultState.Loading()) }
            try {
                currentMonthOfYear?.let { month ->
                    val passengerTime = routes.getOnePersonOperationTimePassengerTrain(
                        month, userSettings.timeZone
                    )
                    val time = routes.getOnePersonOperationTime(month, userSettings.timeZone)
                    _uiState.update {
                        it.copy(onePersonOperationTime = ResultState.Success(time + passengerTime))
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(onePersonOperationTime = ResultState.Error(ErrorEntity(e)))
                }
            }
        }
    }

    private fun calculationHolidayTime(routes: List<Route>, offsetInMoscow: Long) {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(holidayHours = ResultState.Loading()) }
            try {
                currentMonthOfYear?.let { month ->
                    val holidayTime =
                        routes.getWorkingTimeOnAHoliday(month, offsetInMoscow).first()
                    _uiState.update { it.copy(holidayHours = ResultState.Success(holidayTime)) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(holidayHours = ResultState.Error(ErrorEntity(e))) }
            }
        }
    }

    private fun calculationOfTimeWithoutHoliday(routes: List<Route>, offsetInMoscow: Long) {
        viewModelScope.launch(Dispatchers.Default) {
            currentMonthOfYear?.let { month ->
                _timeWithoutHoliday.value =
                    routes.getWorkTimeWithoutHoliday(month, offsetInMoscow)
            }
        }
    }

    private fun getDayOffTime(monthOfYear: MonthOfYear) {
        try {
            val dayOffHours = monthOfYear.getDayoffHours()
            _uiState.update { it.copy(dayOffHours = ResultState.Success(dayOffHours)) }
        } catch (e: Exception) {
            _uiState.update { it.copy(dayOffHours = ResultState.Error(ErrorEntity(e))) }
        }
    }

    fun calculationHomeRest(route: Route?) {
        viewModelScope.launch {
            if (route == null) return@launch
            val user = currentUserSetting ?: return@launch
            val endWork = route.basicData.timeEndWork ?: return@launch
            val startWork = route.basicData.timeStartWork ?: return@launch
            val workTime = endWork - startWork
            val rawDuration = (workTime.toDouble() * 2.6).toLong()
            val duration = maxOf(rawDuration, user.minTimeHomeRest)
            _previewRouteUiState.update { it.copy(homeRest = duration) }
        }
    }

    // endregion

    // region Timers

    fun workTimer(startWork: Long) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val offset = _uiState.value.offsetInMoscow
            val now = Clock.System.now().toEpochMilliseconds()
            val startWorkTime = startWork + offset
            var difference = now - startWorkTime

            _workTimeInCurrentRoute.tryEmit(difference)

            // Align to next minute
            val ldt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val remainingSec = 60 - ldt.second
            delay(remainingSec * 1000L)

            while (_currentRoute.value != null) {
                difference += 60_000L
                _workTimeInCurrentRoute.tryEmit(difference)
                delay(60_000L)
            }
        }
    }

    fun countdownTimer(startWork: Long) {
        countdownTimerJob?.cancel()
        countdownTimerJob = viewModelScope.launch {
            val offset = _uiState.value.offsetInMoscow
            val now = Clock.System.now().toEpochMilliseconds()
            val startWorkTime = startWork + offset

            var difference = startWorkTime - now
            if (difference <= 0) {
                _countdownToNextRoute.tryEmit(0L)
                return@launch
            }

            _countdownToNextRoute.tryEmit(difference)

            val ldt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val remainingSec = 60 - ldt.second
            delay(remainingSec * 1000L)

            while (difference > 0) {
                difference -= 60_000L
                if (difference <= 0) {
                    _countdownToNextRoute.tryEmit(0L)
                    _nextFutureRoute.value = null
                    break
                }
                _countdownToNextRoute.tryEmit(difference)
                delay(60_000L)
            }
        }
    }

    // endregion

    // region Time formatting

    fun convertTimeToStringFormat(timeToLong: Long?): String {
        currentUserSetting?.let { settings ->
            return if (settings.isDecimalTime) {
                ConverterLongToTime.getTimeInStringDecimalFormat(timeToLong)
            } else {
                ConverterLongToTime.getTimeInStringFormat(timeToLong)
            }
        }
        return ConverterLongToTime.getTimeInStringFormat(timeToLong)
    }

    // endregion

    // region Route actions

    fun removeRoute(route: Route) {
        removeRouteJob?.cancel()
        removeRouteJob = viewModelScope.launch {
            routeUseCase.markAsRemoved(route).collect { result ->
                when (result) {
                    is ResultState.Loading -> _uiState.update {
                        it.copy(removeRouteState = ResultState.Loading())
                    }

                    is ResultState.Success -> _uiState.update {
                        it.copy(removeRouteState = null)
                    }

                    is ResultState.Error -> _uiState.update {
                        it.copy(removeRouteState = ResultState.Error(result.entity))
                    }
                }
            }
        }
    }

    fun setFavoriteRoute(route: Route) {
        viewModelScope.launch {
            val updated = route.copy(
                basicData = route.basicData.copy(isFavorite = !route.basicData.isFavorite)
            )
            routeUseCase.saveRoute(updated).collect { /* ignored */ }
        }
    }

    // endregion

    // region Month navigation

    fun setCurrentMonth(yearAndMonth: Pair<Int, Int>) {
        viewModelScope.launch {
            calendarUseCase.loadFlowMonthOfYearListState().collect { list ->
                val found = list.find {
                    it.year == yearAndMonth.first && it.month == yearAndMonth.second
                }
                found?.let { month ->
                    currentMonthOfYear = month
                    currentUserSetting = currentUserSetting?.copy(selectMonthOfYear = month)
                    _uiState.update {
                        it.copy(
                            monthSelected = ResultState.Success(month),
                            settingState = ResultState.Success(currentUserSetting),
                        )
                    }
                    getDayOffTime(month)
                    settingsUseCase.setCurrentMonthOfYear(month).collect { /* result */ }
                }
            }
        }
    }

    fun setNextMonth() {
        val month = currentMonthOfYear ?: return
        val newMonth = if (month.month >= 11) 0 else month.month + 1
        val newYear = if (month.month >= 11) month.year + 1 else month.year
        setCurrentMonth(Pair(newYear, newMonth))
    }

    fun setPreviousMonth() {
        val month = currentMonthOfYear ?: return
        val newMonth = if (month.month <= 0) 11 else month.month - 1
        val newYear = if (month.month <= 0) month.year - 1 else month.year
        setCurrentMonth(Pair(newYear, newMonth))
    }

    // endregion

    fun clearError() {
        _uiState.update {
            it.copy(
                routeListState = when (it.routeListState) {
                    is ResultState.Error -> ResultState.Loading()
                    else -> it.routeListState
                }
            )
        }
    }
}
