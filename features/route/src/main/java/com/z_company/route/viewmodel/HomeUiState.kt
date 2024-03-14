package com.z_company.route.viewmodel

import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.route.Route

data class HomeUiState(
    val routeListState: ResultState<List<Route>> = ResultState.Loading,
    val removeRouteState: ResultState<Unit>? = null,
    val monthSelected: MonthOfYear = MonthOfYear(),
    val monthList: List<Int> = listOf(),
    val yearList: List<Int> = listOf()
)
