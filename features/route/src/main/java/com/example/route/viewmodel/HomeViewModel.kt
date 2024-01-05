package com.example.route.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ResultState
import com.example.domain.entities.MonthOfYear
import com.example.domain.entities.route.Route
import com.example.domain.entities.route.UtilsForEntities.getWorkTime
import com.example.domain.use_cases.CalendarUseCase
import com.example.domain.use_cases.RouteUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class HomeViewModel : ViewModel(), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    private val calendarUseCase: CalendarUseCase by inject()
    var totalTime by mutableLongStateOf(0L)
        private set

    var monthList = mutableStateListOf<Int>()
    var yearList = mutableStateListOf<Int>()

    private var loadRouteJob: Job? = null
    private var removeRouteJob: Job? = null
    private var loadCalendarJob: Job? = null
    private var loadMonthJob: Job? = null
    private var loadYearJob: Job? = null

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
            }
        }.launchIn(viewModelScope)
    }

    fun remove(route: Route) {
        removeRouteJob?.cancel()
        removeRouteJob = routeUseCase.removeRoute(route).onEach { result ->
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

    fun setMonthSelected(month: Int) {
        _uiState.update {
            it.copy(
                monthSelected = MonthOfYear(
                    month = month,
                    year = it.monthSelected.year
                )
            )
        }
    }

    fun setYearSelect(year: Int){
        _uiState.update {
            it.copy(
                monthSelected = MonthOfYear(
                    month = it.monthSelected.month,
                    year = year
                )
            )
        }
    }

    private fun loadCurrentMonthOfYear() {
        loadCalendarJob?.cancel()
        loadCalendarJob = calendarUseCase.getCurrentMonthOfYear().onEach { result ->
            if (result is ResultState.Success) {
                _uiState.update {
                    it.copy(monthSelected = result.data)
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun loadMonth() {
        loadMonthJob?.cancel()
        loadMonthJob = calendarUseCase.monthList().onEach { result ->
            if (result is ResultState.Success) {
                monthList.addAll(result.data)
            }
        }.launchIn(viewModelScope)
    }


    private fun loadYear() {
        loadYearJob?.cancel()
        loadYearJob = calendarUseCase.yearList().onEach { result ->
            if (result is ResultState.Success) {
                yearList.addAll(result.data)
            }
        }.launchIn(viewModelScope)
    }


    init {
        loadRoutes()
        loadCurrentMonthOfYear()
        loadMonth()
        loadYear()
    }
}