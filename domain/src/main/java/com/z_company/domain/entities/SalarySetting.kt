package com.z_company.domain.entities

import java.util.UUID

const val SALARY_SETTINGS_KEY = "Salary_Setting_Key"
data class SalarySetting(
    val key: String = SALARY_SETTINGS_KEY,
    val tariffRate: Double = 0.0,
    val nightTimePercent: Double = 40.0,
    val averagePaymentHour: Double = 0.0,
    val districtCoefficient : Double = 0.0,
    val nordicPercent: Double = 0.0,
    val onePersonOperationPercent: Double = 0.0,
    val harmfulnessPercent: Double = 0.0,
    val percentLongDistanceTrain: Double = 0.0,
    val lengthLongDistanceTrain: Int = 0,
    val zonalSurcharge: Double = 25.0,
    val surchargeQualificationClass: Double = 0.0,
    var surchargeExtendedServicePhaseList: List<SurchargeExtendedServicePhase> = emptyList(),
    var surchargeHeavyTrainsList: List<SurchargeHeavyTrains> = emptyList(),
    val otherSurcharge: Double = 0.0,
    val ndfl: Double = 13.0,
    val unionistsRetention: Double = 1.0,
    val otherRetention: Double = 0.0
)

data class SurchargeExtendedServicePhase(
    val id: String = UUID.randomUUID().toString(),
    var distance: String = "",
    var percentSurcharge: String = ""
)

data class SurchargeHeavyTrains(
    val id: String = UUID.randomUUID().toString(),
    val weight: String = "",
    val percentSurcharge: String = ""
)
