package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.z_company.domain.navigation.Router
import com.z_company.route.ui.SalaryCalculationScreen
import com.z_company.route.viewmodel.SalaryCalculationViewModel


@Composable
fun SalaryCalculationDestination(router: Router){
    val viewModel: SalaryCalculationViewModel = viewModel()

    SalaryCalculationScreen(
        onBack = router::back
    )
}