package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.z_company.domain.navigation.Router
import com.z_company.route.ui.AllRouteScreen
import com.z_company.route.viewmodel.all_route_view_model.AllRouteViewModel
import com.z_company.route.viewmodel.home_view_model.StartPurchasesEvent
import ru.rustore.sdk.core.feature.model.FeatureAvailabilityResult

@Composable
fun AllRouteScreenDestination(
    router: Router
) {
    val viewModel: AllRouteViewModel = viewModel()

    LaunchedEffect(Unit) {
        viewModel.openRouteFormEvent.collect { event ->
            router.showRouteForm(basicId = event.basicId, isMakeCopy = event.isMakeCopy)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.purchasesEvent.collect { event ->
            when (event) {
                is StartPurchasesEvent.PurchasesAvailability -> {
                    when (val avail = event.availability) {
                        is FeatureAvailabilityResult.Available -> {
                            // UI performs navigation
                            router.showPurchasesScreen()
                        }

                        is FeatureAvailabilityResult.Unavailable -> {
                            // ViewModel already showed snackbar; optionally handle here
                        }
                    }
                }

                is StartPurchasesEvent.Error -> {
                    // event.throwable - show fallback snackbar or handle
                    // you can also rely on ViewModel to show snackbar via snackbarManager
                }
            }
        }
    }

    AllRouteScreen(
        viewModel = viewModel,
        onRouteClick = { router.showRouteForm(it) },
        setSortOption = viewModel::setSort,
        showFormScreen = router::showRouteForm,
    )
}