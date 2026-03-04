package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import com.z_company.domain.navigation.Router
import com.z_company.route.Const.NULLABLE_ID
import com.z_company.shared.ui.screen.FormLocoScreen as SharedFormLocoScreen

@Composable
fun FormLocoDestination(
    router: Router,
    backStackEntry: NavBackStackEntry
) {
    val locoId = FormLoco.getLocoId(backStackEntry)?.takeIf { it != NULLABLE_ID }
    val basicId = FormLoco.getBasicId(backStackEntry) ?: ""

    SharedFormLocoScreen(
        locoId = locoId,
        basicId = basicId,
        onBackClick = router::back,
    )
}
