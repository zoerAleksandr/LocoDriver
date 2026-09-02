package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.z_company.domain.navigation.Router
import com.z_company.route.ui.CalendarScreen
import com.z_company.route.util.toShareIntent
import com.z_company.route.viewmodel.CalendarViewModel
import com.z_company.route.viewmodel.PullToSyncViewModel

@Composable
fun CalendarDestination(
    router: Router,
) {
    val viewModel: CalendarViewModel = viewModel()
    val pullToSyncViewModel: PullToSyncViewModel = viewModel()
    val pullToSyncState by pullToSyncViewModel.uiState.collectAsState()
    val state by viewModel.uiState.collectAsState()
    val plan by viewModel.routePlan.collectAsState()
    val duplicatePlannedRoute by viewModel.duplicatePlannedRoute.collectAsState()
    val subscriptionLimit by viewModel.subscriptionLimit.collectAsState()
    val showPurchases = rememberShowPurchasesScreen(router)
    val previewRouteState by viewModel.previewRouteUiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.prepareScreen()
    }

    // Быстрый просмотр: шаринг маршрута → строим Intent и запускаем из Activity.
    LaunchedEffect(Unit) {
        viewModel.shareRouteEvent.collect { data ->
            context.startActivity(data.toShareIntent(fromActivity = true))
        }
    }

    // Быстрый просмотр: создание копии → открыть форму маршрута.
    LaunchedEffect(Unit) {
        viewModel.openRouteFormEvent.collect { event ->
            router.showRouteForm(basicId = event.basicId, isMakeCopy = event.isMakeCopy)
        }
    }

    // Обновляем месяц при возврате на экран (после добавления отвлечения/выходного/
    // редактирования маршрута с дочернего экрана).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.reload()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    CalendarScreen(
        state = state,
        plan = plan,
        onBack = router::back,
        onShiftMonth = viewModel::shiftMonth,
        onFillMonth = router::showScheduleWizard,
        onTripClick = { routeId -> router.showRouteForm(basicId = routeId, isMakeCopy = false) },
        onEnterRoutePlan = viewModel::enterRoutePlan,
        onExitRoutePlan = viewModel::exitRoutePlan,
        onPickTime = viewModel::pickPlanTime,
        onAddCustomTime = viewModel::addCustomPlanTime,
        onToggleDay = viewModel::toggleDayPlan,
        onCreateRoutes = viewModel::createPlannedRoutes,
        onAddAbsence = { router.showAbsence() },
        onAddDayoff = viewModel::addDayOff,
        onAddTechnicalStudy = viewModel::addTechnicalStudy,
        onDeleteAbsenceDay = viewModel::deleteAbsenceDay,
        onDeleteAbsencePeriod = viewModel::deleteAbsencePeriod,
        dateAndTimeConverter = state.dateAndTimeConverter,
        timeContext = state.timeContext,
        convertTimeToString = viewModel::formatWorkTime,
        onDeleteRoute = viewModel::deleteRoute,
        previewRouteState = previewRouteState,
        onCalculationHomeRest = viewModel::calculationHomeRest,
        onCalculationActualRest = viewModel::calculationActualRest,
        onToggleFavorite = viewModel::setFavoriteRoute,
        onShareRoute = viewModel::shareRoute,
        onSyncRoute = viewModel::syncRoute,
        onMakeCopy = viewModel::makeCopyRoute,
        duplicatePlannedRoute = duplicatePlannedRoute,
        onDismissDuplicatePlannedRoute = viewModel::dismissDuplicatePlannedRoute,
        onReplaceDuplicatePlannedRoute = viewModel::replaceDuplicateAndCreate,
        onKeepDuplicatePlannedRoute = viewModel::keepDuplicateAndCreate,
        subscriptionLimit = subscriptionLimit,
        onDismissSubscriptionLimit = viewModel::dismissSubscriptionLimit,
        onPurchasesClick = showPurchases,
        isPullRefreshing = pullToSyncState.isRefreshing,
        onPullRefresh = { pullToSyncViewModel.refresh(viewModel::reload) },
        pullSyncMessage = pullToSyncState.message,
        onPullSyncMessageShown = pullToSyncViewModel::consumeMessage,
    )
}
