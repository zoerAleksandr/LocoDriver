package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavBackStackEntry
import com.z_company.core.ui.snackbar.ISnackbarManager
import com.z_company.domain.navigation.Router
import com.z_company.route.Const.NULLABLE_ID
import com.z_company.route.ui.FormScreen
import com.z_company.route.util.startShare
import com.z_company.route.viewmodel.ChildEntityType
import com.z_company.route.viewmodel.FormScreenEvent
import com.z_company.route.viewmodel.FormViewModel
import com.z_company.route.viewmodel.home_view_model.StartPurchasesEvent
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun FormDestination(
    router: Router,
    backStackEntry: NavBackStackEntry
) {
    val routeId = FormRoute.getRouteId(backStackEntry) ?: NULLABLE_ID
    val isMakeCopy = FormRoute.isMakeCopy(backStackEntry)
    val viewModel = koinViewModel<FormViewModel>(
        parameters = { parametersOf(routeId, isMakeCopy) }
    )
    val formUiState by viewModel.uiState.collectAsState()
    val dialogRestUiState by viewModel.dialogRestUiState.collectAsState()
    val salaryState by viewModel.salaryForRouteState.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarManager: ISnackbarManager = koinInject()

    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val currentRoute by viewModel.currentRoute.collectAsState()
    val dateAndTimeConverter by viewModel.dateAndTimeConverter.collectAsState()
    val userSetting by viewModel.userSetting.collectAsState()

    // Share intent из overflow-меню FormScreen
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.shareLinkEvent.collect { data ->
            context.startShare(data)
        }
    }

    LaunchedEffect(Unit) {
        scope.launch {
            viewModel.events.flowWithLifecycle(lifecycle).collect { event ->
                when (event) {
                    FormScreenEvent.ActivatedFavoriteRoute -> {
                        snackbarManager.show(message = "Добавлен в избранное")
                    }

                    FormScreenEvent.DeactivatedFavoriteRoute -> {
                        snackbarManager.show(message = "Удален из избранного")
                    }

                    FormScreenEvent.RouteSaved -> {
                        // Не показываем тост здесь: FormScreen ещё активен и его
                        // SnackbarHost поглотит событие из Channel до того как экран-источник
                        // станет активным коллектором. Тост «Маршрут сохранен» показывается
                        // из FormViewModel после завершения навигации (delay + sync).
                        launch { viewModel.prepareReviewDialog() }
                        // Возврат на экран, откуда открыли форму (Главный / Все маршруты /
                        // Поиск), а не всегда на Главный.
                        router.back()
                    }

                    is FormScreenEvent.NavigateToChildForm -> {
                        when (event.entityType) {
                            ChildEntityType.LOCOMOTIVE -> router.showEmptyLocoForm(event.basicId)
                            ChildEntityType.TRAIN -> router.showEmptyTrainForm(event.basicId)
                            ChildEntityType.PASSENGER -> router.showEmptyPassengerForm(event.basicId)
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.purchasesEvent.collect { event ->
            when (event) {
                is StartPurchasesEvent.ShowPurchasesScreen -> router.showPurchasesScreen()
                is StartPurchasesEvent.Error -> {}
            }
        }
    }

    FormScreen(
        viewModel = viewModel,
        formUiState = formUiState,
        dialogRestUiState = dialogRestUiState,
        currentRoute = currentRoute,
        exitScreen = { router.back() },
        isCopy = formUiState.isCopy,
        isNewRoute = routeId == NULLABLE_ID,
        onNumberChanged = viewModel::setNumber,
        checkedOnePersonOperation = viewModel::checkedOnePersonOperation,
        onNotesChanged = viewModel::setNotes,
        onSettingClick = router::showSettingsRoute,
        onFormSettingsClick = router::showSettingsRouteForm,
        onRestSettingClick = router::showSettingsRest,
        resetSaveState = viewModel::resetSaveState,
        onTimeStartWorkChanged = viewModel::setTimeStartWork,
        onTimeEndWorkChanged = viewModel::setTimeEndWork,
        onTimeStartBreakChanged = viewModel::setTimeStartBreak,
        onTimeEndBreakChanged = viewModel::setTimeEndBreak,
        isShowBreak = userSetting?.isShowBreak ?: true,
        isShowOnePersonSwitch = userSetting?.isShowOnePersonSwitch ?: true,
        onRestChanged = viewModel::onRestChanged,
        onChangedLocoClick = router::showChangedLocoForm,
        onNewLocoClick = { viewModel.onAddChildEntity(it, ChildEntityType.LOCOMOTIVE) },
        onDeleteLoco = viewModel::onDeleteLoco,
        onChangeTrainClick = router::showChangeTrainForm,
        onNewTrainClick = { viewModel.onAddChildEntity(it, ChildEntityType.TRAIN) },
        onDeleteTrain = viewModel::onDeleteTrain,
        onChangePassengerClick = router::showChangePassengerForm,
        onNewPassengerClick = { viewModel.onAddChildEntity(it, ChildEntityType.PASSENGER) },
        onDeletePassenger = viewModel::onDeletePassenger,
        nightTime = formUiState.nightTime,
        salaryForRouteState = salaryState,
        onSalarySettingClick = router::showSettingSalary,
        setFavoriteState = viewModel::setFavoriteRoute,
        dateAndTimeConverter = dateAndTimeConverter,
        showPurchasesScreen = router::showPurchasesScreen,
        onCopyClick = {
            currentRoute?.basicData?.id?.let { id ->
                router.showRouteForm(basicId = id, isMakeCopy = true)
            }
        }
    )
}