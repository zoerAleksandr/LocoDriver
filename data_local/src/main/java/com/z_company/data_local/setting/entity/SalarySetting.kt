package com.z_company.data_local.setting.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.z_company.data_local.setting.type_converter.SurchargeExtendedServicePhaseToPrimitiveConverter

@Entity
@TypeConverters(
    SurchargeExtendedServicePhaseToPrimitiveConverter::class
)
internal data class SalarySetting(
    @PrimaryKey
    val salarySettingKey: String,
    var tariffRate: Double,
    var zonalSurcharge: Double,
    var surchargeQualificationClass: Double,
    var surchargeExtendedServicePhaseList: List<SurchargeExtendedServicePhase>,
    var surchargeHeavyLongDistanceTrains: Double,
    val otherSurcharge: Double,
    var ndfl: Double,
    val unionistsRetention: Double,
    var otherRetention: Double
)
@Entity
internal data class SurchargeExtendedServicePhase(
    @PrimaryKey
    val id: String,
    val distance: String = "",
    val percentSurcharge: String = ""
)
