package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import com.z_company.domain.navigation.Router
import com.z_company.shared.ui.screen.SettingsScreen as SharedSettingsScreen

@Composable
fun SettingDestination(
    router: Router
) {
    SharedSettingsScreen(
        onBackClick = { router.showHome(HomeRoute.route) },
        onShowSettingSalary = router::showSettingSalary,
    )
}
