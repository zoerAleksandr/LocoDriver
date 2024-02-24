package com.example.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavBackStackEntry
import com.example.domain.navigation.Router
import com.example.route.Const.NULLABLE_ID
import com.example.route.ui.FormTrainScreen
import com.example.route.viewmodel.TrainFormViewModel
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun FormTrainDestination(
    router: Router,
    backStackEntry: NavBackStackEntry
){
    val trainId = FormTrain.getTrainId(backStackEntry) ?: NULLABLE_ID
    val basicId = FormTrain.getBasicId(backStackEntry) ?: NULLABLE_ID

    val viewModel = getViewModel<TrainFormViewModel>(
        parameters = { parametersOf(trainId, basicId) }
    )
    val formUiState by viewModel.uiState.collectAsState()

    FormTrainScreen(
        formUiState = formUiState,
        currentTrain = viewModel.currentTrain,
        onBackPressed = router::back,
        onSaveClick = viewModel::saveTrain,
        onTrainSaved = router::back,
        onClearAllField = viewModel::clearAllField,
        resetSaveState = viewModel::resetSaveState,
        resetErrorMessage = viewModel::resetErrorMessage,
        onNumberChanged = viewModel::setNumber,
        onWeightChanged = viewModel::setWeight,
        onAxleChanged = viewModel::setAxle,
        onLengthChanged = viewModel::setConditionalLength,
        onAddingStation = viewModel::addingStation,
        onDeleteStation = viewModel::deleteStation,
        onStationNameChanged = viewModel::setStationName,
        onArrivalTimeChanged = viewModel::setArrivalTime,
        onDepartureTimeChanged = viewModel::setDepartureTime,
        stationListState = formUiState.stationsListState
    )
}