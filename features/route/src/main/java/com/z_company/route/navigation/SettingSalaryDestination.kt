package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import com.z_company.domain.navigation.Router
import com.z_company.shared.ui.screen.SettingSalaryScreen as SharedSettingSalaryScreen

@Composable
fun SettingSalaryDestination(
    router: Router
) {
    SharedSettingSalaryScreen(
        onBackClick = { router.showHome(HomeRoute.route) },
    )
}
