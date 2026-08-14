package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.z_company.domain.navigation.Router
import com.z_company.route.ui.AllRouteScreen
import com.z_company.route.viewmodel.all_route_view_model.AllRouteViewModel
import com.z_company.route.viewmodel.home_view_model.StartPurchasesEvent

@Composable
fun AllRouteScreenDestination(
    router: Router
) {
    val viewModel: AllRouteViewModel = viewModel()

    LaunchedEffect(Unit) {
        viewModel.syncOnScreenOpen()
    }
    DisposableEffect(viewModel) {
        onDispose(viewModel::stopScreenSync)
    }

    LaunchedEffect(Unit) {
        viewModel.openRouteFormEvent.collect { event ->
            router.showRouteForm(basicId = event.basicId, isMakeCopy = event.isMakeCopy)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.purchasesEvent.collect { event ->
            when (event) {
                is StartPurchasesEvent.ShowPurchasesScreen -> router.showPurchasesScreen()
                is StartPurchasesEvent.Error -> {}
            }
        }
    }

    AllRouteScreen(
        viewModel = viewModel,
        onRouteClick = { router.showRouteForm(it) },
        setSortOption = viewModel::setSort,
        showFormScreen = router::showRouteForm,
        showPurchasesScreen = router::showPurchasesScreen,
        onBack = router::back,
    )
}
