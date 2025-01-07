package com.z_company.data_local.setting.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.z_company.data_local.setting.type_converter.SurchargeExtendedServicePhaseToPrimitiveConverter
import com.z_company.data_local.setting.type_converter.SurchargeHeavyTrainsToPrimitiveConverter

@Entity
@TypeConverters(
    SurchargeExtendedServicePhaseToPrimitiveConverter::class,
    SurchargeHeavyTrainsToPrimitiveConverter::class
)
internal data class SalarySetting(
    @PrimaryKey
    val salarySettingKey: String,
    var tariffRate: Double,
    var zonalSurcharge: Double,
    @ColumnInfo(defaultValue = "0.0")
    val averagePaymentHour: Double,
    @ColumnInfo(defaultValue = "0.0")
    val districtCoefficient : Double,
    @ColumnInfo(defaultValue = "0.0")
    val nordicCoefficient: Double,
    @ColumnInfo(defaultValue = "0.0")
    val onePersonOperationPercent: Double,
    @ColumnInfo(defaultValue = "0.0")
    val harmfulnessPercent: Double,
    var surchargeQualificationClass: Double,
    var surchargeExtendedServicePhaseList: List<SurchargeExtendedServicePhase>,
    @ColumnInfo(defaultValue = "[]")
    val surchargeHeavyTrainsList: List<SurchargeHeavyTrains>,
    @ColumnInfo(defaultValue = "0.0")
    val surchargeLongTrain: Double,
//    var surchargeHeavyLongDistanceTrains: Double,
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

@Entity
internal data class SurchargeHeavyTrains(
    @PrimaryKey
    val id: String,
    val weight: String = "",
    val percentSurcharge: String = ""
)
