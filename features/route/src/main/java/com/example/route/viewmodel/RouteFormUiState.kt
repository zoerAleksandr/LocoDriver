package com.example.route.viewmodel

import com.example.core.ResultState
import com.example.domain.entities.route.Route

data class RouteFormUiState(
    val routeDetailState : ResultState<Route?> = ResultState.Loading,
    val saveRouteState : ResultState<Unit>? = null
)
