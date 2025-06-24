package com.z_company.route.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.SalarySetting
import com.z_company.domain.entities.SurchargeExtendedServicePhase
import com.z_company.domain.entities.SurchargeHeavyTrains

data class SettingSalaryUIState(
    val currentMonthOfYear: MonthOfYear? = null,
    val saveSettingState: ResultState<Unit>? = null,
    val isEnableSaveButton: Boolean = true,
    val settingSalaryState: ResultState<SalarySetting?> = ResultState.Loading(),
    val tariffRate: ResultState<String> = ResultState.Loading(),
    val isErrorInputTariffRate: Boolean = false,
    val oldTariffRate: ResultState<String> = ResultState.Loading(),
    val isErrorInputOldTariffRate: Boolean = false,
    val averagePaymentHour: ResultState<String> = ResultState.Loading(),
    val isErrorInputAveragePayment: Boolean = false,
    val districtCoefficient: ResultState<String> = ResultState.Loading(),
    val isErrorInputDistrictCoefficient: Boolean = false,
    val nordicCoefficient: ResultState<String> = ResultState.Loading(),
    val isErrorInputNordicCoefficient: Boolean = false,
    val zonalSurcharge: ResultState<String> = ResultState.Loading(),
    val isErrorInputZonalSurcharge: Boolean = false,
    val surchargeQualificationClass: ResultState<String> = ResultState.Loading(),
    val isErrorInputSurchargeQualificationClass: Boolean = false,
    val onePersonOperationPercent: ResultState<String> = ResultState.Loading(),
    val isErrorInputOnePersonOperation: Boolean = false,
    val harmfulnessPercent: ResultState<String> = ResultState.Loading(),
    val isErrorInputHarmfulnessPercent: Boolean = false,
    val longDistanceTrainPercent: ResultState<String> = ResultState.Loading(),
    val isErrorInputLongDistanceTrainPercent: Boolean = false,
    val lengthLongDistanceTrain: ResultState<String> = ResultState.Loading(),
    val isErrorInputLengthLongDistanceTrain: Boolean = false,
    val surchargeHeavyTrain: SnapshotStateList<SurchargeHeavyTrains> = mutableStateListOf(SurchargeHeavyTrains()),
    val isErrorInputSurchargeHeavyTrain: Boolean = false,
    val surchargeExtendedServicePhaseList: SnapshotStateList<SurchargeExtendedServicePhase> = mutableStateListOf(SurchargeExtendedServicePhase()),
    val isErrorInputSurchargeExtendedServicePhase: Boolean = false,
    val otherSurchargeState: ResultState<String> = ResultState.Loading(),
    val isErrorInputOtherSurcharge: Boolean = false,
    val ndfl: ResultState<String> = ResultState.Loading(),
    val isErrorInputNdfl: Boolean = false,
    val unionistsRetentionState: ResultState<String> = ResultState.Loading(),
    val isErrorInputUnionistsRetention: Boolean = false,
    val otherRetention: ResultState<String> = ResultState.Loading(),
    val isErrorInputOtherRetention: Boolean = false,
    val isShowDialogChangeTariffRate: Boolean = false,
)
