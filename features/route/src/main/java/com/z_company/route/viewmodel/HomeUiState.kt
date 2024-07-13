package com.z_company.route.viewmodel

import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.entities.route.Route

data class HomeUiState(
    val routeListState: ResultState<List<Route>> = ResultState.Loading,
    val settingState: ResultState<UserSettings?> = ResultState.Loading,
    val removeRouteState: ResultState<Unit>? = null,
    val monthSelected: ResultState<MonthOfYear?> = ResultState.Loading,
    val monthList: List<Int> = listOf(),
    val yearList: List<Int> = listOf(),
    val minTimeRest: Long? = null,
    val minTimeHomeRest: Long? = null,
    val nightTimeInRouteList: Long? = null,
    val passengerTimeInRouteList: Long? = null,
    val showFirstEntryToAccountDialog: Boolean = false
)
