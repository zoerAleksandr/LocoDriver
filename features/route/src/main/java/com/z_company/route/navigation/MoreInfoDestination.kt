package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavBackStackEntry
import com.z_company.domain.navigation.Router
import com.z_company.route.Const
import com.z_company.route.ui.MoreInfoScreen
import com.z_company.route.viewmodel.MoreInfoViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun MoreInfoDestination(
    router: Router,
    backStackEntry: NavBackStackEntry
) {
    val monthOfYearId = MoreInfoRoute.getMonthOfYearId(backStackEntry) ?: Const.NULLABLE_ID
    val viewModel = koinViewModel<MoreInfoViewModel>(
        parameters = { parametersOf(monthOfYearId) }
    )
    val uiState by viewModel.uiState.collectAsState()

    MoreInfoScreen(
        onBack = router::back,
        currentMonthOfYearState = uiState.currentMonthOfYearState,
        totalWorkTimeState = uiState.totalWorkTimeState,
        nightTimeState = uiState.nightTimeState,
        passengerTimeState = uiState.passengerTimeState
    )
}