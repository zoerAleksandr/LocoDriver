package com.z_company.data_local.setting.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class SalarySetting(
    @PrimaryKey
    val salarySettingKey: String,
    var tariffRate: Double,
    var zonalSurcharge: Double,
    var surchargeQualificationClass: Double,
    var surchargeExtendedServicePhase: Double,
    var surchargeHeavyLongDistanceTrains: Double,
    val otherSurcharge: Double,
    var ndfl: Double,
    val unionistsRetention: Double,
    var otherRetention: Double
)
