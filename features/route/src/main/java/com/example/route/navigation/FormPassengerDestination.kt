package com.example.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavBackStackEntry
import com.example.domain.navigation.Router
import com.example.route.Const
import com.example.route.ui.FormPassengerScreen
import com.example.route.viewmodel.PassengerFormViewModel
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun FormPassengerDestination(
    router: Router,
    backStackEntry: NavBackStackEntry
){
    val passengerId = FormPassenger.getPassengerId(backStackEntry) ?: Const.NULLABLE_ID
    val basicId = FormPassenger.getBasicId(backStackEntry) ?: Const.NULLABLE_ID

    val viewModel = getViewModel<PassengerFormViewModel>(
        parameters = { parametersOf(passengerId, basicId) }
    )
    val formUiState by viewModel.uiState.collectAsState()

    FormPassengerScreen(
        currentPassenger = viewModel.currentPassenger,
        passengerDetailState = formUiState.passengerDetailState,
        savePassengerState = formUiState.savePassengerState,
        onBackPressed = router::back,
        onSaveClick =  viewModel::savePassenger,
        onPassengerSaved = router::back,
        onClearAllField = viewModel::clearAllField,
        resetSaveState = viewModel::resetSaveState,
        onNumberChanged = viewModel::setNumberTrain,
        onStationDepartureChanged = viewModel::setStationDeparture,
        onStationArrivalChanged = viewModel::setStationArrival,
        onTimeDepartureChanged = viewModel::setTimeDeparture,
        onTimeArrivalChanged = viewModel::setTimeArrival,
        onNotesChanged = viewModel::setNotes,
        resultTime = formUiState.resultTime,
        errorState = formUiState.errorTimeState,
        resetError = viewModel::resetErrorState,
        formValid = formUiState.formValid
    )
}