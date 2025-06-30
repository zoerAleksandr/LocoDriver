package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavBackStackEntry
import com.z_company.domain.navigation.Router
import com.z_company.route.Const
import com.z_company.route.ui.FormPassengerScreen
import com.z_company.route.viewmodel.PassengerFormViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun FormPassengerDestination(
    router: Router,
    backStackEntry: NavBackStackEntry
){
    val passengerId = FormPassenger.getPassengerId(backStackEntry) ?: Const.NULLABLE_ID
    val basicId = FormPassenger.getBasicId(backStackEntry) ?: Const.NULLABLE_ID

    val viewModel = koinViewModel<PassengerFormViewModel>(
        parameters = { parametersOf(passengerId, basicId) }
    )
    val formUiState by viewModel.uiState.collectAsState()

    FormPassengerScreen(
        currentPassenger = viewModel.currentPassenger,
        passengerDetailState = formUiState.passengerDetailState,
        changeHaveState = formUiState.changesHaveState,
        savePassengerState = formUiState.savePassengerState,
        onBackPressed = viewModel::checkBeforeExitTheScreen,
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
        errorMessage = formUiState.errorMessage,
        resetError = viewModel::resetErrorState,
        formValid = formUiState.formValid,
        changeShowConfirmExitDialog = viewModel::changeShowConfirmDialog,
        exitFromScreenState = formUiState.exitFromScreen,
        exitScreen = router::back,
        exitWithoutSave = viewModel::exitWithoutSaving,
        showConfirmExitDialogState = formUiState.confirmExitDialogShow,
        dropDownMenuList = formUiState.stationList,
        changeExpandMenuDepartureStation = viewModel::changeExpandMenuDepartureStation,
        changeExpandMenuArrivalStation = viewModel::changeExpandMenuArrivalStation,
        isExpandedMenuDepartureStation = formUiState.isExpandMenuDepartureStation,
        isExpandedMenuArrivalStation = formUiState.isExpandMenuArrivalStation,
        onDeleteStationName = viewModel::removeStationName,
        onChangedDropDownContentDepartureStation = viewModel::onChangedDropDownContentDepartureStation,
        onChangedDropDownContentArrivalStation = viewModel::onChangedDropDownContentArrivalStation,
        onSettingClick = router::showSettings
    )
}