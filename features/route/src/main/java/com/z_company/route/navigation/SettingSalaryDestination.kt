package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.z_company.domain.navigation.Router
import com.z_company.route.ui.SettingSalaryScreen
import com.z_company.route.viewmodel.SettingSalaryViewModel

@Composable
fun SettingSalaryDestination(
    router: Router
) {
    val viewModel: SettingSalaryViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    SettingSalaryScreen(
        onBack = router::back,
        onSaveClick = viewModel::saveSetting,
        saveSettingState = uiState.saveSettingState,
        resetSaveState = viewModel::resetSaveState,
        isEnableSaveButton = uiState.isEnableSaveButton,
        tariffRateValueState = uiState.tariffRate,
        setTariffRate = viewModel::setTariffRate,
        isErrorInputTariffRate = uiState.isErrorInputTariffRate,
        zonalSurchargeValueState = uiState.zonalSurcharge,
        setZonalSurcharge = viewModel::setZonalSurcharge,
        isErrorInputZonalSurcharge = uiState.isErrorInputZonalSurcharge,
        surchargeQualificationClassValueState = uiState.surchargeQualificationClass,
        setSurchargeQualificationClass = viewModel::setSurchargeQualificationClass,
        isErrorInputSurchargeQualificationClass = uiState.isErrorInputSurchargeQualificationClass,
        surchargeExtendedServicePhaseValueState = uiState.surchargeExtendedServicePhase,
        setSurchargeExtendedServicePhase = viewModel::setSurchargeExtendedServicePhase,
        isErrorInputSurchargeExtendedServicePhase = uiState.isErrorInputSurchargeExtendedServicePhase,
        surchargeHeavyLongDistanceTrainsValueState = uiState.surchargeHeavyLongDistanceTrains,
        setSurchargeHeavyLongDistanceTrains = viewModel::setSurchargeHeavyLongDistanceTrains,
        isErrorInputSurchargeHeavyLongDistanceTrains = uiState.isErrorInputSurchargeHeavyLongDistanceTrains,
        otherSurchargeState = uiState.otherSurchargeState,
        setOtherSurcharge = viewModel::setOtherSurcharge,
        isErrorInputOtherSurcharge = uiState.isErrorInputOtherSurcharge,
        ndflValueState = uiState.ndfl,
        setNDFL = viewModel::setNDFL,
        unionistsRetentionState = uiState.unionistsRetentionState,
        setUnionistsRetention = viewModel::setUnionistsRetention,
        isErrorInputUnionistsRetention = uiState.isErrorInputUnionistsRetention,
        isErrorInputNdfl = uiState.isErrorInputNdfl,
        otherRetentionValueState = uiState.otherRetention,
        setOtherRetention = viewModel::setOtherRetention,
        isErrorInputOtherRetention = uiState.isErrorInputOtherRetention,
    )
}