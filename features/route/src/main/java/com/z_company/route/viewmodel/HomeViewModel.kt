package com.z_company.route.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.core.util.CalculateNightTime
import com.z_company.data_local.setting.DataStoreRepository
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.util.plus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar.DAY_OF_MONTH
import java.util.Calendar.HOUR_OF_DAY
import java.util.Calendar.MILLISECOND
import java.util.Calendar.MINUTE
import java.util.Calendar.MONTH
import java.util.Calendar.YEAR
import java.util.Calendar.getInstance
import com.z_company.domain.util.minus

class HomeViewModel : ViewModel(), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    private val calendarUseCase: CalendarUseCase by inject()
    private val dataStoreRepository: DataStoreRepository by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    var totalTime by mutableLongStateOf(0L)
        private set

    private var loadRouteJob: Job? = null
    private var removeRouteJob: Job? = null
    private var loadCalendarJob: Job? = null
    private var setCalendarJob: Job? = null

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    fun loadRoutes() {
        loadRouteJob?.cancel()
        viewModelScope.launch {
            loadRouteJob =
                routeUseCase.listRoutesByMonth(_uiState.value.monthSelected).onEach { result ->
                    _uiState.update {
                        it.copy(routeListState = result)
                    }
                    if (result is ResultState.Success) {
                        calculationOfTotalTime(result.data)
                        calculationOfNightTime(result.data)
                        calculationPassengerTime(result.data)
                    }
                }.launchIn(this)
        }
    }

    private fun calculationPassengerTime(routes: List<Route>) {
        var passengerTime by mutableLongStateOf(0L)
        routes.forEach { route ->
            route.passengers.forEach { passenger ->
                passengerTime = if (isTransition(passenger.timeDeparture, passenger.timeArrival)) {
                    passengerTime.plus(
                        getTimeInCurrentMonth(
                            passenger.timeDeparture!!,
                            passenger.timeArrival!!,
                            uiState.value.monthSelected
                        )
                    )
                } else {
                    passengerTime.plus((passenger.timeArrival - passenger.timeDeparture) ?: 0L)
                }
            }
        }
        _uiState.update {
            it.copy(
                passengerTimeInRouteList = passengerTime
            )
        }
    }

    private suspend fun calculationOfNightTime(routes: List<Route>) {
        viewModelScope.launch {
            settingsUseCase.getCurrentSettings().collect { result ->
                if (result is ResultState.Success) {
                    var nightTimeState by mutableStateOf<Long?>(0L)
                    var startNightHour by mutableIntStateOf(0)
                    var startNightMinute by mutableIntStateOf(0)
                    var endNightHour by mutableIntStateOf(0)
                    var endNightMinute by mutableIntStateOf(0)

                    result.data?.let {
                        startNightHour = it.nightTime.startNightHour
                        startNightMinute = it.nightTime.startNightMinute
                        endNightHour = it.nightTime.endNightHour
                        endNightMinute = it.nightTime.endNightMinute
                    }
                    routes.forEach { route ->
                        if (isTransition(
                                route.basicData.timeStartWork,
                                route.basicData.timeEndWork
                            )
                        ) {
                            val nightTimeInRoute = CalculateNightTime.getNightTimeTransitionRoute(
                                month = uiState.value.monthSelected.month,
                                year = uiState.value.monthSelected.year,
                                startMillis = route.basicData.timeStartWork,
                                endMillis = route.basicData.timeEndWork,
                                hourStart = startNightHour,
                                minuteStart = startNightMinute,
                                hourEnd = endNightHour,
                                minuteEnd = endNightMinute
                            )
                            nightTimeState = nightTimeState.plus(nightTimeInRoute ?: 0L)
                        } else {
                            val nightTimeInRoute = CalculateNightTime.getNightTime(
                                startMillis = route.basicData.timeStartWork,
                                endMillis = route.basicData.timeEndWork,
                                hourStart = startNightHour,
                                minuteStart = startNightMinute,
                                hourEnd = endNightHour,
                                minuteEnd = endNightMinute
                            )
                            nightTimeState = nightTimeState.plus(nightTimeInRoute ?: 0L)
                        }
                    }
                    _uiState.update {
                        it.copy(
                            nightTimeInRouteList = nightTimeState
                        )
                    }
                }
            }
        }
    }

    fun remove(route: Route) {
        removeRouteJob?.cancel()
        removeRouteJob = routeUseCase.markAsRemoved(route).onEach { result ->
            _uiState.update {
                it.copy(removeRouteState = result)
            }
        }.launchIn(viewModelScope)
    }

    fun resetRemoveRouteState() {
        _uiState.update {
            it.copy(removeRouteState = null)
        }
    }

    private fun calculationOfTotalTime(routes: List<Route>) {
        totalTime = 0
        routes.forEach { route ->
            if (isTransition(route.basicData.timeStartWork, route.basicData.timeEndWork)) {
                totalTime += getTimeInCurrentMonth(
                    route.basicData.timeStartWork!!,
                    route.basicData.timeEndWork!!,
                    uiState.value.monthSelected
                )
            } else {
                route.getWorkTime().let { time ->
                    totalTime += time ?: 0
                }
            }
        }
    }

    private fun isTransition(startTime: Long?, endTime: Long?): Boolean {
        if (startTime == null || endTime == null) {
            return false
        } else {
            val startCalendar = getInstance().also {
                it.timeInMillis = startTime
            }
            val yearStart = startCalendar.get(YEAR)
            val monthStart = startCalendar.get(MONTH)

            val endCalendar = getInstance().also {
                it.timeInMillis = endTime
            }
            val yearEnd = endCalendar.get(YEAR)
            val monthEnd = endCalendar.get(MONTH)
            return if (monthStart < monthEnd && yearStart == yearEnd) {
                true
            } else if (monthStart > monthEnd && yearStart < yearEnd) {
                true
            } else {
                false
            }
        }
    }

    private fun getTimeInCurrentMonth(
        startTime: Long,
        endTime: Long,
        monthOfYear: MonthOfYear
    ): Long {
        val startCalendar = getInstance().also {
            it.timeInMillis = startTime
        }

        if (startCalendar.get(MONTH) == monthOfYear.month) {
            val endCurrentDay = getInstance().also {
                it.timeInMillis = startTime
                it.set(DAY_OF_MONTH, it.get(DAY_OF_MONTH) + 1)
                it.set(HOUR_OF_DAY, 0)
                it.set(MINUTE, 0)
                it.set(MILLISECOND, 0)
            }
            val endCurrentDayInMillis = endCurrentDay.timeInMillis
            return endCurrentDayInMillis - startTime
        } else {
            val startCurrentDay = getInstance().also {
                it.timeInMillis = endTime
                it.set(HOUR_OF_DAY, 0)
                it.set(MINUTE, 0)
                it.set(MILLISECOND, 0)
            }
            val startCurrentDayInMillis = startCurrentDay.timeInMillis
            return endTime - startCurrentDayInMillis
        }
    }

    fun setCurrentMonth(yearAndMonth: Pair<Int, Int>) {
        setCalendarJob?.cancel()
        setCalendarJob = calendarUseCase.loadMonthOfYearList().onEach { result ->
            if (result is ResultState.Success) {
                result.data.find {
                    it.year == yearAndMonth.first && it.month == yearAndMonth.second
                }?.let { selectMonthOfYear ->
                    _uiState.update {
                        it.copy(monthSelected = selectMonthOfYear)
                    }
                }
            }
        }
            .launchIn(viewModelScope)
    }

    private fun loadMonthList() {
        loadCalendarJob?.cancel()
        loadCalendarJob = calendarUseCase.loadMonthOfYearList().onEach { result ->
            if (result is ResultState.Success) {
                _uiState.update { state ->
                    state.copy(
                        monthList = result.data.map { it.month }.distinct(),
                        yearList = result.data.map { it.year }.distinct()
                    )

                }
            }
        }
            .launchIn(viewModelScope)
    }

    private fun loadMinTimeRestInRoute() {
        CoroutineScope(Dispatchers.IO).launch {
            val minTimeRest = viewModelScope.async { dataStoreRepository.getMinTimeRest() }.await()
            minTimeRest.collect { time ->
                _uiState.update {
                    it.copy(minTimeRest = time)
                }
            }
        }
    }

    init {
        val calendar = getInstance()

        loadRoutes()
        loadMonthList()
        setCurrentMonth(
            Pair(
                calendar.get(YEAR),
                calendar.get(MONTH)
            )
        )
        loadMinTimeRestInRoute()
    }
}