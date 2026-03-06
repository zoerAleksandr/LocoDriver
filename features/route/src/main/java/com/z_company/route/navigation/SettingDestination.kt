package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import com.z_company.domain.navigation.Router
import com.z_company.shared.ui.screen.SettingsScreen as SharedSettingsScreen

@Composable
fun SettingDestination(
    router: Router
) {
    val settingsViewModel: SettingsViewModel = viewModel()
    val uiState by settingsViewModel.uiState.collectAsState()
    SettingsScreen(
        viewModel = settingsViewModel,
        settingsUiState = uiState,
        currentSettings = settingsViewModel.currentSettings,
        onSettingSaved = { router.showHome(HomeRoute.route) },
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
        timeZoneRussiaList = settingsViewModel.timeZoneList,
        setTimeZone = settingsViewModel::setTimeZone,
        servicePhases = uiState.servicePhases,
        showDialogAddServicePhase = settingsViewModel::showDialogAddServicePhase,
        hideDialogAddServicePhase = settingsViewModel::hideDialogAddServicePhase,
        addServicePhase = settingsViewModel::addServicePhase,
        deleteServicePhase = settingsViewModel::deleteServicePhase,
        updateServicePhase = settingsViewModel::selectToUpdateServicePhase,
    )
}
