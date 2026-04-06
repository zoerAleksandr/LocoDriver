package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.z_company.domain.navigation.Router
import com.z_company.route.ui.SalaryCalculationScreen
import com.z_company.route.viewmodel.SalaryCalculationViewModel
import org.koin.compose.koinInject

@Composable
fun SalaryCalculationDestination(router: Router){
    val viewModel: SalaryCalculationViewModel = koinInject()
    val uiState by viewModel.uiState.collectAsState()

    SalaryCalculationScreen(
        viewModel = viewModel,
        uiState = uiState,
        onSettingsSalaryClick = router::showSettingSalary,
    )
}