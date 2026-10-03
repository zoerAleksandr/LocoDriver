@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.domain.entities.setting

import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.util.generateId
import kotlin.time.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val SETTINGS_KEY = "User_Settings_Key"
val timestamp = Clock.System.now().toEpochMilliseconds()
const val oneHourInMillis = 3_600_000
const val hourInMillis8 = oneHourInMillis * 8L
const val hourInMillis20 = oneHourInMillis * 20L

@Serializable
enum class CrossMonthTimezone { LOCAL, MOSCOW }

@Serializable
data class UserSettings(
    var key: String = SETTINGS_KEY,
    var minTimeRestPointOfTurnover: Long = 10_800_000L,
    val minTimeRestPointOfTurnoverSecond: Long = 14_400_000L,
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
    val isShowOnePersonSwitch: Boolean = true,
    val isShowLocomotive: Boolean = true,
    val isShowTrain: Boolean = true,
    val isShowPassenger: Boolean = true,
    val isShowOtherWork: Boolean = true,
    val otherWorkTypeList: List<String> = listOf(),
    val isShowPartner: Boolean = true,
    val isShowLocoHeating: Boolean = true,
    val isShowLocoAuxiliary: Boolean = true,
    val isConsiderLocoHeatingInTotal: Boolean = true,
    val isConsiderLocoAuxiliaryInTotal: Boolean = true,
    val isShowLocoStatistics: Boolean = true,
    val isShowLocoNorma: Boolean = true,
    val isShowOtherCurrent: Boolean = false,
    val country: String = "RU",
    val crossMonthTimezone: CrossMonthTimezone = CrossMonthTimezone.LOCAL,
    val useStandardTimePicker: Boolean = false,
    /**
     * Региональный код субъекта РФ (или другой страны) в формате ISO 3166-2:
     * "RU-TA" — Татарстан, "RU-BA" — Башкортостан, "RU-CR" — Крым и т.д.
     * null = используется только стандартный календарь по [country].
     */
    val region: String? = null,
    /** Синхронизируемая средняя длина пассажирского вагона для расчёта длины поезда. */
    val passengerWagonLengthMeters: Double = 24.5,
)

@Serializable
data class ServicePhase(
    val id: String = generateId(),
    val departureStation: String,
    val arrivalStation: String,
    val distance: Int,
    /** Стоимость линейного пробега по этому плечу, рублей за километр. */
    val linearMileageRate: Double = 0.0,
    /** Станции между крайними, по порядку от отправления к прибытию. */
    val intermediateStations: List<ServicePhaseStation> = emptyList(),
    /** Координаты станции отправления; null — неизвестны. */
    val departurePoint: ServicePhasePoint? = null,
    /** Координаты станции прибытия; null — неизвестны. */
    val arrivalPoint: ServicePhasePoint? = null,
    /**
     * «Измерено по карте»: строка «расстояние|подпись координат станций» (SCREEN_SPECS §6.6).
     * Чип «Измерено» показывается, если совпадает с текущими расстоянием и подписью; null — не измерено.
     */
    val measuredSignature: String? = null,
) {
    /**
     * Копия для рейса (`Train.servicePhase`): станции плеча в рейс не кладём —
     * они живут в справочнике и берутся оттуда по [id].
     */
    fun forTrain(): ServicePhase =
        copy(intermediateStations = emptyList(), departurePoint = null, arrivalPoint = null, measuredSignature = null)

    /**
     * Обратное плечо: станции в обратном порядке, км пересчитаны от нового начала
     * (`distance − km`; станции без км остаются без км).
     */
    fun reversed(): ServicePhase = ServicePhase(
        departureStation = arrivalStation,
        arrivalStation = departureStation,
        distance = distance,
        linearMileageRate = linearMileageRate,
        intermediateStations = intermediateStations.asReversed().map { st ->
            st.copy(
                id = generateId(),
                kmFromStart = st.kmFromStart?.let { km -> (distance - km).coerceAtLeast(0.0) },
            )
        },
        departurePoint = arrivalPoint,
        arrivalPoint = departurePoint,
    )
}

@Serializable
data class ServicePhasePoint(
    val latitude: Double,
    val longitude: Double,
    val osmId: Long? = null,
    /** Код ЕСР (6 цифр). */
    val esr: String? = null,
)

@Serializable
enum class ServicePhaseStationKind {
    @SerialName("station") STATION,
    /** Остановочный пункт (о.п.). */
    @SerialName("halt") HALT,
}

@Serializable
enum class ServicePhaseStationSource {
    @SerialName("manual") MANUAL,
    @SerialName("osm") OSM,
    @SerialName("map") MAP,
}

@Serializable
data class ServicePhaseStation(
    val id: String = generateId(),
    val name: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** Расстояние от станции отправления плеча по пути, км. */
    val kmFromStart: Double? = null,
    val kind: ServicePhaseStationKind = ServicePhaseStationKind.STATION,
    /** Код ЕСР (6 цифр). */
    val esr: String? = null,
    val osmId: Long? = null,
    val source: ServicePhaseStationSource = ServicePhaseStationSource.MANUAL,
) {
    val hasCoordinates: Boolean get() = latitude != null && longitude != null
}

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
