package com.example.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavBackStackEntry
import com.example.domain.navigation.Router
import com.example.route.Const.NULLABLE_ID
import com.example.route.ui.FormScreen
import com.example.route.viewmodel.FormViewModel
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun FormDestination(
    router: Router,
    backStackEntry: NavBackStackEntry
) {
    val routeId = FormRoute.getRouteId(backStackEntry) ?: NULLABLE_ID
    val viewModel = getViewModel<FormViewModel>(
        parameters = { parametersOf(routeId) }
    )
    val formUiState by viewModel.uiState.collectAsState()
    FormScreen(
        formUiState = formUiState,
        currentRoute = viewModel.currentRoute,
        onBackPressed = router::back,
        onRouteSaved = router::back,
        onSaveClick = viewModel::saveRoute,
        onNumberChanged = viewModel::setNumber,
        onSettingClick = router::showSettings,
        resetSaveState = viewModel::resetSaveState,
        onClearAllField = {},
        onTimeStartWorkChanged = viewModel::setTimeStartWork,
        onTimeEndWorkChanged = viewModel::setTimeEndWork,
        onRestChanged = viewModel::setRestValue,
        onChangedLocoClick = router::showChangedLocoForm,
        onNewLocoClick = router::showEmptyLocoForm,
        onDeleteLoco = viewModel::onDeleteLoco,
        onTrainClick = router::showTrainForm,
        onDeleteTrain = viewModel::onDeleteTrain,
        onPassengerClick = router::showPassengerForm,
        onDeletePassenger = viewModel::onDeletePassenger,
        onNotesClick = router::showNotesForm,
        onDeleteNotes = viewModel::onDeleteNotes
    )
}