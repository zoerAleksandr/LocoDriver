package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    HomeScreen(
        routeListState = uiState.routeListState,
        removeRouteState = uiState.removeRouteState,
        onRouteClick = router::showRouteDetails,
        onNewRouteClick = { router.showRouteForm() },
        onChangeRoute = {
            router.showRouteForm(it)
        },
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
        calculationHomeRest = homeViewModel::calculationHomeRest,
        firstEntryDialogState = uiState.showFirstEntryToAccountDialog,
        resetStateFirstEntryDialog = homeViewModel::disableFirstEntryToAccountDialog
    )
}