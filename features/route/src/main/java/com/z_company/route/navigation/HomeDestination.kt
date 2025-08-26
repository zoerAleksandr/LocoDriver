package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.z_company.domain.navigation.Router
import com.z_company.route.ui.HomeScreen
import com.z_company.route.viewmodel.home_view_model.HomeViewModel

@Composable
fun HomeDestination(
    router: Router
) {
    val homeViewModel: HomeViewModel = viewModel()
    val uiState by homeViewModel.uiState.collectAsState()
    val previewRouteUiState by homeViewModel.previewRouteUiState.collectAsState()
    HomeScreen(
        listRouteState = uiState.listItemState,
        routeListState = uiState.routeListState,
        removeRouteState = uiState.removeRouteState,
        onRouteClick = {
            router.showRouteForm(it)
        },
        makeCopyRoute = {router.showRouteForm(basicId = it, isMakeCopy = true)},
        onMoreInfoClick = { router.showMoreInfo(it) },
        onNewRouteClick = homeViewModel::newRouteClick,
        onDeleteRoute = homeViewModel::removeRoute,
        onDeleteRouteConfirmed = homeViewModel::resetRemoveRouteState,
//        reloadRoute = homeViewModel::loadSetting,
        onSettingsClick = { router.showSettings() },
        onSearchClick = { router.showSearch() },
        totalTime = homeViewModel.timeWithoutHoliday,
        currentMonthOfYear = homeViewModel.currentMonthOfYear,
        yearList = uiState.yearList,
        monthList = uiState.monthList,
        selectYearAndMonth = homeViewModel::setCurrentMonth,
        minTimeRest = uiState.minTimeRest,
        nightTimeState = uiState.nightTimeInRouteList,
        totalTimeWithHoliday = uiState.totalTimeWithHoliday,
        passengerTimeState = uiState.passengerTimeInRouteList,
        singleLocomotiveTimeState = uiState.singleLocomotiveTimeState,
        dayoffHours = uiState.dayOffHours,
        holidayHours = uiState.holidayHours,
        calculationHomeRest = homeViewModel::calculationHomeRest,
        homeRestValue = previewRouteUiState.homeRestState,
        firstEntryDialogState = uiState.showFirstEntryToAccountDialog,
        resetStateFirstEntryDialog = homeViewModel::disableFirstEntryToAccountDialog,
        purchasesEvent = homeViewModel.checkPurchasesEvent,
        showPurchasesScreen = router::showPurchasesScreen,
        isShowFormScreen = uiState.showNewRouteScreen,
        showFormScreen = { router.showRouteForm() },
        isLoadingStateAddButton = uiState.isLoadingStateAddButton,
        showFormScreenReset = homeViewModel::showFormScreenReset,
        alertBeforePurchasesState = homeViewModel.alertBeforePurchasesEvent,
        checkPurchasesAvailability = homeViewModel::checkPurchasesAvailability,
        restorePurchases = homeViewModel::restorePurchases,
        restoreResultState = uiState.restoreSubscriptionState,
        resetSubscriptionState = homeViewModel::resetSubscriptionState,
        showConfirmDialogRemoveRoute = uiState.showConfirmRemoveRoute,
        changeShowConfirmExitDialog = homeViewModel::isShowConfirmRemoveRoute,
        offsetInMoscow = uiState.offsetInMoscow,
        syncRouteState = uiState.syncRouteState,
        resetSyncRouteState = homeViewModel::resetSyncRouteState,
        syncRoute = homeViewModel::syncRoute,
        completeUpdateRequested = homeViewModel::completeUpdateRequested,
        updateEvent = homeViewModel.updateEvents,
        setFavoriteState = homeViewModel::setFavoriteRoute,
        getSharedIntent = homeViewModel::getUriToRoute,
        getTextWorkTime = homeViewModel::getTextWorkTime,
        dateAndTimeConverter = uiState.dateAndTimeConverter,
        extendedServicePhaseTime = uiState.extendedServicePhaseTime,
        longDistanceTrainsTime = uiState.longDistanceTrainsTime,
        heavyTrainsTime = uiState.heavyTrainsTime,
        onePersonOperationTime = uiState.onePersonOperationTime,
        currentRoute = homeViewModel.currentRoute,
        currentRouteTimeWork = homeViewModel.workTimeInCurrentRoute,
        onNewLocoClick = {
            router.showEmptyLocoForm(it)
        },
        onChangedLocoClick = router::showChangedLocoForm,
        onNewTrainClick = {
            router.showEmptyTrainForm(it)
        },
        onChangedTrainClick = router::showChangeTrainForm,
        onNewPassengerClick = {
            router.showEmptyPassengerForm(it)
        },
        onChangedPassengerClick = router::showChangePassengerForm,
        onGoClicked = homeViewModel::onGoClicked,
//        isOnTheWayState = homeViewModel.isOnTheWay,
//        isShowSnackbar = isShowSnackbarOnTheWay,
//        resetStateShowSnackbar = homeViewModel::resetStateShowSnackbar,
//        resetStateIsLaunchedInitState = homeViewModel::resetStateIsLaunchedInitState,
        onAllRouteClick = router::showAllRoute,
        uiState = uiState.uiState,
        saveTimeEvent = homeViewModel.saveTimeEvent,
        isNextDeparture = homeViewModel::isNextDeparture
    )
}