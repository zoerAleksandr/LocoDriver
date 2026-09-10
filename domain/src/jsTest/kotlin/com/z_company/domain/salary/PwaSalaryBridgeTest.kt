package com.z_company.domain.salary

import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.route.BasicData
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
}
