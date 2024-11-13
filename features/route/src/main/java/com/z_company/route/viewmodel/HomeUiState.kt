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
    val nightTimeInRouteList: ResultState<Long>? = null,
    val passengerTimeInRouteList: ResultState<Long>? = null,
    val dayOffHours: ResultState<Int>? = null,
    val holidayHours: ResultState<Long>? = null,
    val showFirstEntryToAccountDialog: Boolean = false,
    val showNewRouteScreen: Boolean = false,
    val showPurchasesScreen: Boolean = false,
    val isLoadingStateAddButton: Boolean = false,
    val restoreSubscriptionState: ResultState<String>? = null,
    val showConfirmRemoveRoute: Boolean = false
)
