package com.example.route.viewmodel

import com.example.core.ResultState
import com.example.domain.entities.route.Route

data class RouteFormUiState(
    val routeDetailState: ResultState<Route?> = ResultState.Loading,
    val saveRouteState: ResultState<Unit>? = null,
    var exitFromScreen: Boolean = false,
    val changesHaveState: Boolean = false,
    var confirmExitDialogShow: Boolean = false,
    val errorMessage: String? = null,
    val minTimeRest: Long? = null,
    val fullTimeRest: Long? = null
)
