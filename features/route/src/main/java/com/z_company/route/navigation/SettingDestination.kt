package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import com.z_company.domain.navigation.Router
import com.z_company.route.ui.SettingsScreen
import com.z_company.route.viewmodel.PartnerListViewModel
import com.z_company.route.viewmodel.SeriesListViewModel
import com.z_company.route.viewmodel.SettingsViewModel
import com.z_company.route.viewmodel.PullToSyncViewModel
import com.z_company.route.viewmodel.StationNormListViewModel

@Composable
fun SettingDestination(
    router: Router,
    backStackEntry: NavBackStackEntry
) {
    val settingsViewModel: SettingsViewModel = viewModel()
    val pullToSyncViewModel: PullToSyncViewModel = viewModel()
    val pullToSyncState by pullToSyncViewModel.uiState.collectAsState()
    val uiState by settingsViewModel.uiState.collectAsState()
    val initialSubScreen = SettingsScreenRoute.getSubScreen(backStackEntry)

    val seriesListViewModel: SeriesListViewModel = viewModel()
    val stationListViewModel: StationNormListViewModel = viewModel()
    val partnerListViewModel: PartnerListViewModel = viewModel()

    SettingsScreen(
        viewModel = settingsViewModel,
        settingsUiState = uiState,
        currentSettings = settingsViewModel.currentSettings,
        initialSubScreen = initialSubScreen,
        workTimeChanged = settingsViewModel::changeDefaultWorkTime,
        restTimeChanged = settingsViewModel::changeMinTimeRest,
        homeRestTimeChanged = settingsViewModel::changeMinTimeHomeRest,
        showAbsenceScreen = router::showAbsence,
        logOut = router::showSignIn,
        resetUploadState = settingsViewModel::resetUploadState,
        resetDownloadState = settingsViewModel::resetDownloadState,
        changeStartNightTime = settingsViewModel::changeStartNightTime,
        changeEndNightTime = settingsViewModel::changeEndNightTime,
        changeUsingDefaultWorkTime = settingsViewModel::changeUsingDefaultWorkTime,
        changeConsiderFutureRoute = settingsViewModel::changeConsiderFutureRoute,
        changeShowBreak = settingsViewModel::changeShowBreak,
        changeShowOnePersonSwitch = settingsViewModel::changeShowOnePersonSwitch,
        changeShowLocomotive = settingsViewModel::changeShowLocomotive,
        changeShowTrain = settingsViewModel::changeShowTrain,
        changeShowPassenger = settingsViewModel::changeShowPassenger,
        changeShowOtherWork = settingsViewModel::changeShowOtherWork,
        changeShowPartner = settingsViewModel::changeShowPartner,
        changeShowLocoHeating = settingsViewModel::changeShowLocoHeating,
        changeShowLocoAuxiliary = settingsViewModel::changeShowLocoAuxiliary,
        changeShowOtherCurrent = settingsViewModel::changeShowOtherCurrent,
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
        seriesListViewModel = seriesListViewModel,
        stationListViewModel = stationListViewModel,
        partnerListViewModel = partnerListViewModel,
        isPullRefreshing = pullToSyncState.isRefreshing,
        onPullRefresh = { pullToSyncViewModel.refresh() },
        pullSyncMessage = pullToSyncState.message,
        onPullSyncMessageShown = pullToSyncViewModel::consumeMessage,
    )
}
