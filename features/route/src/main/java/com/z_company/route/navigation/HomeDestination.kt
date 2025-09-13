package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.z_company.domain.navigation.Router
import com.z_company.route.ui.HomeScreen
import com.z_company.route.viewmodel.home_view_model.HomeViewModel
import com.z_company.route.viewmodel.home_view_model.StartPurchasesEvent
import ru.rustore.sdk.core.feature.model.FeatureAvailabilityResult

@Composable
fun HomeDestination(
    router: Router
) {
    val homeViewModel: HomeViewModel = viewModel()
    val uiState by homeViewModel.uiState.collectAsState()
    val previewRouteUiState by homeViewModel.previewRouteUiState.collectAsState()

    // Подписываемся на событие открытия формы и выполняем навигацию через router
    LaunchedEffect(Unit) {
        homeViewModel.openRouteFormEvent.collect { event ->
            router.showRouteForm(basicId = event.basicId, isMakeCopy = event.isMakeCopy)
        }
    }

    LaunchedEffect(Unit) {
        homeViewModel.purchasesEvent.collect { event ->
            when (event) {
                is StartPurchasesEvent.PurchasesAvailability -> {
                    when (val avail = event.availability) {
                        is FeatureAvailabilityResult.Available -> {
                            // UI performs navigation
                            router.showPurchasesScreen()
                        }

                        is FeatureAvailabilityResult.Unavailable -> {
                            // ViewModel already showed snackbar; optionally handle here
                        }
                    }
                }

                is StartPurchasesEvent.Error -> {
                    // event.throwable - show fallback snackbar or handle
                    // you can also rely on ViewModel to show snackbar via snackbarManager
                }
            }
        }
    }

    HomeScreen(
        listRouteState = uiState.listItemState,
        onRouteClick = {
            router.showRouteForm(it)
        },
        makeCopyRoute = { basicId -> homeViewModel.newRouteClick(basicId) },
        onMoreInfoClick = { router.showMoreInfo(it) },
        onNewRouteClick = homeViewModel::newRouteClick,
        onDeleteRoute = homeViewModel::removeRoute,
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
        calculationHomeRest = homeViewModel::calculationHomeRest,
        homeRestValue = previewRouteUiState.homeRest,
        firstEntryDialogState = uiState.showFirstEntryToAccountDialog,
        resetStateFirstEntryDialog = homeViewModel::disableFirstEntryToAccountDialog,
        showFormScreen =  router::showRouteForm,
        isLoadingStateAddButton = uiState.isLoadingStateAddButton,
        alertBeforePurchasesState = homeViewModel.alertBeforePurchasesEvent,
        checkPurchasesAvailability = homeViewModel::checkPurchasesAvailability,
        restorePurchases = homeViewModel::restorePurchases,
        offsetInMoscow = uiState.offsetInMoscow,
        syncRoute = homeViewModel::syncRoute,
        completeUpdateRequested = homeViewModel::completeUpdateRequested,
        updateEvent = homeViewModel.updateEvents,
        setFavoriteState = homeViewModel::setFavoriteRoute,
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
        onAllRouteClick = router::showAllRoute,
        uiState = uiState.uiState,
        saveTimeEvent = homeViewModel.saveTimeEvent,
        isNextDeparture = homeViewModel::isNextDeparture,
    )
}