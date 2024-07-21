package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavBackStackEntry
import com.z_company.domain.navigation.Router
import com.z_company.route.Const.NULLABLE_ID
import com.z_company.route.ui.FormLocoScreen
import com.z_company.route.viewmodel.LocoFormViewModel
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun FormLocoDestination(
    router: Router,
    backStackEntry: NavBackStackEntry
) {
    val locoId = FormLoco.getLocoId(backStackEntry) ?: NULLABLE_ID
    val basicId = FormLoco.getBasicId(backStackEntry) ?: NULLABLE_ID
    val viewModel = getViewModel<LocoFormViewModel>(
        parameters = { parametersOf(locoId, basicId) }
    )
    val formUiState by viewModel.uiState.collectAsState()

    FormLocoScreen(
        currentLoco = viewModel.currentLoco,
        dieselSectionListState = formUiState.dieselSectionList,
        electricSectionListState = formUiState.electricSectionList,
        onBackPressed = viewModel::checkBeforeExitTheScreen,
        onSaveClick = viewModel::saveLoco,
        onLocoSaved = router::back,
        onClearAllField = viewModel::clearAllField,
        formUiState = formUiState,
        resetSaveState = viewModel::resetSaveState,
        onNumberChanged = viewModel::setNumber,
        onSeriesChanged = viewModel::setSeries,
        onChangedTypeLoco = viewModel::changeLocoType,
        onStartAcceptedTimeChanged = viewModel::setStartAcceptedTime,
        onEndAcceptedTimeChanged = viewModel::setEndAcceptedTime,
        onStartDeliveryTimeChanged = viewModel::setStartDeliveryTime,
        onEndDeliveryTimeChanged = viewModel::setEndDeliveryTime,
        onFuelAcceptedChanged = viewModel::setFuelAccepted,
        onFuelDeliveredChanged = viewModel::setFuelDelivery,
        onDeleteSectionDiesel = viewModel::deleteSectionDiesel,
        addingSectionDiesel = viewModel::addingSectionDiesel,
        focusChangedDieselSection = viewModel::focusChangedDieselSection,
        onEnergyAcceptedChanged = viewModel::setEnergyAccepted,
        onEnergyDeliveryChanged = viewModel::setEnergyDelivery,
        onRecoveryAcceptedChanged = viewModel::setRecoveryAccepted,
        onRecoveryDeliveryChanged = viewModel::setRecoveryDelivery,
        onDeleteSectionElectric = viewModel::deleteSectionElectric,
        addingSectionElectric = viewModel::addingSectionElectric,
        focusChangedElectricSection = viewModel::focusChangedElectricSection,
        onExpandStateElectricSection = viewModel::isExpandElectricItem,
        isShowRefuelDialog = formUiState.refuelDialogShow,
        showRefuelDialog = viewModel::showRefuelDialog,
        onRefuelValueChanged = viewModel::setRefuel,
        isShowCoefficientDialog = formUiState.coefficientDialogShow,
        showCoefficientDialog = viewModel::showCoefficientDialog,
        onCoefficientValueChanged = viewModel::setCoefficient,
        exitScreen = router::back,
        changeShowConfirmExitDialog = viewModel::changeShowConfirmDialog,
        exitWithoutSave = viewModel::exitWithoutSaving
    )
}