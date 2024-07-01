package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.z_company.domain.navigation.Router
import com.z_company.route.ui.SearchScreen
import com.z_company.route.viewmodel.SearchViewModel

@Composable
fun SearchDestination(router: Router) {
    val viewModel : SearchViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    SearchScreen(
        setQueryValue = viewModel::setQueryValue,
        query = viewModel.query,
        onBack = router::back,
        sendRequest = viewModel::sendRequest,
        addResponse = viewModel::addResponse,
        clearFilter = viewModel::clearFilter,
        setSearchFilter = viewModel::setSearchFilter,
        setPeriodFilter = viewModel::setPeriodFilter,
        searchFilter = uiState.searchFilter,
        isVisibleHints = uiState.isVisibleHints,
        isVisibleHistory = uiState.isVisibleHistory,
        isVisibleResult = uiState.isVisibleResult,
        hints = uiState.hints,
        searchState = uiState.searchState,
        onRouteClick = router::showRouteDetails,
        searchHistoryList = uiState.searchHistoryList,
        removeHistoryResponse = viewModel::removeHistoryResponse,
        setPreliminarySearch = viewModel::setPreliminarySearch
    )
}
