package com.z_company.data_local.setting.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.z_company.data_local.setting.type_converter.MonthOfYearToPrimitiveConverter
import com.z_company.data_local.setting.type_converter.NightTimeToPrimitiveConverter
import com.z_company.data_local.setting.type_converter.ServicePhaseToPrimitiveConverter
import com.z_company.data_local.setting.type_converter.StringListToPrimitiveConverter
import com.z_company.domain.entities.route.LocoType

@Entity
@TypeConverters(
    NightTimeToPrimitiveConverter::class,
    MonthOfYearToPrimitiveConverter::class,
    StringListToPrimitiveConverter::class,
    ServicePhaseToPrimitiveConverter::class
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
    var monthOfYear: MonthOfYear,
    @ColumnInfo(defaultValue = "1")
    var isVisibleNightTime: Boolean,
    @ColumnInfo(defaultValue = "1")
    var isVisiblePassengerTime: Boolean,
    @ColumnInfo(defaultValue = "1")
    var isVisibleRelationTime: Boolean,
    @ColumnInfo(defaultValue = "1")
    var isVisibleHolidayTime: Boolean,
    @ColumnInfo(defaultValue = "1")
    var isVisibleExtendedServicePhase: Boolean,
    @ColumnInfo(defaultValue = "[]")
    val stationList: List<String> = listOf(),
    @ColumnInfo(defaultValue = "0")
    val timeZone: Long,
    @ColumnInfo(defaultValue = "[]")
    val locomotiveSeriesList: List<String> = listOf(),
    @ColumnInfo(defaultValue = "[]")
    val servicePhases: List<ServicePhase> = listOf(),
    @ColumnInfo(defaultValue = "Барабан")
    var dateTimePickerType: String
)

data class ServicePhase(
    val id: String,
    val departureStation: String,
    val arrivalStation: String,
    val distance: Int
)

data class NightTime(
    var startNightHour: Int = 22,
    var startNightMinute: Int = 0,
    var endNightHour: Int = 6,
    var endNightMinute: Int = 0
)