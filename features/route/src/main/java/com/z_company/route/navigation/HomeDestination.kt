package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.z_company.domain.navigation.Router
import com.z_company.route.ui.HomeScreen
import com.z_company.route.viewmodel.HomeViewModel

@Composable
fun HomeDestination(
    router: Router
) {
    val homeViewModel: HomeViewModel = viewModel()
    val uiState by homeViewModel.uiState.collectAsState()
    val previewRouteUiState by homeViewModel.previewRouteUiState.collectAsState()

    HomeScreen(
        routeListState = uiState.routeListState,
        removeRouteState = uiState.removeRouteState,
        onRouteClick = {
            router.showRouteForm(it)
        },
        onMoreInfoClick = { router.showMoreInfo(it) },
        onNewRouteClick = homeViewModel::newRouteClick,
        onDeleteRoute = homeViewModel::remove,
        onDeleteRouteConfirmed = homeViewModel::resetRemoveRouteState,
        reloadRoute = homeViewModel::loadRoutes,
        onSettingsClick = { router.showSettings() },
        onSearchClick = { router.showSearch() },
        totalTime = homeViewModel.totalTime,
        currentMonthOfYear = homeViewModel.currentMonthOfYear,
        yearList = uiState.yearList,
        monthList = uiState.monthList,
        selectYearAndMonth = homeViewModel::setCurrentMonth,
        minTimeRest = uiState.minTimeRest,
        nightTime = uiState.nightTimeInRouteList,
        passengerTime = uiState.passengerTimeInRouteList,
        dayOffHours = uiState.dayOffHours,
        calculationHomeRest = homeViewModel::calculationHomeRest,
        homeRestValue = previewRouteUiState.homeRestState,
        firstEntryDialogState = uiState.showFirstEntryToAccountDialog,
        resetStateFirstEntryDialog = homeViewModel::disableFirstEntryToAccountDialog,
        purchasesEvent = homeViewModel.checkPurchasesEvent,
        showPurchasesScreen = router::showPurchasesScreen,
        isShowFormScreen = uiState.showNewRouteScreen,
        showFormScreen = { router.showRouteForm() },
        isLoadingStateAddButton = uiState.isLoadingStateAddButton,
        showFormScreenReset = homeViewModel::showFormScreenReset,
        alertBeforePurchasesState = homeViewModel.alertBeforePurchasesEvent,
        checkPurchasesAvailability = homeViewModel::checkPurchasesAvailability,
        restorePurchases = homeViewModel::restorePurchases,
        restoreResultState = uiState.restoreSubscriptionState,
        resetSubscriptionState = homeViewModel::resetSubscriptionState
    )
}