package com.z_company.domain.entities

import com.z_company.domain.entities.route.LocoType
import java.util.Calendar
import java.util.UUID

const val SETTINGS_KEY = "User_Settings_Key"
val timestamp = Calendar.getInstance().timeInMillis

data class UserSettings(
    var key: String = SETTINGS_KEY,
    var minTimeRestPointOfTurnover: Long = 10_800_000L,
    var minTimeHomeRest: Long = 57_600_000L,
    var lastEnteredDieselCoefficient: Double = 0.83,
    var nightTime: NightTime = NightTime(),
    var defaultLocoType: LocoType = LocoType.ELECTRIC,
    var defaultWorkTime: Long = 43_200_000L,
    var usingDefaultWorkTime: Boolean = false,
    var isConsiderFutureRoute: Boolean = true,
    var updateAt: Long = timestamp,
    var selectMonthOfYear: MonthOfYear = MonthOfYear(),
    var isVisibleNightTime: Boolean = true,
    var isVisiblePassengerTime: Boolean = true,
    var isVisibleRelationTime: Boolean = true,
    var isVisibleHolidayTime: Boolean = true,
    var isVisibleExtendedServicePhase: Boolean = true,
    val stationList: List<String> = listOf(),
    val locomotiveSeriesList: List<String> = listOf(),
    val timeZone: Long = 0L,
    var servicePhases: List<ServicePhase> = listOf(),
    var dateTimePickerType: String = TypeDateTimePicker.WHEEL.text
)


enum class TypeDateTimePicker(val text: String) {
    ROUND("Круглые часы"), INPUT("Ввод цифр"), WHEEL("Барабан")
}

data class ServicePhase(
    val id: String = UUID.randomUUID().toString(),
    val departureStation: String,
    val arrivalStation: String,
    val distance: Int
): java.io.Serializable

data class NightTime(
    val startNightHour: Int = 22,
    val startNightMinute: Int = 0,
    val endNightHour: Int = 6,
    val endNightMinute: Int = 0
) {
    override fun toString(): String {
        val startNightHourText = if (this.startNightHour.toString().length == 1) {
            "0${this.startNightHour}"
        } else {
            "${this.startNightHour}"
        }
        val startNightMinuteText = if (this.startNightMinute.toString().length == 1) {
            "0${this.startNightMinute}"
        } else {
            "${this.startNightMinute}"
        }

        val endNightHourText = if (this.endNightHour.toString().length == 1) {
            "0${this.endNightHour}"
        } else {
            "${this.endNightHour}"
        }
        val endNightMinuteText = if (this.endNightMinute.toString().length == 1) {
            "0${this.endNightMinute}"
        } else {
            "${this.endNightMinute}"
        }


        return "$startNightHourText:$startNightMinuteText " +
                "- $endNightHourText:$endNightMinuteText"
    }
}
