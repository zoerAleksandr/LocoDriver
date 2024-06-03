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
    val homeUiState by homeViewModel.uiState.collectAsState()
    HomeScreen(
        routeListState = homeUiState.routeListState,
        removeRouteState = homeUiState.removeRouteState,
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
        currentMonthOfYear = homeUiState.monthSelected,
        yearList = homeUiState.yearList,
        monthList = homeUiState.monthList,
        selectYearAndMonth = homeViewModel::setCurrentMonth,
        minTimeRest = homeUiState.minTimeRest,
        nightTime = homeUiState.nightTimeInRouteList,
        passengerTime = homeUiState.passengerTimeInRouteList
    )
}