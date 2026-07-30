package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavBackStackEntry
import com.z_company.domain.navigation.Router
import com.z_company.route.Const
import com.z_company.route.ui.FormOtherWorkScreen
import com.z_company.route.viewmodel.OtherWorkFormViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun FormOtherWorkDestination(
    router: Router,
    backStackEntry: NavBackStackEntry
) {
    val otherWorkId = FormOtherWork.getOtherWorkId(backStackEntry) ?: Const.NULLABLE_ID
    val basicId = FormOtherWork.getBasicId(backStackEntry) ?: Const.NULLABLE_ID

    val viewModel = koinViewModel<OtherWorkFormViewModel>(
        parameters = { parametersOf(otherWorkId, basicId) }
    )
    val formUiState by viewModel.uiState.collectAsState()

    FormOtherWorkScreen(
        viewModel = viewModel,
        formUiState = formUiState,
        currentOtherWork = viewModel.currentOtherWork,
        onExit = router::back,
        resetSaveState = viewModel::resetSaveState,
        onWorkTypeChanged = viewModel::setWorkType,
        onAddCustomType = viewModel::addCustomType,
        onDeleteCustomType = viewModel::deleteCustomType,
        onStationChanged = viewModel::setStation,
        onTimeStartChanged = viewModel::setTimeStart,
        onTimeEndChanged = viewModel::setTimeEnd,
        onApplyTimeFromWork = viewModel::applyTimeFromWork,
        onApplyTimeFromLoco = { viewModel.applyTimeFromLoco() },
        locoPicker = formUiState.locoPicker,
        onPickLoco = { loco -> viewModel.applyTimeFromLoco(loco) },
        onDismissLocoPicker = viewModel::dismissLocoPicker,
        messageFlow = viewModel.message,
        onNotesChanged = viewModel::setNotes,
        workTypeList = formUiState.workTypeList,
        stationDropDownList = formUiState.stationList,
        onChangedStationContent = viewModel::onChangedDropDownContentStation,
        onDeleteStationName = viewModel::removeStationName,
        resultTime = formUiState.resultTime,
        errorMessage = formUiState.errorMessage,
        dateAndTimeConverter = formUiState.dateAndTimeConverter
    )
}
