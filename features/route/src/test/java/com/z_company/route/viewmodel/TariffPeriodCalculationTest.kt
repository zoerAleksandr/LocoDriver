@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.route.viewmodel

import com.z_company.domain.entities.Day
import com.z_company.domain.entities.DateSetTariffRate
import com.z_company.domain.entities.MonthOfYear
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

class TariffPeriodCalculationTest {
    private val hour = 3_600_000L

    private fun instant(day: Int, hour: Int): Long =
        LocalDateTime(2025, 1, day, hour, 0)
            .toInstant(TimeZone.of("GMT+3"))
            .toEpochMilliseconds()

    private fun route(day: Int): Route = Route(
        basicData = BasicData(
            timeStartWork = instant(day, 8),
            timeEndWork = instant(day, 16),
        ),
    )

    private fun route(startDay: Int, startHour: Int, endDay: Int, endHour: Int): Route = Route(
        basicData = BasicData(
            timeStartWork = instant(startDay, startHour),
            timeEndWork = instant(endDay, endHour),
        ),
    )

    private fun helper(
        routes: List<Route>,
        dateSetTariffRate: DateSetTariffRate? = null,
    ): SalaryCalculationHelper {
        val month = MonthOfYear(
            year = 2025,
            month = 0,
            tariffRate = 200.0,
            dateSetTariffRate = dateSetTariffRate,
            days = (1..31).map { Day(it, TagForDay.WORKING_DAY) },
        )
        return SalaryCalculationHelper(
            userSettings = UserSettings(selectMonthOfYear = month, timeZone = 0L),
            salarySetting = SalarySetting(),
            allRoutes = routes,
        )
    }

    @Test
    fun tariffPeriodUsesOnlyRoutesPassedForThatPeriod() = runTest {
        val beforeChange = route(day = 5)
        val afterChange = route(day = 20)
        val helper = helper(listOf(beforeChange, afterChange))

        val firstPeriodTime = helper.getWorkTimeInPeriodAtTariffFlow(
            routeList = listOf(beforeChange),
            period = 1 to 15,
        ).first()
        val secondPeriodTime = helper.getWorkTimeInPeriodAtTariffFlow(
            routeList = listOf(afterChange),
            period = 15 to 31,
        ).first()

        assertEquals(8 * hour, firstPeriodTime)
        assertEquals(8 * hour, secondPeriodTime)
        assertEquals(16 * hour, firstPeriodTime + secondPeriodTime)
    }

    @Test
    fun moneyUsesOldAndNewRatesOnlyForTheirOwnRoutes() = runTest {
        val helper = helper(
            routes = listOf(route(day = 5), route(day = 20)),
            dateSetTariffRate = DateSetTariffRate(dateNewRate = 15, oldRate = 100.0),
        )

        assertEquals(2_400.0, helper.getMoneyAtWorkTimeAtTariff().first(), 0.001)
    }

    @Test
    fun routeCrossingTariffMidnightIsSplitWithoutLostOrDuplicatedTime() = runTest {
        val crossingRoute = route(startDay = 14, startHour = 22, endDay = 15, endHour = 2)
        val helper = helper(
            routes = listOf(crossingRoute),
            dateSetTariffRate = DateSetTariffRate(dateNewRate = 15, oldRate = 100.0),
        )

        val (oldRateRoutes, newRateRoutes) = helper.getTwoRouteList(listOf(crossingRoute)).first()
        val oldTime = helper.getWorkTimeInPeriodAtTariffFlow(oldRateRoutes, 1 to 15).first()
        val newTime = helper.getWorkTimeInPeriodAtTariffFlow(newRateRoutes, 15 to 31).first()

        assertEquals(2 * hour, oldTime)
        assertEquals(2 * hour, newTime)
        assertEquals(4 * hour, oldTime + newTime)
        assertEquals(600.0, helper.getMoneyAtWorkTimeAtTariff().first(), 0.001)
    }
}
