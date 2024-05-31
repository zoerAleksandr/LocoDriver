package com.z_company.data_local.setting.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.z_company.data_local.setting.type_converter.NightTimeToPrimitiveConverter

@Entity
@TypeConverters(
    NightTimeToPrimitiveConverter::class
)
internal data class UserSettings(
    @PrimaryKey
    val settingsKey: String,
    val minTimeRest: Long?,
    val lastEnteredDieselCoefficient: Double,
    val nightTime: NightTime
)

data class NightTime(
    val startNightHour: Int = 22,
    val startNightMinute: Int = 0,
    val endNightHour: Int = 6,
    val endNightMinute: Int = 0
)
