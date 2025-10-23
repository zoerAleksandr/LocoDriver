package com.z_company.route.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavBackStackEntry
import com.z_company.domain.navigation.Router
import com.z_company.route.Const.NULLABLE_ID
import com.z_company.route.navigation.HomeRoute
import com.z_company.route.ui.FormScreen
import com.z_company.route.ui.TestFormScreen
import com.z_company.route.viewmodel.FormScreenEvent
import com.z_company.route.viewmodel.FormViewModel
import com.z_company.route.viewmodel.TestFormViewModel
import com.z_company.route.viewmodel.home_view_model.StartPurchasesEvent
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import ru.rustore.sdk.pay.model.PurchaseAvailabilityResult

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
    val snackbarHostState = remember { SnackbarHostState() }

    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(Unit) {
        scope.launch {
            viewModel.events.flowWithLifecycle(lifecycle).collect { event ->
                when (event) {
                    FormScreenEvent.ActivatedFavoriteRoute -> {
                        snackbarHostState.showSnackbar(message = "Добавлен в избранное")
                    }

                    FormScreenEvent.DeactivatedFavoriteRoute -> {
                        snackbarHostState.showSnackbar(message = "Удален из избранного")
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.purchasesEvent.collect { event ->
            when (event) {
                is StartPurchasesEvent.PurchasesAvailability -> {
                    when (val avail = event.availability) {
                        is PurchaseAvailabilityResult.Available -> {
                            // UI performs navigation
                            router.showPurchasesScreen()
                        }

                        is PurchaseAvailabilityResult.Unavailable -> {
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

    FormScreen(
        viewModel = viewModel,
        formUiState = formUiState,
        dialogRestUiState = dialogRestUiState,
        currentRoute = viewModel.currentRoute,
        exitScreen = { router.showHome(HomeRoute.route) },
        isCopy = formUiState.isCopy,
        onSaveClick = viewModel::onSaveClick,
        onBack = viewModel::checkBeforeExitTheScreen,
        onNumberChanged = viewModel::setNumber,
        checkedOnePersonOperation = viewModel::setOnePersonOperation,
        onNotesChanged = viewModel::setNotes,
        onSettingClick = router::showSettings,
        resetSaveState = viewModel::resetSaveState,
        onTimeStartWorkChanged = viewModel::setTimeStartWork,
        onTimeEndWorkChanged = viewModel::setTimeEndWork,
        onRestChanged = viewModel::setRestValue,
        onChangedLocoClick = router::showChangedLocoForm,
        onNewLocoClick = {
            router.showEmptyLocoForm(it)
            viewModel.preSaveRoute()
        },
        onDeleteLoco = viewModel::onDeleteLoco,
        onChangeTrainClick = router::showChangeTrainForm,
        onNewTrainClick = {
            router.showEmptyTrainForm(it)
            viewModel.preSaveRoute()
        },
        onDeleteTrain = viewModel::onDeleteTrain,
        onChangePassengerClick = router::showChangePassengerForm,
        onNewPassengerClick = {
            router.showEmptyPassengerForm(it)
            viewModel.preSaveRoute()
        },
        onDeletePassenger = viewModel::onDeletePassenger,
        nightTime = formUiState.nightTime,
        changeShowConfirmExitDialog = viewModel::changeShowConfirmDialog,
        exitWithoutSave = viewModel::exitWithoutSaving,
        salaryForRouteState = salaryState,
        onSalarySettingClick = router::showSettingSalary,
        setFavoriteState = viewModel::setFavoriteRoute,
        checkIsCorrectTime = viewModel::isValidTime,
        timeZoneText = viewModel.timeZoneText,
        dateAndTimeConverter = formUiState.dateAndTimeConverter
    )
}