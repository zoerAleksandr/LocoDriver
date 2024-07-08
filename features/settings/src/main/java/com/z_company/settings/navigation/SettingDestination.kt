package com.z_company.settings.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.z_company.domain.navigation.Router
import com.z_company.settings.ui.SettingsScreen
import com.z_company.settings.viewmodel.SettingsViewModel

@Composable
fun SettingDestination(
    router: Router
){
    val settingsViewModel: SettingsViewModel = viewModel()
    val uiState by settingsViewModel.uiState.collectAsState()
    SettingsScreen(
        settingsUiState = uiState,
        currentSettings = settingsViewModel.currentSettings,
        currentUser = settingsViewModel.currentUser,
        resetSaveState = settingsViewModel::resetSaveState,
        onSaveClick = settingsViewModel::saveSettings,
        onBack = router::back,
        onSettingSaved = router::back,
        onLogOut = settingsViewModel::logOut,
        onSync = settingsViewModel::onSync,
        workTimeChanged = settingsViewModel::changeDefaultWorkTime,
        locoTypeChanged = settingsViewModel::changeDefaultLocoType,
        restTimeChanged = settingsViewModel::changeMinTimeRest,
        homeRestTimeChanged = settingsViewModel::changeMinTimeHomeRest,
        showReleaseDaySelectScreen = router::showSelectReleaseDayScreen,
        yearList = uiState.yearList,
        monthList = uiState.monthList,
        selectMonthOfYear = settingsViewModel::setCurrentMonth,
        logOut = router::showSignIn
    )
}