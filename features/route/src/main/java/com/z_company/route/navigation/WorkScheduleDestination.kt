package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import com.z_company.domain.navigation.Router
import com.z_company.shared.ui.screen.WorkScheduleScreen as SharedWorkScheduleScreen

@Composable
fun WorkScheduleDestination(
    router: Router
) {
    SharedWorkScheduleScreen(
        onBackClick = router::back,
    )
}
