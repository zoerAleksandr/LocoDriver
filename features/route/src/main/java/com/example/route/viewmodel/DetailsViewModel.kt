package com.example.route.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ResultState
import com.example.domain.entities.route.Route
import com.example.domain.use_cases.RouteUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DetailsViewModel constructor(
    private val routeId: String
) : ViewModel(), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()

    private val _routeDetailsState = MutableStateFlow<ResultState<Route?>>(ResultState.Loading)
    val routeDetailsState = _routeDetailsState.asStateFlow()

    val minTimeRestState = MutableStateFlow(0L)

    private var getRouteJob: Job? = null

    private fun getRouteById(id: String) {
        val currentState = _routeDetailsState.value
        if (currentState is ResultState.Success && routeId == currentState.data?.basicData?.id) return

        getRouteJob?.cancel()
        getRouteJob = routeUseCase.routeDetails(id).onEach { response ->
            _routeDetailsState.value = response
        }.launchIn(viewModelScope)
    }

    init {
        Log.d("ZZZ", "routeId = $routeId")
        getRouteById(routeId)
    }

}
