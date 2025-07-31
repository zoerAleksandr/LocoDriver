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
) {
    val settingsViewModel: SettingsViewModel = viewModel()
    val uiState by settingsViewModel.uiState.collectAsState()
    SettingsScreen(
        settingsUiState = uiState,
        currentSettings = settingsViewModel.currentSettings,
        currentUserState = uiState.userDetailsState,
        resetSaveState = settingsViewModel::resetSaveState,
        onSaveClick = settingsViewModel::saveSettings,
        onBack = router::back,
        onSettingSaved = router::back,
        onLogOut = settingsViewModel::logOut,
        onDownloadFromRemote = settingsViewModel::onDownloadFromRemote,
        onUploadToRemote = settingsViewModel::onUploadToServer,
        workTimeChanged = settingsViewModel::changeDefaultWorkTime,
        locoTypeChanged = settingsViewModel::changeDefaultLocoType,
        restTimeChanged = settingsViewModel::changeMinTimeRest,
        homeRestTimeChanged = settingsViewModel::changeMinTimeHomeRest,
        showReleaseDaySelectScreen = router::showSelectReleaseDayScreen,
        logOut = router::showSignIn,
        onResentVerificationEmail = settingsViewModel::emailConfirmation,
        emailForConfirm = settingsViewModel.currentEmail,
        onChangeEmail = settingsViewModel::setEmail,
        enableButtonConfirmVerification = uiState.resentVerificationEmailButton,
        resetUploadState = settingsViewModel::resetUploadState,
        resetDownloadState = settingsViewModel::resetDownloadState,
        changeStartNightTime = settingsViewModel::changeStartNightTime,
        changeEndNightTime = settingsViewModel::changeEndNightTime,
        changeUsingDefaultWorkTime = settingsViewModel::changeUsingDefaultWorkTime,
        changeConsiderFutureRoute = settingsViewModel::changeConsiderFutureRoute,
        purchasesState = uiState.purchasesEndTime,
        onBillingClick = router::showPurchasesScreen,
        isRefreshing = uiState.isRefreshing,
        onRefresh = settingsViewModel::refreshingUserData,
        onSettingHomeScreenClick = router::showSettingHomeScreen,
        timeZoneRussiaList = settingsViewModel.timeZoneList,
        setTimeZone = settingsViewModel::setTimeZone,
        servicePhases = uiState.servicePhases,
        showDialogAddServicePhase = settingsViewModel::showDialogAddServicePhase,
        hideDialogAddServicePhase = settingsViewModel::hideDialogAddServicePhase,
        addServicePhase = settingsViewModel::addServicePhase,
        deleteServicePhase = settingsViewModel::deleteServicePhase,
        updateServicePhase = settingsViewModel::selectToUpdateServicePhase,
        setInputDateTimeType = settingsViewModel::setInputDateTimeType,
        inputDateTimeType = uiState.inputDateTimeType,
        getAllRouteRemote = settingsViewModel::getAllRouteRemote,
        dateAndTimeConverter = settingsViewModel.dateAndTimeConverter
    )
}