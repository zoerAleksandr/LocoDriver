package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import com.z_company.domain.navigation.Router
import com.z_company.route.Const.NULLABLE_ID
import com.z_company.shared.ui.screen.FormPassengerScreen as SharedFormPassengerScreen

@Composable
fun FormPassengerDestination(
    router: Router,
    backStackEntry: NavBackStackEntry
) {
    val passengerId = FormPassenger.getPassengerId(backStackEntry)?.takeIf { it != NULLABLE_ID }
    val basicId = FormPassenger.getBasicId(backStackEntry) ?: ""

    SharedFormPassengerScreen(
        passengerId = passengerId,
        basicId = basicId,
        onBackClick = router::back,
    )
}
