package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import com.z_company.domain.navigation.Router
import com.z_company.shared.ui.screen.SalaryCalculationScreen as SharedSalaryCalculationScreen

@Composable
fun SalaryCalculationDestination(router: Router) {
    SharedSalaryCalculationScreen(
        onBackClick = router::back,
        onShowSettingSalary = router::showSettingSalary,
    )
}
