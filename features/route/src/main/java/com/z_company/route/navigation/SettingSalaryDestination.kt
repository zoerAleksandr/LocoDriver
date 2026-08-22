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
        onePersonOperationPassengerTrainPercent = uiState.onePersonOperationPassengerTrainPercent,
        setOnePersonOperationPassengerTrainPercent = viewModel::setOnePersonOperationPassengerTrainPercent,
        isErrorInputOnePersonOperationPassengerTrain = uiState.isErrorInputOnePersonOperationPassengerTrain,
        harmfulnessPercentState = uiState.harmfulnessPercent,
        setHarmfulnessPercent = viewModel::setHarmfulnessPercent,
        isErrorInputHarmfulness = uiState.isErrorInputHarmfulnessPercent,
        surchargeHeavyLongDistanceTrainsState = uiState.surchargeHeavyLongDistanceTrains,
        setSurchargeHeavyLongDistanceTrains = viewModel::setSurchargeHeavyLongDistanceTrains,
        isErrorInputSurchargeHeavyLongDistanceTrains = uiState.isErrorInputSurchargeHeavyLongDistanceTrains,
        surchargeHeavyTrainsState = uiState.surchargeHeavyTrain,
        addSurchargeHeavyTran = viewModel::addSurchargeHeavyTrain,
        setSurchargeHeavyTrainPercent = viewModel::setSurchargeHeavyTrainPercent,
        setSurchargeHeavyTrainWeight = viewModel::setSurchargeHeavyTrainWeight,
        onSurchargeHeavyTrainDismissed = viewModel::deleteSurchargeHeavyTrain,
        surchargeLongTrainsState = uiState.surchargeLongTrain,
        addSurchargeLongTrain = viewModel::addSurchargeLongTrain,
        setSurchargeLongTrainPercent = viewModel::setSurchargeLongTrainPercent,
        setSurchargeLongTrainLength = viewModel::setSurchargeLongTrainLength,
        onSurchargeLongTrainDismissed = viewModel::deleteSurchargeLongTrain,
        surchargeExtendedServicePhaseValueState = uiState.surchargeExtendedServicePhaseList,
        setSurchargeExtendedServicePhaseDistance = viewModel::setSurchargeExtendedServicePhaseDistance,
        setSurchargeExtendedServicePhasePercent = viewModel::setSurchargeExtendedServicePhasePercent,
        addServicePhase = viewModel::addSurchargeExtendedServicePhase,
        ndflValueState = uiState.ndfl,
        setNDFL = viewModel::setNDFL,
        isErrorInputNdfl = uiState.isErrorInputNdfl,
        unionistsRetentionState = uiState.unionistsRetentionState,
        setUnionistsRetention = viewModel::setUnionistsRetention,
        isErrorInputUnionistsRetention = uiState.isErrorInputUnionistsRetention,
        otherRetentionValueState = uiState.otherRetention,
        setOtherRetention = viewModel::setOtherRetention,
        isErrorInputOtherRetention = uiState.isErrorInputOtherRetention,
        welfarePercentState = uiState.welfarePercentState,
        setWelfarePercent = viewModel::setWelfarePercent,
        isErrorInputWelfarePercent = uiState.isErrorInputWelfarePercent,
        alimonyPercentState = uiState.alimonyPercentState,
        setAlimonyPercent = viewModel::setAlimonyPercent,
        isErrorInputAlimonyPercent = uiState.isErrorInputAlimonyPercent,
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
