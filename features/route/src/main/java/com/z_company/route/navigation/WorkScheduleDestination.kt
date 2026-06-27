package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.z_company.domain.navigation.Router
import com.z_company.route.ui.WorkScheduleScreen
import com.z_company.route.viewmodel.WorkScheduleViewModel
import com.z_company.route.viewmodel.home_view_model.StartPurchasesEvent

@Composable
fun WorkScheduleDestination(
    router: Router
) {
    val viewModel: WorkScheduleViewModel = viewModel()

    LaunchedEffect(Unit) {
        viewModel.purchasesEvent.collect { event ->
            when (event) {
                is StartPurchasesEvent.ShowPurchasesScreen -> router.showPurchasesScreen()
                is StartPurchasesEvent.Error -> {}
            }
        }
    }

    WorkScheduleScreen(
        viewModel = viewModel,
        onReleaseDayScreenClick = router::showSelectReleaseDayScreen,
        showPurchasesScreen = router::showPurchasesScreen,
        onBack = router::back,
    )
}
