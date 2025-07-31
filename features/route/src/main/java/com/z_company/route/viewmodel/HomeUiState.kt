package com.z_company.route.viewmodel

import com.z_company.core.ResultState
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.entities.route.Route
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

data class HomeUiState(
    val routeListState: ResultState<List<Route>> = ResultState.Loading(),
    val settingState: ResultState<UserSettings?> = ResultState.Loading(),
    val removeRouteState: ResultState<Unit>? = null,
    val monthSelected: ResultState<MonthOfYear?> = ResultState.Loading(),
    val monthList: List<Int> = listOf(),
    val yearList: List<Int> = listOf(),
    val minTimeRest: Long? = null,
    val minTimeHomeRest: Long? = null,
    val totalTimeWithHoliday: ResultState<Long>? = ResultState.Loading(),
    val nightTimeInRouteList: ResultState<Long>? = ResultState.Loading(),
    val passengerTimeInRouteList: ResultState<Long>? = ResultState.Loading(),
    val dayOffHours: ResultState<Int>? = ResultState.Loading(),
    val holidayHours: ResultState<Long>? = ResultState.Loading(),
    val showFirstEntryToAccountDialog: Boolean = false,
    val showNewRouteScreen: Boolean = false,
    val showPurchasesScreen: Boolean = false,
    val isLoadingStateAddButton: Boolean = false,
    val restoreSubscriptionState: ResultState<String>? = null,
    val showConfirmRemoveRoute: Boolean = false,
    val offsetInMoscow: Long = 0L,
    val syncRouteState: ResultState<String>? = null,
    val listItemState: MutableList<ItemState> = mutableListOf<ItemState>(),
    var dateAndTimeConverter: DateAndTimeConverter? = null
)

data class ItemState(
    val route: Route,
    val isHoliday: Boolean = false,
    val isExtendedServicePhaseTrains: Boolean = false,
    val isHeavyTrains: Boolean = false,
)
