package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.z_company.domain.navigation.Router
import com.z_company.route.ui.WorkScheduleScreen
import com.z_company.route.viewmodel.WorkScheduleViewModel

@Composable
fun WorkScheduleDestination(
    router: Router
){
    val viewModel : WorkScheduleViewModel = viewModel()
    WorkScheduleScreen(
        viewModel = viewModel,
        onReleaseDayScreenClick = router::showSelectReleaseDayScreen
    )
}