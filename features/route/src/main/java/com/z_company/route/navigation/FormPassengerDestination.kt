package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavBackStackEntry
import com.z_company.core.ResultState
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
    // Единый источник — снапшот formUiState.passengerDetailState, а не отдельный
    // геттер viewModel.currentPassenger. Два независимых чтения геттера в одной
    // композиции не гарантированно консистентны между собой при параллельной
    // записи из корутины (init/автосейв), что могло давать рассинхронизацию
    // между currentPassenger и isWorkStartByArrival на экране.
    val currentPassenger = (formUiState.passengerDetailState as? ResultState.Success)?.data

    FormPassengerScreen(
        viewModel = viewModel,
        formUiState = formUiState,
        currentPassenger = currentPassenger,
        onPassengerSaved = router::back,
        resetSaveState = viewModel::resetSaveState,
        onNumberChanged = viewModel::setNumberTrain,
        onStationDepartureChanged = viewModel::setStationDeparture,
        onStationArrivalChanged = viewModel::setStationArrival,
        onTimeDepartureChanged = viewModel::setTimeDeparture,
        onTimeArrivalChanged = viewModel::setTimeArrival,
        onNotesChanged = viewModel::setNotes,
        isWorkStartByArrival = currentPassenger?.isWorkStartByArrival ?: false,
        onWorkStartByArrivalChanged = viewModel::setWorkStartByArrival,
        resultTime = formUiState.resultTime,
        errorMessage = formUiState.errorMessage,
        dropDownMenuList = formUiState.stationList,
        changeExpandMenuDepartureStation = viewModel::changeExpandMenuDepartureStation,
        changeExpandMenuArrivalStation = viewModel::changeExpandMenuArrivalStation,
        isExpandedMenuDepartureStation = formUiState.isExpandMenuDepartureStation,
        isExpandedMenuArrivalStation = formUiState.isExpandMenuArrivalStation,
        onDeleteStationName = viewModel::removeStationName,
        onChangedDropDownContentDepartureStation = viewModel::onChangedDropDownContentDepartureStation,
        onChangedDropDownContentArrivalStation = viewModel::onChangedDropDownContentArrivalStation,
        dateAndTimeConverter = formUiState.dateAndTimeConverter
    )
}