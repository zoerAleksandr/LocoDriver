package com.z_company.route.viewmodel

import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Route

data class RouteFormUiState(
    val routeDetailState: ResultState<Route?> = ResultState.Loading,
    val saveRouteState: ResultState<Unit>? = null,
    var exitFromScreen: Boolean = false,
    val changesHaveState: Boolean = false,
    var confirmExitDialogShow: Boolean = false,
    val errorMessage: String? = null,
    val nightTime: Long? = null,
    val passengerTime: Long? = null,
    val isCopy: Boolean = false
)
