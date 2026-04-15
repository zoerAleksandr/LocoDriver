package com.z_company.route.viewmodel.home_view_model

import androidx.compose.runtime.Stable
import com.z_company.core.ResultState
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.entities.route.Route
import com.z_company.domain.util.TimeCalculationContext
import com.z_company.route.viewmodel.SyncStepState
import com.z_company.route.viewmodel.SyncType

data class HomeUiState(
    val uiState: ResultState<Unit> = ResultState.Loading(),
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
    val singleLocomotiveTimeState: ResultState<Long>? = ResultState.Loading(),
    val passengerTimeInRouteList: ResultState<Long>? = ResultState.Loading(),
    val extendedServicePhaseTime: ResultState<Long>? = ResultState.Loading(),
    val longDistanceTrainsTime: ResultState<Long>? = ResultState.Loading(),
    val heavyTrainsTime: ResultState<Long>? = ResultState.Loading(),
    val onePersonOperationTime: ResultState<Long>? = ResultState.Loading(),
    val dayOffHours: ResultState<Int>? = ResultState.Loading(),
    val holidayHours: ResultState<Long>? = ResultState.Loading(),
    val showNewRouteScreen: Boolean = false,
    val showPurchasesScreen: Boolean = false,
    val isLoadingStateAddButton: Boolean = false,
    val showConfirmRemoveRoute: Boolean = false,
    val offsetInMoscow: Long = 0L,
    val timeCalculationContext: TimeCalculationContext? = null,
    val listItemState: List<ItemState> = emptyList(),
    val dateAndTimeConverter: DateAndTimeConverter? = null,
    val showSnackbar: Boolean = false,
    val unsyncedRoutesCount: Int = 0,
    val showSyncDialog: Boolean = false,
    val isSyncComplete: Boolean = false,
    val isSyncSuccess: Boolean = false,
    val syncType: SyncType? = null,
    val syncUploadProgress: Map<String, SyncStepState> = emptyMap(),
    val syncRouteErrors: List<String> = emptyList(),
    val syncRoutesTotalAttempted: Int = 0,
    val syncRoutesSavedCount: Int = 0,
    val syncReportUserId: String? = null,
    val isNetworkError: Boolean = false
)

@Stable
data class ItemState(
    val route: Route,
    val isHoliday: Boolean = false,
    val isExtendedServicePhaseTrains: Boolean = false,
    val isHeavyTrains: Boolean = false,
    val isLongCompositionTrain: Boolean = false,
    val isTransition: Boolean = false,
    val isFuture: Boolean = false
)
