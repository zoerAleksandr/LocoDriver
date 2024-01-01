package com.example.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.use_cases.RouteUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class HomeViewModel: ViewModel(), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()

    private var loadRouteJob: Job? = null

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private fun getListRoute(){
        loadRouteJob?.cancel()
        loadRouteJob = routeUseCase.listRoutes().onEach { result ->
            _uiState.update {
                it.copy(routeListState = result)
            }
        }.launchIn(viewModelScope)
    }

    init {
        getListRoute()
    }
}