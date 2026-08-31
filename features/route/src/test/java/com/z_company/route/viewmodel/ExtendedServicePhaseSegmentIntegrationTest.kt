@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.route.viewmodel

import com.z_company.domain.entities.DateSetTariffRate
import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.SurchargeExtendedServicePhase
import com.z_company.domain.entities.setting.UserSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Test
import kotlin.test.assertEquals

class ExtendedServicePhaseSegmentIntegrationTest {
    private val hour = 3_600_000L
    private val moscow = TimeZone.of("GMT+3")

    private fun instant(day: Int, hour: Int): Long =
        LocalDateTime(2025, 1, day, hour, 0).toInstant(moscow).toEpochMilliseconds()

    private fun helper(totalDistance: Int): SalaryCalculationHelper {
        val month = MonthOfYear(
            year = 2025,
            month = 0,
            tariffRate = 200.0,
            dateSetTariffRate = DateSetTariffRate(dateNewRate = 11, oldRate = 100.0),
            days = (1..31).map { Day(it, TagForDay.WORKING_DAY) },
        )
        val firstDistance = totalDistance / 2
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
            trains = mutableListOf(
                Train(distance = firstDistance.toString()),
                Train(distance = (totalDistance - firstDistance).toString()),
            ),
        )
        return SalaryCalculationHelper(
            userSettings = UserSettings(selectMonthOfYear = month, timeZone = 0L),
            salarySetting = SalarySetting(
                surchargeExtendedServicePhaseList = listOf(
                    SurchargeExtendedServicePhase(distance = "250", percentSurcharge = "10"),
                ),
            ),
            allRoutes = listOf(route),
        )
    }

    @Test
    fun extendedPhaseUsesRouteDistanceAndSegmentTariffExcludingBreakAndPassenger() = runTest {
        val helper = helper(totalDistance = 270)

        assertEquals(listOf(3 * hour), helper.getTimeListSurchargeServicePhaseFlow().first())
        assertEquals(3 * hour, helper.getTotalTimeSurchargeServicePhaseFlow().first())
        assertEquals(listOf(50.0), helper.getMoneyListSurchargeExtendedServicePhaseFlow().first())
    }

    @Test
    fun routeBelowExtendedPhaseThresholdProducesZero() = runTest {
        val helper = helper(totalDistance = 249)

        assertEquals(listOf(0L), helper.getTimeListSurchargeServicePhaseFlow().first())
        assertEquals(listOf(0.0), helper.getMoneyListSurchargeExtendedServicePhaseFlow().first())
    }

    @Test
    fun extendedPhaseSurchargeEntersTotalChargedExactlyOnce() = runTest {
        val eligible = helper(totalDistance = 270)
        val belowThreshold = helper(totalDistance = 249)

        assertEquals(
            50.0,
            eligible.getMoneyTotalChargedFlow().first() -
                    belowThreshold.getMoneyTotalChargedFlow().first(),
            0.001,
        )
    }
}
