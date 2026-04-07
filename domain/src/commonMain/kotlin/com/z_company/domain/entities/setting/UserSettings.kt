@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.domain.entities.setting

import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.util.generateId
import kotlin.time.Clock
import kotlinx.serialization.Serializable

const val SETTINGS_KEY = "User_Settings_Key"
val timestamp = Clock.System.now().toEpochMilliseconds()
const val oneHourInMillis = 3_600_000
const val hourInMillis8 = oneHourInMillis * 8L
const val hourInMillis20 = oneHourInMillis * 20L

@Serializable
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
    val stationList: List<String> = listOf(),
    val locomotiveSeriesList: List<String> = listOf(),
    val timeZone: Long = 0L,
    var servicePhases: List<ServicePhase> = listOf(),
    val standardTimesStartWork: List<Long> = listOf(hourInMillis8, hourInMillis20),
    val subscriptionPeriod: Long = 0,
    val isDecimalTime: Boolean = false,
    val isShowBreak: Boolean = true,
    val isShowLocoHeating: Boolean = true,
    val isShowLocoAuxiliary: Boolean = true,
    val isShowLocoStatistics: Boolean = true,
    val isShowLocoNorma: Boolean = true,
    val isShowOtherCurrent: Boolean = false,
    val country: String = "RU",
)

@Serializable
data class ServicePhase(
    val id: String = generateId(),
    val departureStation: String,
    val arrivalStation: String,
    val distance: Int
)

@Serializable
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
