package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import com.z_company.domain.navigation.Router
import com.z_company.route.ui.BrakeCertificateScreen
import com.z_company.route.viewmodel.BrakeCertificateViewModel

@Composable
fun BrakeCertificateDestination(
    router: Router,
    backStackEntry: NavBackStackEntry,
) {
    val viewModel: BrakeCertificateViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val weight = BrakeCertificateRoute.getWeight(backStackEntry)
    val axles = BrakeCertificateRoute.getAxles(backStackEntry)
    LaunchedEffect(weight, axles) { viewModel.prefill(weight, axles) }

    BrakeCertificateScreen(
        state = state,
        onBack = router::back,
        viewModel = viewModel,
    )
}
