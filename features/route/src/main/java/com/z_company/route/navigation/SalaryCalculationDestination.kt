package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.z_company.domain.navigation.Router
import com.z_company.route.ui.SalaryCalculationScreen
import com.z_company.route.viewmodel.SalaryCalculationViewModel


@Composable
fun SalaryCalculationDestination(router: Router){
    val viewModel: SalaryCalculationViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    SalaryCalculationScreen(
        onBack = router::back,
        uiState = uiState,
        onSettingsSalaryClick = router::showSettingSalary
    )
}