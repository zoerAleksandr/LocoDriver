package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.z_company.core.navigation.AppRoutes
import com.z_company.domain.navigation.Router
import com.z_company.route.ui.HomeScreen
import com.z_company.route.util.startShare
import com.z_company.route.viewmodel.home_view_model.HomeViewModel
import com.z_company.route.viewmodel.PullToSyncViewModel
import com.z_company.route.R

@Composable
fun HomeDestination(
    router: Router
) {
    val homeViewModel: HomeViewModel = viewModel()
    val pullToSyncViewModel: PullToSyncViewModel = viewModel()
    val pullToSyncState by pullToSyncViewModel.uiState.collectAsState()
    val uiState by homeViewModel.uiState.collectAsState()
    val previewRouteUiState by homeViewModel.previewRouteUiState.collectAsState()

    val months by homeViewModel.monthList.collectAsState()
    val years by homeViewModel.yearList.collectAsState()

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        homeViewModel.syncOnScreenOpen()
    }
    DisposableEffect(homeViewModel) {
        onDispose(homeViewModel::stopScreenSync)
    }
    LaunchedEffect(homeViewModel) {
        homeViewModel.shareLinkEvent.collect { data ->
            context.startShare(data)
        }
    }

    HomeScreen(
        viewModel = homeViewModel,
        listRouteState = uiState.listItemState,
        onRouteClick = {
            router.showRouteForm(it)
        },
        makeCopyRoute = { basicId ->
            router.showRouteForm(basicId = basicId, isMakeCopy = true)
        },
        onDeleteRoute = homeViewModel::removeRoute,
        onSearchClick = { router.showSearch() },
        totalTime = homeViewModel.timeWithoutHoliday,
        todayWorkTime = homeViewModel.todayWorkTime,
        isConsiderFutureRoute = homeViewModel.isConsiderFutureRoute,
        currentMonthOfYear = homeViewModel.currentMonthOfYear,
        yearList = years,
        monthList = months,
        selectYearAndMonth = homeViewModel::setCurrentMonth,
        minTimeRest = uiState.minTimeRest,
        nightTimeState = uiState.nightTimeInRouteList,
        totalTimeWithHoliday = uiState.totalTimeWithHoliday,
        toBeCredited = uiState.toBeCredited,
        onSalaryClick = router::showSalaryCalculation,
        passengerTimeState = uiState.passengerTimeInRouteList,
        singleLocomotiveTimeState = uiState.singleLocomotiveTimeState,
        calculationHomeRest = homeViewModel::calculationHomeRest,
        homeRestValue = previewRouteUiState.homeRest,
        calculationActualRest = homeViewModel::calculationActualRest,
        actualRestDuration = previewRouteUiState.actualRestDuration,
        actualRestUntil = previewRouteUiState.actualRestUntil,
        computeRoutePayment = homeViewModel::computeRoutePayment,
        previewPaymentText = previewRouteUiState.paymentText,
        offsetInMoscow = uiState.offsetInMoscow,
        timeCalculationContext = uiState.timeCalculationContext,
        syncRoute = homeViewModel::syncRoute,
        shareRoute = homeViewModel::shareRoute,
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
        nextFutureRoute = homeViewModel.nextFutureRoute,
        countdownToNextRoute = homeViewModel.countdownToNextRoute,
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
        onCalendar = router::showCalendar,
        onStatistics = router::showStatistics,
        unsyncedRoutesCount = uiState.unsyncedRoutesCount,
        hasActiveSubscription = uiState.hasActiveSubscription,
        subscriptionEndTime = uiState.subscriptionEndTime,
        onPurchasesClick = router::showPurchasesScreen,
        onSyncClick = homeViewModel::manualSync,
        isPullRefreshing = pullToSyncState.isRefreshing,
        onPullRefresh = { pullToSyncViewModel.refresh() },
        pullSyncMessage = pullToSyncState.message,
        onPullSyncMessageShown = pullToSyncViewModel::consumeMessage,
        showSyncDialog = uiState.showSyncDialog,
        isSyncSuccess = uiState.isSyncSuccess,
        isSyncComplete = uiState.isSyncComplete,
        syncType = uiState.syncType,
        syncUploadProgress = uiState.syncUploadProgress,
        syncRouteErrors = uiState.syncRouteErrors,
        syncRoutesTotalAttempted = uiState.syncRoutesTotalAttempted,
        syncRoutesSavedCount = uiState.syncRoutesSavedCount,
        syncReportUserId = uiState.syncReportUserId,
        isNetworkError = uiState.isNetworkError,
        onResetSyncState = homeViewModel::resetSyncState,
        normaHours = uiState.normaHours,
        isBackgroundSyncing = uiState.isBackgroundSyncing,
    )
}


sealed class NavigationItem(var route: AppRoutes, var icon: Int, var title: String) {
    data object Home : NavigationItem(HomeRoute, R.drawable.home_24px, "Главная")
    data object Money : NavigationItem(SalaryCalculationRoute, R.drawable.wallet_24px, "Зарплата")
    data object Add : NavigationItem(FormRoute, R.drawable.add_circle_24px, "Добавить")
    data object Setting : NavigationItem(SettingsScreenRoute, R.drawable.settings_24px, "Настройки")
    data object Profile : NavigationItem(ProfileRoute, R.drawable.account_circle_24px, "Профиль")
}
