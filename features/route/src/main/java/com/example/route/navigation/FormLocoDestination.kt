package com.example.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavBackStackEntry
import com.example.domain.navigation.Router
import com.example.route.Const.NULLABLE_ID
import com.example.route.ui.FormLocoScreen
import com.example.route.viewmodel.LocoFormViewModel
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
        onBackPressed =  router::back,
        onSaveClick = viewModel::saveLoco,
        onLocoSaved = router::back,
        onClearAllField = { /*TODO*/ },
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
        onDeleteSectionDiesel = viewModel::deleteSection
    )
}