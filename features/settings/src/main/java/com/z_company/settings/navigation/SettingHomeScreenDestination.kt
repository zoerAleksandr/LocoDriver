package com.z_company.settings.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.z_company.domain.navigation.Router
import com.z_company.settings.ui.SettingHomeScreen
import com.z_company.settings.viewmodel.SettingHomeScreenViewModel

@Composable
fun SettingHomeScreenDestination(router: Router) {

    val viewModel: SettingHomeScreenViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    SettingHomeScreen(
        uiState = uiState,
        onBack = router::back,
        onSaveClick = viewModel::saveSetting,
        onSettingSaved = router::back,
        resetSaveState = viewModel::resetSaveState,
        changeIsVisibleNightTime = viewModel::changeIsVisibleNightTime,
        changeIsVisiblePassengerTime = viewModel::changeIsVisiblePassengerTime,
        changeIsVisibleRelationTime = viewModel::changeIsVisibleRelationTime,
        changeIsVisibleHolidayTime = viewModel::changeIsVisibleHolidayTime,
        changeIsVisibleExtendedServicePhase = viewModel::changeIsVisibleExtendedServicePhase
    )
}