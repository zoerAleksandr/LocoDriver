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
    var minTimeRest: Long?,
    var lastEnteredDieselCoefficient: Double,
    var nightTime: NightTime,
    var updateAt: Long,
)

data class NightTime(
    var startNightHour: Int = 22,
    var startNightMinute: Int = 0,
    var endNightHour: Int = 6,
    var endNightMinute: Int = 0
)