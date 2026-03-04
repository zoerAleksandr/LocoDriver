package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import com.z_company.domain.navigation.Router
import com.z_company.shared.ui.screen.SelectReleaseDaysScreen as SharedSelectReleaseDaysScreen

@Composable
fun SelectReleaseDaysDestination(router: Router) {
    SharedSelectReleaseDaysScreen(
        onBackClick = { router.showHome(HomeRoute.route) },
    )
}
