@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.route.viewmodel

import com.z_company.domain.entities.DateSetTariffRate
import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Test
import kotlin.test.assertEquals

class OvertimePaymentCompositionTest {
    private val hour = 3_600_000L

    private fun instant(hour: Int): Long = instant(day = 5, hour = hour)

    private fun instant(day: Int, hour: Int): Long = LocalDateTime(2025, 1, day, hour, 0)
        .toInstant(TimeZone.of("GMT+3"))
        .toEpochMilliseconds()

    private fun helper(): SalaryCalculationHelper {
        val route = Route(
            basicData = BasicData(
                isOnePersonOperation = true,
                timeStartWork = instant(8),
                timeEndWork = instant(12),
            ),
            trains = mutableListOf(Train(number = "2503")),
        )
        return SalaryCalculationHelper(
            userSettings = UserSettings(
                selectMonthOfYear = MonthOfYear(
                    year = 2025,
                    month = 0,
                    tariffRate = 100.0,
                    days = emptyList(), // норма 0: все четыре часа сверхурочные
                ),
                timeZone = 0L,
            ),
            salarySetting = SalarySetting(
                onePersonOperationPercent = 40.0,
                zonalSurcharge = 0.0,
            ),
            allRoutes = listOf(route),
        )
    }

    @Test
    fun regularOvertimeLineContainsTariffOnlyAndDoesNotRepeatSurcharge() = runTest {
        val helper = helper()

        assertEquals(4 * hour, helper.getTimeOvertimeFlow().first())
        assertEquals(400.0, helper.getMoneyOvertimeFlow().first(), 0.001)
        assertEquals(160.0, helper.getMoneyOnePersonOperationFlow().first(), 0.001)
    }

    @Test
    fun overtimeLinesTogetherProduceOneAndHalfThenDoubleApplicableBase() = runTest {
        val helper = helper()

        val tariffPart = helper.getMoneyOvertimeFlow().first()
        val onePersonPart = helper.getMoneyOnePersonOperationFlow().first()
        val halfRatePart = helper.getMoneySurchargeOvertime05Flow().first()
        val fullRatePart = helper.getMoneySurchargeOvertimeFlow().first()

        assertEquals(140.0, halfRatePart, 0.001)
        assertEquals(280.0, fullRatePart, 0.001)
        assertEquals(980.0, tariffPart + onePersonPart + halfRatePart + fullRatePart, 0.001)
    }

    @Test
    fun overtimeTariffPartUsesRateOfLaterPeriodWhereOvertimeOccurred() = runTest {
        fun route(day: Int) = Route(
            basicData = BasicData(
                timeStartWork = instant(day, 8),
                timeEndWork = instant(day, 16),
            ),
        )
        val helper = SalaryCalculationHelper(
            userSettings = UserSettings(
                selectMonthOfYear = MonthOfYear(
                    year = 2025,
                    month = 0,
                    tariffRate = 200.0,
                    dateSetTariffRate = DateSetTariffRate(dateNewRate = 15, oldRate = 100.0),
                    days = (1..31).map { day ->
                        Day(
                            dayOfMonth = day,
                            tag = if (day == 1) TagForDay.WORKING_DAY else TagForDay.NON_WORKING_DAY,
                        )
                    },
                ),
                timeZone = 0L,
            ),
            salarySetting = SalarySetting(zonalSurcharge = 0.0),
            allRoutes = listOf(route(day = 5), route(day = 20)),
        )

        assertEquals(8 * hour, helper.getTimeOvertimeFlow().first())
        assertEquals(1_600.0, helper.getMoneyOvertimeFlow().first(), 0.001)
    }

    @Test
    fun overRestPaymentIsNotIncludedInOvertimeMultiplierBase() = runTest {
        val firstRoute = Route(
            basicData = BasicData(
                timeStartWork = instant(day = 5, hour = 8),
                timeEndWork = instant(day = 5, hour = 12),
                restPointOfTurnover = true,
            ),
        )
        val secondRoute = Route(
            basicData = BasicData(
                timeStartWork = instant(day = 5, hour = 18),
                timeEndWork = instant(day = 5, hour = 22),
            ),
        )
        val helper = SalaryCalculationHelper(
            userSettings = UserSettings(
                selectMonthOfYear = MonthOfYear(
                    year = 2025,
                    month = 0,
                    tariffRate = 100.0,
                    days = emptyList(),
                ),
                timeZone = 0L,
            ),
            salarySetting = SalarySetting(zonalSurcharge = 0.0),
            allRoutes = listOf(firstRoute, secondRoute),
        )

        assertEquals(2 * hour, helper.getOverRestTimeFlow().first())
        assertEquals(200.0 * (2.0 / 3.0), helper.getMoneyOverRestFlow().first(), 0.001)
        assertEquals(200.0, helper.getMoneySurchargeOvertime05Flow().first(), 0.001)
        assertEquals(400.0, helper.getMoneySurchargeOvertimeFlow().first(), 0.001)
    }
}
