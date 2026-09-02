@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.route.viewmodel

import com.z_company.domain.entities.DateSetTariffRate
import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Station
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

class HeavyLongDistanceSegmentIntegrationTest {
    private val hour = 3_600_000L
    private val moscow = TimeZone.of("GMT+3")

    private fun instant(day: Int, hour: Int): Long =
        LocalDateTime(2025, 1, day, hour, 0).toInstant(moscow).toEpochMilliseconds()

    private fun helper(weight: String, axle: String = "351"): SalaryCalculationHelper {
        val start = instant(day = 10, hour = 22)
        val end = instant(day = 11, hour = 3)
        val route = Route(
            basicData = BasicData(
                timeStartWork = start,
                timeEndWork = end,
                timeStartBreak = instant(day = 10, hour = 23),
                timeEndBreak = instant(day = 11, hour = 0),
            ),
            trains = mutableListOf(
                Train(
                    weight = weight,
                    axle = axle,
                    stations = mutableListOf(
                        Station(timeDeparture = start),
                        Station(timeArrival = end),
                    ),
                ),
            ),
            passengers = mutableListOf(
                Passenger(
                    timeDeparture = instant(day = 11, hour = 0),
                    timeArrival = instant(day = 11, hour = 1),
                ),
            ),
        )
        val month = MonthOfYear(
            year = 2025,
            month = 0,
            tariffRate = 200.0,
            dateSetTariffRate = DateSetTariffRate(dateNewRate = 11, oldRate = 100.0),
            days = (1..31).map { Day(it, TagForDay.WORKING_DAY) },
        )
        return SalaryCalculationHelper(
            userSettings = UserSettings(selectMonthOfYear = month, timeZone = 0L),
            salarySetting = SalarySetting(surchargeHeavyLongDistanceTrains = 5.0),
            allRoutes = listOf(route),
        )
    }

    @Test
    fun qualifyingTrainUsesActualPayableTariffSegments() = runTest {
        val helper = helper(weight = "6000,5")

        assertEquals(3 * hour, helper.getTimeHeavyLongDistanceTrainsFlow().first())
        assertEquals(25.0, helper.getMoneyHeavyLongDistanceTrainsFlow().first(), 0.001)
    }

    @Test
    fun exactlySixThousandDoesNotQualify() = runTest {
        val helper = helper(weight = "6000")

        assertEquals(0L, helper.getTimeHeavyLongDistanceTrainsFlow().first())
        assertEquals(0.0, helper.getMoneyHeavyLongDistanceTrainsFlow().first(), 0.001)
    }

    @Test
    fun exactlyThreeHundredFiftyAxlesDoesNotQualify() = runTest {
        val helper = helper(weight = "6000,5", axle = "350")

        assertEquals(0L, helper.getTimeHeavyLongDistanceTrainsFlow().first())
        assertEquals(0.0, helper.getMoneyHeavyLongDistanceTrainsFlow().first(), 0.001)
    }

    @Test
    fun threeHundredFiftyOneAxlesQualifies() = runTest {
        val helper = helper(weight = "6000,5", axle = "351")

        assertEquals(3 * hour, helper.getTimeHeavyLongDistanceTrainsFlow().first())
        assertEquals(25.0, helper.getMoneyHeavyLongDistanceTrainsFlow().first(), 0.001)
    }
}
