package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import com.z_company.domain.navigation.Router
import com.z_company.route.ui.BrakeHoldingScreen
import com.z_company.route.viewmodel.BrakeHoldingViewModel

@Composable
fun BrakeHoldingDestination(
    router: Router,
    backStackEntry: NavBackStackEntry,
) {
    val viewModel: BrakeHoldingViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val prefill = BrakeHoldingRoute.getPrefill(backStackEntry)
    LaunchedEffect(prefill) { viewModel.prefill(prefill) }

    BrakeHoldingScreen(
        state = state,
        onBack = router::back,
        viewModel = viewModel,
    )
}
