package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavBackStackEntry
import com.z_company.domain.navigation.Router
import com.z_company.route.Const.NULLABLE_ID
import com.z_company.route.ui.FormScreen
import com.z_company.route.viewmodel.FormViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun FormDestination(
    router: Router,
    backStackEntry: NavBackStackEntry
) {
    val routeId = FormRoute.getRouteId(backStackEntry) ?: NULLABLE_ID
    val viewModel = koinViewModel<FormViewModel>(
        parameters = { parametersOf(routeId) }
    )
    val formUiState by viewModel.uiState.collectAsState()
    FormScreen(
        formUiState = formUiState,
        currentRoute = viewModel.currentRoute,
        exitScreen = router::back,
        onSaveClick = viewModel::saveRoute,
        onBack = viewModel::checkBeforeExitTheScreen,
        onNumberChanged = viewModel::setNumber,
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
        minTimeRest = viewModel.minTimeRest,
        nightTime = formUiState.nightTime,
        changeShowConfirmExitDialog = viewModel::changeShowConfirmDialog,
        exitWithoutSave = viewModel::exitWithoutSaving
    )
}