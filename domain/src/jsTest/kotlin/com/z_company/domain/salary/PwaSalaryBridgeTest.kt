package com.z_company.domain.salary

import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.WorkScheduleProfile
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Route
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class PwaSalaryBridgeTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun `catalog exports every reference without photo source`() {
        val catalog = json.parseToJsonElement(PwaSalaryBridge.catalogJson()).jsonArray

        assertEquals(286, catalog.size)
        assertFalse(catalog.first().jsonObject.containsKey("source"))
        assertTrue(catalog.all { it.jsonObject.keys.containsAll(listOf("code", "shortName", "description", "type")) })
    }

    @Test
    fun `empty web request uses shared calculator and returns zero totals`() = runTest {
        val request = buildJsonObject {
            put("userSettings", json.encodeToJsonElement(UserSettings.serializer(), UserSettings()))
            put("salarySetting", json.encodeToJsonElement(SalarySetting.serializer(), SalarySetting()))
            putJsonArray("routes") {}
            put("effectiveNormaHours", 0)
            put("annualOvertimeBeforePeriod", 0L)
        }

        val result = json.parseToJsonElement(PwaSalaryBridge.calculate(request.toString()).await()).jsonObject

        assertEquals(0.0, result.getValue("totalAccrued").jsonPrimitive.double)
        assertEquals(0.0, result.getValue("totalDeducted").jsonPrimitive.double)
        assertEquals(0.0, result.getValue("payable").jsonPrimitive.double)
        assertEquals(0, result.getValue("accruals").jsonArray.size)
        assertEquals(0, result.getValue("deductions").jsonArray.size)
    }

    @Test
    fun `web result matches shared calculator for a worked route`() = runTest {
        val start = LocalDateTime(2025, 1, 10, 8, 0).toInstant(TimeZone.of("GMT+3")).toEpochMilliseconds()
        val end = LocalDateTime(2025, 1, 10, 12, 0).toInstant(TimeZone.of("GMT+3")).toEpochMilliseconds()
        val userSettings = UserSettings(
            selectMonthOfYear = MonthOfYear(
                year = 2025,
                month = 0,
                tariffRate = 100.0,
                days = (1..31).map { Day(it, TagForDay.WORKING_DAY) },
            ),
            timeZone = 0L,
        )
        val salarySetting = SalarySetting(
            nightTimePercent = 0.0,
            harmfulnessPercent = 4.0,
            zonalSurcharge = 25.0,
            surchargeQualificationClass = 10.0,
            districtCoefficient = 0.0,
            nordicPercent = 0.0,
            ndfl = 13.0,
            surchargeHeavyTrainsList = emptyList(),
            surchargeLongTrainsList = emptyList(),
        )
        val route = Route(basicData = BasicData(timeStartWork = start, timeEndWork = end))
        val shared = SalaryCalculationHelper(userSettings, salarySetting, listOf(route))
        val request = buildJsonObject {
            put("userSettings", json.encodeToJsonElement(UserSettings.serializer(), userSettings))
            put("salarySetting", json.encodeToJsonElement(SalarySetting.serializer(), salarySetting))
            putJsonArray("routes") { add(json.encodeToJsonElement(Route.serializer(), route)) }
            put("effectiveNormaHours", 0)
            put("annualOvertimeBeforePeriod", 0L)
        }

        val result = json.parseToJsonElement(PwaSalaryBridge.calculate(request.toString()).await()).jsonObject

        assertEquals(shared.getMoneyTotalChargedFlow().first(), result.getValue("totalAccrued").jsonPrimitive.double, 0.001)
        assertEquals(shared.getMoneyTotalRetentionFlow().first(), result.getValue("totalDeducted").jsonPrimitive.double, 0.001)
        assertEquals(shared.getMoneyToBeCredited().first(), result.getValue("payable").jsonPrimitive.double, 0.001)
        assertEquals(shared.getTotalWorkTimeWithCommute().first().toDouble(), result.getValue("totalWorkedMillis").jsonPrimitive.double)
        assertTrue(result.getValue("accruals").jsonArray.any { it.jsonObject.getValue("id").jsonPrimitive.content == "TARIFF" })
        assertTrue(result.getValue("deductions").jsonArray.any { it.jsonObject.getValue("id").jsonPrimitive.content == "NDFL" })
    }

    @Test
    fun `web request applies work schedule profile to monthly norma`() = runTest {
        // 10.01.2025 — пятница. Единственный день месяца в календаре: стандартная
        // норма 8 ч, по личному графику (пятница 2 ч) — 2 ч → 4-часовая смена
        // даёт 2 ч сверхурочных только с профилем.
        val tz = TimeZone.of("GMT+3")
        val start = LocalDateTime(2025, 1, 10, 8, 0).toInstant(tz).toEpochMilliseconds()
        val end = LocalDateTime(2025, 1, 10, 12, 0).toInstant(tz).toEpochMilliseconds()
        val userSettings = UserSettings(
            selectMonthOfYear = MonthOfYear(
                year = 2025,
                month = 0,
                tariffRate = 100.0,
                days = listOf(Day(10, TagForDay.WORKING_DAY)),
            ),
            timeZone = 0L,
        )
        val salarySetting = SalarySetting(
            nightTimePercent = 0.0,
            harmfulnessPercent = 0.0,
            zonalSurcharge = 0.0,
            surchargeHeavyTrainsList = emptyList(),
            surchargeLongTrainsList = emptyList(),
        )
        val route = Route(basicData = BasicData(timeStartWork = start, timeEndWork = end))
        val profile = WorkScheduleProfile.standard().withHours(DayOfWeek.FRIDAY, 2)
        fun request(withProfile: Boolean) = buildJsonObject {
            put("userSettings", json.encodeToJsonElement(UserSettings.serializer(), userSettings))
            put("salarySetting", json.encodeToJsonElement(SalarySetting.serializer(), salarySetting))
            putJsonArray("routes") { add(json.encodeToJsonElement(Route.serializer(), route)) }
            put("effectiveNormaHours", 0)
            put("annualOvertimeBeforePeriod", 0L)
            if (withProfile) put("workScheduleProfile", json.encodeToJsonElement(WorkScheduleProfile.serializer(), profile))
        }
        fun overtimeMillis(result: String): Long = json.parseToJsonElement(result).jsonObject
            .getValue("accruals").jsonArray
            .filter { it.jsonObject.getValue("id").jsonPrimitive.content.startsWith("OVERTIME") }
            .maxOfOrNull { it.jsonObject.getValue("hoursMillis").jsonPrimitive.content.toLong() } ?: 0L

        assertEquals(0L, overtimeMillis(PwaSalaryBridge.calculate(request(withProfile = false).toString()).await()))
        val withProfile = PwaSalaryBridge.calculate(request(withProfile = true).toString()).await()
        assertEquals(2 * 3_600_000L, overtimeMillis(withProfile))
        // Сверхурочные месяца отдаются и отдельным полем — для годового порога в PWA.
        assertEquals(2 * 3_600_000L, json.parseToJsonElement(withProfile).jsonObject.getValue("overtimeMillis").jsonPrimitive.content.toLong())
    }

    @Test
    fun `trip result matches shared calculator components for a worked route`() = runTest {
        val tz = TimeZone.of("GMT+3")
        val start = LocalDateTime(2025, 1, 10, 8, 0).toInstant(tz).toEpochMilliseconds()
        val end = LocalDateTime(2025, 1, 10, 12, 0).toInstant(tz).toEpochMilliseconds()
        val userSettings = UserSettings(
            selectMonthOfYear = MonthOfYear(
                year = 2025,
                month = 0,
                tariffRate = 100.0,
                days = (1..31).map { Day(it, TagForDay.WORKING_DAY) },
            ),
            timeZone = 0L,
        )
        val salarySetting = SalarySetting(
            nightTimePercent = 0.0,
            harmfulnessPercent = 4.0,
            zonalSurcharge = 25.0,
            surchargeQualificationClass = 10.0,
            districtCoefficient = 0.0,
            nordicPercent = 0.0,
            surchargeHeavyTrainsList = emptyList(),
            surchargeLongTrainsList = emptyList(),
        )
        val route = Route(basicData = BasicData(timeStartWork = start, timeEndWork = end))
        val shared = SalaryCalculationHelper(userSettings, salarySetting, listOf(route))
        val request = buildJsonObject {
            put("userSettings", json.encodeToJsonElement(UserSettings.serializer(), userSettings))
            put("salarySetting", json.encodeToJsonElement(SalarySetting.serializer(), salarySetting))
            put("route", json.encodeToJsonElement(Route.serializer(), route))
        }

        val result = json.parseToJsonElement(PwaSalaryBridge.calculateTrip(request.toString()).await()).jsonObject
        val rows = result.getValue("rows").jsonArray.associate {
            it.jsonObject.getValue("id").jsonPrimitive.content to it.jsonObject.getValue("amount").jsonPrimitive.double
        }

        assertTrue(result.getValue("isCalculated").jsonPrimitive.content.toBoolean())
        assertTrue(result.getValue("isSetTariffRate").jsonPrimitive.content.toBoolean())
        assertEquals(shared.getMoneyAtWorkTimeAtTariffSingleRoute().first(), rows.getValue("TARIFF"), 0.001)
        assertEquals(shared.getMoneyZonalSurchargeFlow().first(), rows.getValue("ZONAL"), 0.001)
        assertEquals(
            shared.getMoneyAtQualificationClassFlow().first() + shared.getMoneyHarmfulnessFlow().first(),
            rows.getValue("OTHER_SURCHARGE"),
            0.001,
        )
        assertEquals(rows.values.sum(), result.getValue("totalPayment").jsonPrimitive.double, 0.001)
        assertEquals(4 * 3_600_000.0, result.getValue("workTimeMillis").jsonPrimitive.double)
    }

    @Test
    fun `trip result pays over-rest after previous turnover rest`() = runTest {
        val tz = TimeZone.of("GMT+3")
        fun at(day: Int, hour: Int) = LocalDateTime(2025, 1, day, hour, 0).toInstant(tz).toEpochMilliseconds()
        val userSettings = UserSettings(
            selectMonthOfYear = MonthOfYear(
                year = 2025,
                month = 0,
                tariffRate = 100.0,
                days = (1..31).map { Day(it, TagForDay.WORKING_DAY) },
            ),
            timeZone = 0L,
            minTimeRestPointOfTurnover = 3 * 3_600_000L,
        )
        val salarySetting = SalarySetting(zonalSurcharge = 0.0, nightTimePercent = 0.0, harmfulnessPercent = 0.0)
        // Предыдущий: работа 4 ч, отдых в ПО → оплачиваемый переотдых с 16:00 + 4 ч = 20:00.
        val previous = Route(basicData = BasicData(id = "prev", timeStartWork = at(9, 12), timeEndWork = at(9, 16), restPointOfTurnover = true))
        // Явка в 22:00 → переотдых 2 ч × 100 × 2/3.
        val route = Route(basicData = BasicData(id = "cur", timeStartWork = at(9, 22), timeEndWork = at(10, 2)))
        val request = buildJsonObject {
            put("userSettings", json.encodeToJsonElement(UserSettings.serializer(), userSettings))
            put("salarySetting", json.encodeToJsonElement(SalarySetting.serializer(), salarySetting))
            put("route", json.encodeToJsonElement(Route.serializer(), route))
            put("previousRoute", json.encodeToJsonElement(Route.serializer(), previous))
        }

        val result = json.parseToJsonElement(PwaSalaryBridge.calculateTrip(request.toString()).await()).jsonObject
        val overRest = result.getValue("rows").jsonArray.first { it.jsonObject.getValue("id").jsonPrimitive.content == "OVER_REST" }.jsonObject

        assertEquals(2 * 3_600_000.0, overRest.getValue("hoursMillis").jsonPrimitive.double)
        assertEquals(2 * 100.0 * 2.0 / 3.0, overRest.getValue("amount").jsonPrimitive.double, 0.001)
    }

    @Test
    fun `trip result pays over-rest across month boundary from candidate routes`() = runTest {
        // Явка 1 мая после отдыха в ПО 30 апреля: previousRoute выбирается из
        // candidateRoutes тем же правилом, что в Android FormViewModel
        // (ближайшая явка раньше явки маршрута, без привязки к месяцу).
        val tz = TimeZone.of("GMT+3")
        fun at(month: Int, day: Int, hour: Int) = LocalDateTime(2025, month, day, hour, 0).toInstant(tz).toEpochMilliseconds()
        val userSettings = UserSettings(
            selectMonthOfYear = MonthOfYear(
                year = 2025,
                month = 4,
                tariffRate = 100.0,
                days = (1..31).map { Day(it, TagForDay.WORKING_DAY) },
            ),
            timeZone = 0L,
            minTimeRestPointOfTurnover = 3 * 3_600_000L,
        )
        val salarySetting = SalarySetting(zonalSurcharge = 0.0, nightTimePercent = 0.0, harmfulnessPercent = 0.0)
        // Работа 4 ч, отдых в ПО → оплачиваемый переотдых с 19:00 + 4 ч = 23:00 30 апреля.
        val lastOfApril = Route(basicData = BasicData(id = "apr-last", timeStartWork = at(4, 30, 15), timeEndWork = at(4, 30, 19), restPointOfTurnover = true))
        val earlierInApril = Route(basicData = BasicData(id = "apr-early", timeStartWork = at(4, 10, 8), timeEndWork = at(4, 10, 12)))
        // Явка 1 мая 03:00 → переотдых 4 ч (1 ч в апреле + 3 ч в мае) × 100 × 2/3.
        val route = Route(basicData = BasicData(id = "may-first", timeStartWork = at(5, 1, 3), timeEndWork = at(5, 1, 9)))
        val request = buildJsonObject {
            put("userSettings", json.encodeToJsonElement(UserSettings.serializer(), userSettings))
            put("salarySetting", json.encodeToJsonElement(SalarySetting.serializer(), salarySetting))
            put("route", json.encodeToJsonElement(Route.serializer(), route))
            putJsonArray("candidateRoutes") {
                add(json.encodeToJsonElement(Route.serializer(), earlierInApril))
                add(json.encodeToJsonElement(Route.serializer(), route))
                add(json.encodeToJsonElement(Route.serializer(), lastOfApril))
            }
        }

        val result = json.parseToJsonElement(PwaSalaryBridge.calculateTrip(request.toString()).await()).jsonObject
        val overRest = result.getValue("rows").jsonArray.first { it.jsonObject.getValue("id").jsonPrimitive.content == "OVER_REST" }.jsonObject

        assertEquals(4 * 3_600_000.0, overRest.getValue("hoursMillis").jsonPrimitive.double)
        assertEquals(4 * 100.0 * 2.0 / 3.0, overRest.getValue("amount").jsonPrimitive.double, 0.001)

        // Месячный расчёт с соседями: части по месяцам в сумме дают переотдых поездки.
        fun monthRequest(month0: Int, routes: List<Route>, adjacent: List<Route>) = buildJsonObject {
            put(
                "userSettings",
                json.encodeToJsonElement(
                    UserSettings.serializer(),
                    userSettings.copy(selectMonthOfYear = userSettings.selectMonthOfYear.copy(month = month0)),
                ),
            )
            put("salarySetting", json.encodeToJsonElement(SalarySetting.serializer(), salarySetting))
            putJsonArray("routes") { routes.forEach { add(json.encodeToJsonElement(Route.serializer(), it)) } }
            putJsonArray("adjacentRoutes") { adjacent.forEach { add(json.encodeToJsonElement(Route.serializer(), it)) } }
        }
        fun overRestOf(resultJson: String): Pair<Double, Double> {
            val line = json.parseToJsonElement(resultJson).jsonObject.getValue("accruals").jsonArray
                .map { it.jsonObject }
                .firstOrNull { it.getValue("id").jsonPrimitive.content == "EXCESS_REST" }
                ?: return 0.0 to 0.0
            return line.getValue("hoursMillis").jsonPrimitive.double to line.getValue("amount").jsonPrimitive.double
        }
        val april = overRestOf(PwaSalaryBridge.calculate(monthRequest(3, listOf(earlierInApril, lastOfApril), listOf(route)).toString()).await())
        val may = overRestOf(PwaSalaryBridge.calculate(monthRequest(4, listOf(route), listOf(lastOfApril)).toString()).await())

        assertEquals(1 * 3_600_000.0, april.first)
        assertEquals(3 * 3_600_000.0, may.first)
        assertEquals(overRest.getValue("amount").jsonPrimitive.double, april.second + may.second, 0.001)
    }

    @Test
    fun `web result exposes passenger waiting line 018M`() = runTest {
        val tz = TimeZone.of("GMT+3")
        fun at(day: Int, hour: Int) = LocalDateTime(2025, 1, day, hour, 0).toInstant(tz).toEpochMilliseconds()
        // Сдал локомотив (22:00) → ждал час → поехал пассажиром (23:00–00:00).
        val route = Route(
            basicData = BasicData(timeStartWork = at(10, 20), timeEndWork = at(11, 0)),
            locomotives = mutableListOf(
                Locomotive(basicId = "", timeStartOfAcceptance = at(10, 20), timeEndOfDelivery = at(10, 22)),
            ),
            passengers = mutableListOf(Passenger(timeDeparture = at(10, 23), timeArrival = at(11, 0))),
        )
        val userSettings = UserSettings(
            selectMonthOfYear = MonthOfYear(
                year = 2025,
                month = 0,
                tariffRate = 100.0,
                days = (1..31).map { Day(it, TagForDay.WORKING_DAY) },
            ),
            timeZone = 0L,
        )
        val salarySetting = SalarySetting(zonalSurcharge = 10.0, harmfulnessPercent = 4.0)
        val shared = SalaryCalculationHelper(userSettings, salarySetting, listOf(route))
        val request = buildJsonObject {
            put("userSettings", json.encodeToJsonElement(UserSettings.serializer(), userSettings))
            put("salarySetting", json.encodeToJsonElement(SalarySetting.serializer(), salarySetting))
            putJsonArray("routes") { add(json.encodeToJsonElement(Route.serializer(), route)) }
            put("effectiveNormaHours", 0)
            put("annualOvertimeBeforePeriod", 0L)
        }

        val result = json.parseToJsonElement(PwaSalaryBridge.calculate(request.toString()).await()).jsonObject
        val waiting = result.getValue("accruals").jsonArray
            .map { it.jsonObject }
            .first { it.getValue("id").jsonPrimitive.content == "PASSENGER_WAITING" }

        assertEquals("018M", waiting.getValue("code").jsonPrimitive.content)
        assertEquals(3_600_000.0, waiting.getValue("hoursMillis").jsonPrimitive.double)
        assertEquals(
            shared.getMoneyAtPassengerWaitingFlow().first(),
            waiting.getValue("amount").jsonPrimitive.double,
            0.001,
        )
        assertEquals(100.0, waiting.getValue("amount").jsonPrimitive.double, 0.001)
    }
}
