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

class NightSegmentSalaryIntegrationTest {
    private val hour = 3_600_000L
    private val moscow = TimeZone.of("GMT+3")

    private fun instant(day: Int, hour: Int): Long =
        LocalDateTime(2025, 1, day, hour, 0).toInstant(moscow).toEpochMilliseconds()

    private fun helper(route: Route, holidayDay: Int? = null): SalaryCalculationHelper {
        val month = MonthOfYear(
            year = 2025,
            month = 0,
            tariffRate = 200.0,
            dateSetTariffRate = DateSetTariffRate(dateNewRate = 11, oldRate = 100.0),
            days = (1..31).map { day ->
                Day(day, if (day == holidayDay) TagForDay.HOLIDAY else TagForDay.WORKING_DAY)
            },
        )
        return SalaryCalculationHelper(
            userSettings = UserSettings(selectMonthOfYear = month, timeZone = 0L),
            salarySetting = SalarySetting(nightTimePercent = 40.0),
            allRoutes = listOf(route),
        )
    }

    @Test
    fun nightMoneySplitsAtTariffMidnightAndExcludesBreak() = runTest {
        val route = Route(
            basicData = BasicData(
                timeStartWork = instant(day = 10, hour = 20),
                timeEndWork = instant(day = 11, hour = 4),
                timeStartBreak = instant(day = 10, hour = 23),
                timeEndBreak = instant(day = 11, hour = 0),
            ),
        )

        val helper = helper(route)

        assertEquals(5 * hour, helper.getNightTimeFlow().first())
        assertEquals(360.0, helper.getMoneyAtNightTimeFlow().first(), 0.001)
    }

    @Test
    fun routeOutsideNightWindowHasNoNightAccrual() = runTest {
        val route = Route(
            basicData = BasicData(
                timeStartWork = instant(day = 10, hour = 8),
                timeEndWork = instant(day = 10, hour = 16),
            ),
        )

        assertEquals(0.0, helper(route).getMoneyAtNightTimeFlow().first(), 0.001)
    }

    @Test
    fun passengerMoneySplitsAtTariffMidnightAndExcludesBreak() = runTest {
        val route = Route(
            basicData = BasicData(
                timeStartWork = instant(day = 10, hour = 20),
                timeEndWork = instant(day = 11, hour = 4),
                timeStartBreak = instant(day = 10, hour = 23),
                timeEndBreak = instant(day = 11, hour = 0),
            ),
            passengers = mutableListOf(
                Passenger(
                    timeDeparture = instant(day = 10, hour = 21),
                    timeArrival = instant(day = 11, hour = 2),
                ),
            ),
        )

        val helper = helper(route)

        assertEquals(4 * hour, helper.getPassengerTimeFlow().first())
        assertEquals(600.0, helper.getMoneyAtPassengerFlow().first(), 0.001)
    }

    @Test
    fun holidayMoneyUsesNewRateAfterMidnightAndExcludesBreak() = runTest {
        val route = Route(
            basicData = BasicData(
                timeStartWork = instant(day = 10, hour = 22),
                timeEndWork = instant(day = 11, hour = 2),
                timeStartBreak = instant(day = 11, hour = 0) + 30 * 60_000L,
                timeEndBreak = instant(day = 11, hour = 1),
            ),
        )

        val helper = helper(route, holidayDay = 11)

        assertEquals(90 * 60_000L, helper.getHolidayTimeFlow().first())
        assertEquals(600.0, helper.getMoneyAtHolidayFlow().first(), 0.001)
    }

    @Test
    fun oneMinuteHolidayIsPaidWithoutRoundingToWholeHour() = runTest {
        val route = Route(
            basicData = BasicData(
                timeStartWork = instant(day = 11, hour = 0),
                timeEndWork = instant(day = 11, hour = 0) + 60_000L,
            ),
        )

        val helper = helper(route, holidayDay = 11)

        assertEquals(60_000L, helper.getHolidayTimeFlow().first())
        assertEquals(200.0 * 2.0 / 60.0, helper.getMoneyAtHolidayFlow().first(), 0.001)
    }
}
