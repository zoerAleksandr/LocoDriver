package com.example.route.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.navigation.Router
import com.example.route.ui.HomeScreen
import com.example.route.viewmodel.HomeViewModel

@Composable
fun HomeDestination(
    router: Router
) {
    val homeViewModel: HomeViewModel = viewModel()
    HomeScreen(
        onRouteClick = { router.showRouteDetails(it) },
        addingRoute = { router.showRouteForm() }
    )
}