package com.example.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ResultState
import com.example.domain.entities.route.Route
import com.example.domain.use_cases.RouteUseCase
import com.example.route.Const.NULLABLE_ID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FormViewModel constructor(private val routeId: String?) : ViewModel(), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    private val _uiState = MutableStateFlow(RouteFormUiState())
    val uiState = _uiState.asStateFlow()

    private var loadRouteJob: Job? = null
    private var saveRouteJob: Job? = null

    var currentRoute : Route?
        get() {
            return _uiState.value.routeDetailState.let {
                if (it is ResultState.Success) it.data else null
            }
        }
        private set(value) {
            _uiState.update {
                it.copy(routeDetailState = ResultState.Success(value))
            }
        }

    init {
        if (routeId == NULLABLE_ID) {
            currentRoute = Route()
        } else {
            loadRoute(routeId!!)
        }
    }

    fun resetSaveState() {
        _uiState.update {
            it.copy(saveRouteState = null)
        }
    }
    private fun loadRoute(id: String) {
        if (routeId == currentRoute?.basicData?.id) return
        loadRouteJob?.cancel()
        loadRouteJob = routeUseCase.routeDetails(id).onEach { routeState ->
            _uiState.update {
                if (routeState is ResultState.Success){
                    currentRoute = routeState.data
                }
                it.copy(routeDetailState = routeState)
            }
        }.launchIn(viewModelScope)
    }

    fun saveRoute(){
        val state = _uiState.value.routeDetailState
        if (state is ResultState.Success){
            state.data?.let { route ->
                saveRouteJob?.cancel()
                saveRouteJob = routeUseCase.saveRoute(route).onEach { saveRouteState ->
                    _uiState.update {
                        it.copy(saveRouteState = saveRouteState)
                    }
                }.launchIn(viewModelScope)
            }
        }
    }

    fun setNumber(value: String){
//        currentRoute = currentRoute?.copy(number = value)
    }
}