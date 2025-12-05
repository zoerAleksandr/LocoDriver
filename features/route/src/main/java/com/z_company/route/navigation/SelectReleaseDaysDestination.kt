package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.z_company.domain.navigation.Router
import com.z_company.route.ui.SelectReleaseDaysScreen
import com.z_company.route.viewmodel.SelectReleaseDaysViewModel

@Composable
fun SelectReleaseDaysDestination(router: Router) {
    val viewModel : SelectReleaseDaysViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    SelectReleaseDaysScreen(
        onSaveClick = viewModel::saveNormaHours,
        monthOfYear = viewModel.currentMonthOfYear,
        releasePeriodListState = uiState.releaseDaysPeriodState,
        addingReleasePeriod = viewModel::addReleasePeriod,
        removingReleasePeriod = viewModel::deleteReleasePeriod,
        onReleaseDaysSaved = { router.showHome(HomeRoute.route) },
        saveReleaseDaysState = uiState.saveReleaseDaysState,
        yearList = uiState.yearList,
        monthList = uiState.monthList,
        selectMonthOfYear = viewModel::setCurrentMonth,
        dateAndTimeConverter = uiState.dateAndTimeConverter
    )
}
