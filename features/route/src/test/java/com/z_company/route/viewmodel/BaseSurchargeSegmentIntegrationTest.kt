@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.route.viewmodel

import com.z_company.domain.entities.DateSetTariffRate
import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Test
import kotlin.test.assertEquals

class BaseSurchargeSegmentIntegrationTest {
    private val hour = 3_600_000L
    private val moscow = TimeZone.of("GMT+3")

    private fun instant(day: Int, hour: Int): Long =
        LocalDateTime(2025, 1, day, hour, 0).toInstant(moscow).toEpochMilliseconds()

    private fun helper(route: Route): SalaryCalculationHelper {
        val month = MonthOfYear(
            year = 2025,
            month = 0,
            tariffRate = 200.0,
            dateSetTariffRate = DateSetTariffRate(dateNewRate = 11, oldRate = 100.0),
            days = (1..31).map { Day(it, TagForDay.WORKING_DAY) },
        )
        return SalaryCalculationHelper(
            userSettings = UserSettings(selectMonthOfYear = month, timeZone = 0L),
            salarySetting = SalarySetting(
                harmfulnessPercent = 12.0,
                surchargeQualificationClass = 10.0,
                zonalSurcharge = 25.0,
                otherSurcharge = 8.0,
            ),
            allRoutes = listOf(route),
        )
    }

    @Test
    fun harmfulnessAndZonalIncludePassengerButOtherSurchargesDoNot() = runTest {
        val route = Route(
            basicData = BasicData(
                timeStartWork = instant(day = 10, hour = 22),
                timeEndWork = instant(day = 11, hour = 3),
                timeStartBreak = instant(day = 10, hour = 23),
                timeEndBreak = instant(day = 11, hour = 0),
            ),
            passengers = mutableListOf(
                Passenger(
                    timeDeparture = instant(day = 11, hour = 0),
                    timeArrival = instant(day = 11, hour = 1),
                ),
            ),
        )
        val helper = helper(route)

        assertEquals(4 * hour, helper.getTimeHarmfulnessFlow().first())
        assertEquals(84.0, helper.getMoneyHarmfulnessFlow().first(), 0.001)
        assertEquals(50.0, helper.getMoneyAtQualificationClassFlow().first(), 0.001)
        assertEquals(175.0, helper.getMoneyZonalSurchargeFlow().first(), 0.001)
        assertEquals(40.0, helper.getMoneyOtherSurchargeFlow().first(), 0.001)
    }

    @Test
    fun harmfulnessAndZonalIncludePassengerBeforeArrivalBasedWorkStart() = runTest {
        val route = Route(
            basicData = BasicData(
                timeStartWork = instant(day = 10, hour = 12),
                timeEndWork = instant(day = 10, hour = 14),
            ),
            passengers = mutableListOf(
                Passenger(
                    timeDeparture = instant(day = 10, hour = 10),
                    timeArrival = instant(day = 10, hour = 12),
                    isWorkStartByArrival = true,
                ),
            ),
        )
        val helper = helper(route)

        assertEquals(4 * hour, helper.getTimeHarmfulnessFlow().first())
        assertEquals(48.0, helper.getMoneyHarmfulnessFlow().first(), 0.001)
        assertEquals(100.0, helper.getMoneyZonalSurchargeFlow().first(), 0.001)
    }

    @Test
    fun passengerBeforeWorkUsesTariffOfEachSideOfChangeBoundary() = runTest {
        val route = Route(
            basicData = BasicData(
                timeStartWork = instant(day = 11, hour = 1),
                timeEndWork = instant(day = 11, hour = 2),
            ),
            passengers = mutableListOf(
                Passenger(
                    timeDeparture = instant(day = 10, hour = 23),
                    timeArrival = instant(day = 11, hour = 1),
                    isWorkStartByArrival = true,
                ),
            ),
        )
        val helper = helper(route)

        assertEquals(300.0, helper.getMoneyAtPassengerOutsideWorkFlow().first(), 0.001)
        assertEquals(60.0, helper.getMoneyHarmfulnessFlow().first(), 0.001)
        assertEquals(125.0, helper.getMoneyZonalSurchargeFlow().first(), 0.001)
    }
}
