package com.z_company.domain.entities

import java.util.UUID

const val SALARY_SETTINGS_KEY = "Salary_Setting_Key"
data class SalarySetting(
    val key: String = SALARY_SETTINGS_KEY,
    val tariffRate: Double = 0.0,
    val zonalSurcharge: Double = 25.0,
    val surchargeQualificationClass: Double = 0.0,
    var surchargeExtendedServicePhaseList: List<SurchargeExtendedServicePhase> = emptyList(),
    val surchargeHeavyLongDistanceTrains: Double = 15.0,
    val otherSurcharge: Double = 0.0,
    val ndfl: Double = 13.0,
    val unionistsRetention: Double = 1.0,
    val otherRetention: Double = 0.0
)

data class SurchargeExtendedServicePhase(
    val id: String = UUID.randomUUID().toString(),
    var distance: Int = 0,
    var percentSurcharge: Double = 0.0
)
