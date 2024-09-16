package com.z_company.route.viewmodel

import com.z_company.core.ResultState
import com.z_company.domain.entities.SalarySetting

data class SettingSalaryUIState(
    val saveSettingState: ResultState<Unit>? = null,
    val isEnableSaveButton: Boolean = true,
    val settingSalaryState: ResultState<SalarySetting?> = ResultState.Loading,
    val tariffRate: ResultState<String> = ResultState.Loading,
    val isErrorInputTariffRate: Boolean = false,
    val zonalSurcharge: ResultState<String> = ResultState.Loading,
    val isErrorInputZonalSurcharge: Boolean = false,
    val surchargeQualificationClass: ResultState<String> = ResultState.Loading,
    val isErrorInputSurchargeQualificationClass: Boolean = false,
    val surchargeExtendedServicePhase: ResultState<String> = ResultState.Loading,
    val isErrorInputSurchargeExtendedServicePhase: Boolean = false,
    val surchargeHeavyLongDistanceTrains: ResultState<String> = ResultState.Loading,
    val isErrorInputSurchargeHeavyLongDistanceTrains: Boolean = false,
    val otherSurchargeState: ResultState<String> = ResultState.Loading,
    val isErrorInputOtherSurcharge: Boolean = false,
    val ndfl: ResultState<String> = ResultState.Loading,
    val isErrorInputNdfl: Boolean = false,
    val unionistsRetentionState: ResultState<String> = ResultState.Loading,
    val isErrorInputUnionistsRetention: Boolean = false,
    val otherRetention: ResultState<String> = ResultState.Loading,
    val isErrorInputOtherRetention: Boolean = false
)
