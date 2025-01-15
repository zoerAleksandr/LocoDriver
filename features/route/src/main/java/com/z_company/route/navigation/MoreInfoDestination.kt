package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import com.z_company.domain.navigation.Router
import com.z_company.route.ui.MoreInfoScreen
import com.z_company.route.viewmodel.MoreInfoViewModel

@Composable
fun MoreInfoDestination(
    router: Router,
    backStackEntry: NavBackStackEntry
) {
    val viewModel: MoreInfoViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    MoreInfoScreen(
        onBack = router::back,
        currentMonthOfYearState = uiState.currentMonthOfYearState,
        totalWorkTimeState = uiState.totalWorkTimeState,
        nightTimeState = uiState.nightTimeState,
        passengerTimeState = uiState.passengerTimeState,
        onePersonTimeState = uiState.onePersonTimeState,
        holidayWorkTimeState = uiState.holidayWorkTimeState,
        workTimeWithHoliday = uiState.workTimeWithHoliday,
        todayNormaHours = uiState.todayNormaHours,
        timeBalanceState = uiState.timeBalanceState,
        onSalaryCalculationClick = router::showSalaryCalculation
    )
}