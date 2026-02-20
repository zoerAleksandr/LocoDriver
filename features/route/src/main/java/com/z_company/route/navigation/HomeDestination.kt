package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.z_company.core.navigation.AppRoutes
import com.z_company.domain.navigation.Router
import com.z_company.route.ui.HomeScreen
import com.z_company.route.viewmodel.home_view_model.HomeViewModel
import com.z_company.route.R

@Composable
fun HomeDestination(
    router: Router
) {
    val homeViewModel: HomeViewModel = viewModel()
    val uiState by homeViewModel.uiState.collectAsState()
    val previewRouteUiState by homeViewModel.previewRouteUiState.collectAsState()

    val months by homeViewModel.monthList.collectAsState()
    val years by homeViewModel.yearList.collectAsState()


    HomeScreen(
        viewModel = homeViewModel,
        listRouteState = uiState.listItemState,
        onRouteClick = {
            router.showRouteForm(it)
        },
        makeCopyRoute = { basicId ->
            router.showRouteForm(basicId = basicId, isMakeCopy = true)
        },
        onMoreInfoClick = { router.showMoreInfo(it) },
        onDeleteRoute = homeViewModel::removeRoute,
        onSearchClick = { router.showSearch() },
        totalTime = homeViewModel.timeWithoutHoliday,
        currentMonthOfYear = homeViewModel.currentMonthOfYear,
        yearList = years,
        monthList = months,
        selectYearAndMonth = homeViewModel::setCurrentMonth,
        minTimeRest = uiState.minTimeRest,
        nightTimeState = uiState.nightTimeInRouteList,
        totalTimeWithHoliday = uiState.totalTimeWithHoliday,
        passengerTimeState = uiState.passengerTimeInRouteList,
        singleLocomotiveTimeState = uiState.singleLocomotiveTimeState,
        calculationHomeRest = homeViewModel::calculationHomeRest,
        homeRestValue = previewRouteUiState.homeRest,
        offsetInMoscow = uiState.offsetInMoscow,
        syncRoute = homeViewModel::syncRoute,
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
        onWorkScheduleScreen = router::showWorkScheduleScreen,
        onClickVacation = router::showSelectReleaseDayScreen
    )
}


sealed class NavigationItem(var route: AppRoutes, var icon: Int, var title: String) {
    data object Home : NavigationItem(HomeRoute, R.drawable.home_24px, "Главная")
    data object Money : NavigationItem(SalaryCalculationRoute, R.drawable.wallet_24px, "Зарплата")
    data object Add : NavigationItem(FormRoute, R.drawable.add_circle_24px, "Добавить")
    data object Setting : NavigationItem(SettingsScreenRoute, R.drawable.settings_24px, "Настройки")
    data object Profile : NavigationItem(ProfileRoute, R.drawable.account_circle_24px, "Профиль")
}