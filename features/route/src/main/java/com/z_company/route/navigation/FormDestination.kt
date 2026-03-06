package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import com.z_company.domain.navigation.Router
import com.z_company.route.Const.NULLABLE_ID
import com.z_company.shared.ui.screen.FormRouteScreen as SharedFormRouteScreen

@Composable
fun FormDestination(
    router: Router,
    backStackEntry: NavBackStackEntry
) {
    val routeId = FormRoute.getRouteId(backStackEntry)?.takeIf { it != NULLABLE_ID }

    SharedFormRouteScreen(
        routeId = routeId,
        onBackClick = { router.showHome(HomeRoute.route) },
        onLocoClick = { locoId, basicId ->
            router.showChangedLocoForm(
                com.z_company.domain.entities.route.Locomotive(locoId = locoId, basicId = basicId)
            )
        },
        onNewLocoClick = { basicId -> router.showEmptyLocoForm(basicId) },
        onTrainClick = { trainId, basicId ->
            router.showChangeTrainForm(
                com.z_company.domain.entities.route.Train(trainId = trainId, basicId = basicId)
            )
        },
        onNewTrainClick = { basicId -> router.showEmptyTrainForm(basicId) },
        onPassengerClick = { passengerId, basicId ->
            router.showChangePassengerForm(
                com.z_company.domain.entities.route.Passenger(passengerId = passengerId, basicId = basicId)
            )
        },
        onNewPassengerClick = { basicId -> router.showEmptyPassengerForm(basicId) },
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
                        snackbarManager.show(message = "Маршрут сохранен")
                        viewModel::prepareReviewDialog
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
        exitScreen = { router.showHome(HomeRoute.route) },
        isCopy = formUiState.isCopy,
        onNumberChanged = viewModel::setNumber,
        checkedOnePersonOperation = viewModel::checkedOnePersonOperation,
        onNotesChanged = viewModel::setNotes,
        onSettingClick = router::showSettings,
        resetSaveState = viewModel::resetSaveState,
        onTimeStartWorkChanged = viewModel::setTimeStartWork,
        onTimeEndWorkChanged = viewModel::setTimeEndWork,
        onTimeStartBreakChanged = viewModel::setTimeStartBreak,
        onTimeEndBreakChanged = viewModel::setTimeEndBreak,
        isShowBreak = userSetting?.isShowBreak ?: true,
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
        showPurchasesScreen = router::showPurchasesScreen
    )
}