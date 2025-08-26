package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.z_company.domain.navigation.Router
import com.z_company.route.ui.AllRouteScreen
import com.z_company.route.viewmodel.all_route_view_model.AllRouteViewModel

@Composable
fun AllRouteScreenDestination(
    router: Router
) {
    val viewModel: AllRouteViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    AllRouteScreen(
        listRouteState = uiState.listItemState,
        makeCopyRoute = {},
        onDeleteRoute = {},
        onRouteClick = {
            router.showRouteForm(it)
        },
//        onNewRouteClick = viewModel::newRouteClick,
        getTextWorkTime = {""}
    )
}