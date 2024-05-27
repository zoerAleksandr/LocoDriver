package com.z_company.route.viewmodel

import android.util.Log
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
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.util.plus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar

class HomeViewModel : ViewModel(), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    private val calendarUseCase: CalendarUseCase by inject()
    private val dataStoreRepository: DataStoreRepository by inject()
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
        loadRouteJob = routeUseCase.listRoutes().onEach { result ->
            _uiState.update {
                it.copy(routeListState = result)
            }
            if (result is ResultState.Success) {
                calculationOfTotalTime(result.data)
                calculationOfNightTime(result.data)
            }
        }.launchIn(viewModelScope)
    }

    private suspend fun alternativeCalculateNightTime(){
        val startNightHour = dataStoreRepository.getStartNightHour().take(1)
    }

    private suspend fun calculationOfNightTime(routes: List<Route>) {
        var nightTimeState by mutableStateOf<Long?>(0L)
        var startNightHour by mutableIntStateOf(0)
        var startNightMinute by mutableIntStateOf(0)
        var endNightHour by mutableIntStateOf(0)
        var endNightMinute by mutableIntStateOf(0)

        Log.d("ZZZ", "before startNightHour = $startNightHour")
        viewModelScope.launch {
            startNightHour = dataStoreRepository.getStartNightHour().take(1).first()

            startNightMinute = dataStoreRepository.getStartNightMinute().take(1).first()

            endNightHour = dataStoreRepository.getEndNightHour().take(1).first()

            endNightMinute = dataStoreRepository.getEndNightMinute().take(1).first()

        }.join()
        Log.d("ZZZ", "after startNightHour = $startNightHour")

        routes.forEach { route ->
            val nightTimeInRoute = CalculateNightTime.getNightTime(
                startMillis = route.basicData.timeStartWork,
                endMillis = route.basicData.timeEndWork,
                hourStart = startNightHour,
                minuteStart = startNightMinute,
                hourEnd = endNightHour,
                minuteEnd = endNightMinute
            )
            nightTimeState = nightTimeState.plus(nightTimeInRoute)
        }
        _uiState.update {
            it.copy(
                nightTimeInRouteList = nightTimeState
            )
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
            route.getWorkTime().let { time ->
                totalTime += time ?: 0
            }
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
        val calendar = Calendar.getInstance()

        loadRoutes()
        loadMonthList()
        setCurrentMonth(
            Pair(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH)
            )
        )
        loadMinTimeRestInRoute()
    }
}