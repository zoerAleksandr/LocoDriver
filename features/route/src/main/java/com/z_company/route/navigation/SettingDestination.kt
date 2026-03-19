package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import com.z_company.domain.navigation.Router
import com.z_company.route.ui.SettingsScreen
import com.z_company.route.viewmodel.SettingsViewModel

@Composable
fun SettingDestination(
    router: Router,
    backStackEntry: NavBackStackEntry
) {
    val settingsViewModel: SettingsViewModel = viewModel()
    val uiState by settingsViewModel.uiState.collectAsState()
    val initialSubScreen = SettingsScreenRoute.getSubScreen(backStackEntry)
    SettingsScreen(
        viewModel = settingsViewModel,
        settingsUiState = uiState,
        currentSettings = settingsViewModel.currentSettings,
        initialSubScreen = initialSubScreen,
        workTimeChanged = settingsViewModel::changeDefaultWorkTime,
        restTimeChanged = settingsViewModel::changeMinTimeRest,
        homeRestTimeChanged = settingsViewModel::changeMinTimeHomeRest,
        showReleaseDaySelectScreen = router::showSelectReleaseDayScreen,
        logOut = router::showSignIn,
        resetUploadState = settingsViewModel::resetUploadState,
        resetDownloadState = settingsViewModel::resetDownloadState,
        changeStartNightTime = settingsViewModel::changeStartNightTime,
        changeEndNightTime = settingsViewModel::changeEndNightTime,
        changeUsingDefaultWorkTime = settingsViewModel::changeUsingDefaultWorkTime,
        changeConsiderFutureRoute = settingsViewModel::changeConsiderFutureRoute,
        changeShowBreak = settingsViewModel::changeShowBreak,
        changeShowLocoHeating = settingsViewModel::changeShowLocoHeating,
        changeShowLocoAuxiliary = settingsViewModel::changeShowLocoAuxiliary,
        changeShowLocoStatistics = settingsViewModel::changeShowLocoStatistics,
        timeZoneRussiaList = settingsViewModel.timeZoneList,
        setTimeZone = settingsViewModel::setTimeZone,
        servicePhases = uiState.servicePhases,
        showDialogAddServicePhase = settingsViewModel::showDialogAddServicePhase,
        hideDialogAddServicePhase = settingsViewModel::hideDialogAddServicePhase,
        addServicePhase = settingsViewModel::addServicePhase,
        deleteServicePhase = settingsViewModel::deleteServicePhase,
        updateServicePhase = settingsViewModel::selectToUpdateServicePhase,
        showSettingSalary = router::showSettingSalary,
        onBack = router::back,
    )
}
