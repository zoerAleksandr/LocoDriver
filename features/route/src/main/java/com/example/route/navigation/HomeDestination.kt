package com.example.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.navigation.Router
import com.example.route.ui.HomeScreen
import com.example.route.viewmodel.HomeViewModel

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
    )
}