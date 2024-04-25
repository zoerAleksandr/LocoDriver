package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavBackStackEntry
import com.z_company.domain.navigation.Router
import com.z_company.route.ui.DetailsScreen
import com.z_company.route.viewmodel.DetailsViewModel
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun DetailsDestination(
    router: Router,
    backStackEntry: NavBackStackEntry,
) {
    val routeId = DetailsRoute.getRouteId(backStackEntry)
    val routeDetailsViewModel = getViewModel<DetailsViewModel>(
        parameters = { parametersOf(routeId) }
    )
    val routeDetailsState by routeDetailsViewModel.routeDetailsState.collectAsState()
    val minTimeRestState by routeDetailsViewModel.minTimeRestState.collectAsState()

    DetailsScreen(
        routeDetailState = routeDetailsState,
        minTimeRest = minTimeRestState,
        onEditClick = {
            router.showRouteForm(it)
        },
        onBackPressed = {
            router.back()
        }
    )
}