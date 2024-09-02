package com.z_company.data_local.setting.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.z_company.data_local.setting.type_converter.MonthOfYearToPrimitiveConverter
import com.z_company.data_local.setting.type_converter.NightTimeToPrimitiveConverter
import com.z_company.domain.entities.route.LocoType

@Entity
@TypeConverters(
    NightTimeToPrimitiveConverter::class,
    MonthOfYearToPrimitiveConverter::class
)
internal data class UserSettings(
    @PrimaryKey
    val settingsKey: String,
    var minTimeRest: Long,
    var minTimeHomeRest: Long,
    var lastEnteredDieselCoefficient: Double,
    var nightTime: NightTime,
    var updateAt: Long,
    var defaultLocoType: LocoType,
    var defaultWorkTime: Long,
    var usingDefaultWorkTime: Boolean,
    @ColumnInfo(defaultValue = "1")
    var isConsiderFutureRoute: Boolean,
    var monthOfYear: MonthOfYear
)

data class NightTime(
    var startNightHour: Int = 22,
    var startNightMinute: Int = 0,
    var endNightHour: Int = 6,
    var endNightMinute: Int = 0
)