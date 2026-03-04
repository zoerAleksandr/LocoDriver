package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import com.z_company.domain.navigation.Router
import com.z_company.shared.ui.screen.MoreInfoScreen as SharedMoreInfoScreen

@Composable
fun MoreInfoDestination(
    router: Router,
    backStackEntry: NavBackStackEntry,
) {
    val monthId = MoreInfoRoute.getMonthOfYearId(backStackEntry)
    SharedMoreInfoScreen(
        monthId = monthId,
        onBackClick = router::back,
    )
}
