package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.z_company.domain.navigation.Router
import com.z_company.route.ui.WorkScheduleScreen
import com.z_company.route.viewmodel.WorkScheduleViewModel
import com.z_company.route.viewmodel.home_view_model.StartPurchasesEvent
import ru.rustore.sdk.pay.model.PurchaseAvailabilityResult

@Composable
fun WorkScheduleDestination(
    router: Router
){
    val viewModel : WorkScheduleViewModel = viewModel()

    LaunchedEffect(Unit) {
        viewModel.purchasesEvent.collect { event ->
            when (event) {
                is StartPurchasesEvent.PurchasesAvailability -> {
                    when (val avail = event.availability) {
                        is  PurchaseAvailabilityResult.Available -> {
                            // UI performs navigation
                            router.showPurchasesScreen()
                        }

                        is  PurchaseAvailabilityResult.Unavailable -> {
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

    WorkScheduleScreen(
        viewModel = viewModel,
        onReleaseDayScreenClick = router::showSelectReleaseDayScreen,
        showPurchasesScreen = router::showPurchasesScreen
    )
}