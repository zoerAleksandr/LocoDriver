package com.example.route.viewmodel

import com.example.core.ResultState
import com.example.domain.entities.route.BasicData
import com.example.domain.entities.route.Route

data class HomeUiState(
    val routeListState: ResultState<List<Route>> = ResultState.Loading,
    val removeRouteState: ResultState<Unit>? = null
)
