@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.route.viewmodel

import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.ReleaseType
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.route.BasicData
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BusinessTripBoundaryCalculationTest {
    private val hour = 3_600_000L
    private val moscow = TimeZone.of("Europe/Moscow")

    private fun instant(day: Int, hour: Int): Long =
        LocalDateTime(2025, 1, day, hour, 0).toInstant(moscow).toEpochMilliseconds()

    private fun helper(route: Route, businessDays: Set<Int>): SalaryCalculationHelper {
        val days = (1..31).map { day ->
            Day(
                dayOfMonth = day,
                tag = TagForDay.WORKING_DAY,
                isReleaseDay = day in businessDays,
                releaseType = ReleaseType.BusinessTrip.takeIf { day in businessDays },
            )
        }
        return SalaryCalculationHelper(
            userSettings = UserSettings(
                selectMonthOfYear = MonthOfYear(
                    year = 2025,
                    month = 0,
                    tariffRate = 100.0,
                    days = days,
                ),
                timeZone = 0L,
            ),
            salarySetting = SalarySetting(
                averagePaymentHour = 200.0,
                harmfulnessPercent = 10.0,
            ),
            allRoutes = listOf(route),
        )
    }

    @Test
    fun routeStartingOutsideTripIsSplitAtTripMidnight() = runTest {
        val route = Route(basicData = BasicData(
            timeStartWork = instant(day = 10, hour = 22),
            timeEndWork = instant(day = 11, hour = 4),
            timeStartBreak = instant(day = 11, hour = 0),
            timeEndBreak = instant(day = 11, hour = 1),
        ))
        val calculation = helper(route, businessDays = setOf(11))

        assertTrue(calculation.hasBusinessTripRoutes())
        assertFalse(calculation.isEntirelyBusinessTrip())
        assertEquals(3 * hour, calculation.getBusinessTripTimeFlow().first())
        assertEquals(600.0, calculation.getMoneyBusinessTripFlow().first(), 0.001)
        assertEquals(2 * hour, calculation.getTotalWorkTime().first())
        assertEquals(20.0, calculation.getMoneyHarmfulnessFlow().first(), 0.001)
    }

    @Test
    fun routeStartingInTripKeepsRegularPartAfterTripMidnight() = runTest {
        val route = Route(basicData = BasicData(
            timeStartWork = instant(day = 10, hour = 22),
            timeEndWork = instant(day = 11, hour = 3),
        ))
        val calculation = helper(route, businessDays = setOf(10))

        assertEquals(2 * hour, calculation.getBusinessTripTimeFlow().first())
        assertEquals(3 * hour, calculation.getTotalWorkTime().first())
        assertEquals(30.0, calculation.getMoneyHarmfulnessFlow().first(), 0.001)
    }

    @Test
    fun routeWithoutTripDayRemainsEntirelyRegular() = runTest {
        val route = Route(basicData = BasicData(
            timeStartWork = instant(day = 10, hour = 8),
            timeEndWork = instant(day = 10, hour = 12),
        ))
        val calculation = helper(route, businessDays = emptySet())

        assertFalse(calculation.hasBusinessTripRoutes())
        assertEquals(0L, calculation.getBusinessTripTimeFlow().first())
        assertEquals(4 * hour, calculation.getTotalWorkTime().first())
    }

    @Test
    fun routeInsideTripDayIsEntirelyBusinessTrip() = runTest {
        val route = Route(basicData = BasicData(
            timeStartWork = instant(day = 10, hour = 8),
            timeEndWork = instant(day = 10, hour = 12),
        ))
        val calculation = helper(route, businessDays = setOf(10))

        assertTrue(calculation.isEntirelyBusinessTrip())
        assertEquals(4 * hour, calculation.getBusinessTripTimeFlow().first())
        assertEquals(0L, calculation.getTotalWorkTime().first())
        assertEquals(800.0, calculation.getMoneyTotalChargedFlow().first(), 0.001)
    }
}
