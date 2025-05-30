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
        onSaveClick = viewModel::checkForChangesTariffRate,
        saveSettingState = uiState.saveSettingState,
        uiState = uiState,
        resetSaveState = viewModel::resetSaveState,
        isEnableSaveButton = uiState.isEnableSaveButton,
        tariffRateValueState = uiState.tariffRate,
        setTariffRate = viewModel::setTariffRate,
        oldTariffRateValueState = uiState.oldTariffRate,
        setOldTariffRate = viewModel::setOldTariffRate,
        isErrorInputTariffRate = uiState.isErrorInputTariffRate,
        setAveragePaymentHour = viewModel::setAveragePaymentHour,
        setDistrictCoefficient = viewModel::setDistrictCoefficient,
        setNordicCoefficient = viewModel::setNordicCoefficient,
        zonalSurchargeValueState = uiState.zonalSurcharge,
        setZonalSurcharge = viewModel::setZonalSurcharge,
        isErrorInputZonalSurcharge = uiState.isErrorInputZonalSurcharge,
        surchargeQualificationClassValueState = uiState.surchargeQualificationClass,
        setSurchargeQualificationClass = viewModel::setSurchargeQualificationClass,
        isErrorInputSurchargeQualificationClass = uiState.isErrorInputSurchargeQualificationClass,
        onePersonOperationPercent = uiState.onePersonOperationPercent,
        setOnePersonOperationPercent = viewModel::setOnePersonOperationPercent,
        isErrorInputOnePersonOperation = uiState.isErrorInputOnePersonOperation,
        harmfulnessPercentState = uiState.harmfulnessPercent,
        setHarmfulnessPercent = viewModel::setHarmfulnessPercent,
        isErrorInputHarmfulness = uiState.isErrorInputHarmfulnessPercent,
        surchargeLongDistanceTrainState = uiState.longDistanceTrainPercent,
        setSurchargeLongTrain = viewModel::setSurchargeLongTrain,
        isErrorInputSurchargeLongDistance = uiState.isErrorInputLongDistanceTrainPercent,
        lengthLongDistanceTrainState = uiState.lengthLongDistanceTrain,
        setLengthLongDistanceTrain = viewModel::setLengthLongDistanceTrain,
        isErrorInputLengthLongDistance = uiState.isErrorInputLengthLongDistanceTrain,
        surchargeHeavyTrainsState = uiState.surchargeHeavyTrain,
        addSurchargeHeavyTran = viewModel::addSurchargeHeavyTrain,
        setSurchargeHeavyTrainWeight = viewModel::setSurchargeHeavyTrainWeight,
        setSurchargeHeavyTrainPercent = viewModel::setSurchargeHeavyTrainPercent,
        onSurchargeHeavyTrainDismissed = viewModel::deleteSurchargeHeavyTrain,
        surchargeExtendedServicePhaseValueState = uiState.surchargeExtendedServicePhaseList,
        setSurchargeExtendedServicePhaseDistance = viewModel::setSurchargeExtendedServicePhaseDistance,
        setSurchargeExtendedServicePhasePercent = viewModel::setSurchargeExtendedServicePhasePercent,
        addServicePhase = viewModel::addSurchargeExtendedServicePhase,
        ndflValueState = uiState.ndfl,
        setNDFL = viewModel::setNDFL,
        unionistsRetentionState = uiState.unionistsRetentionState,
        setUnionistsRetention = viewModel::setUnionistsRetention,
        isErrorInputUnionistsRetention = uiState.isErrorInputUnionistsRetention,
        isErrorInputNdfl = uiState.isErrorInputNdfl,
        otherRetentionValueState = uiState.otherRetention,
        setOtherRetention = viewModel::setOtherRetention,
        isErrorInputOtherRetention = uiState.isErrorInputOtherRetention,
        onServicePhaseDismissed = viewModel::deleteSurchargeExtendedServicePhase,
        isShowDialogChangeTariffRate = uiState.isShowDialogChangeTariffRate,
        onHideDialogChangeTariffRate = viewModel::hideDialogTariffRate,
        saveOnlyMonthTariffRate = viewModel::saveSettingAndOnlyMonthTariffRate,
        saveTariffRateCurrentAndNextMonth = viewModel::saveSettingAndTariffRateCurrentAndNextMonth,
        setOtherSurcharge = viewModel::setOtherSurcharge,
        currentMonthOfYear = uiState.currentMonthOfYear,
        setDateNewTariffRate = viewModel::setDateSetTariffRate
    )
}