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
import com.z_company.domain.entities.setting.SurchargeHeavyTrains
import com.z_company.domain.entities.setting.SurchargeLongTrains
import com.z_company.domain.entities.setting.UserSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Test
import kotlin.test.assertEquals

class TrainSurchargeSegmentIntegrationTest {
    private val hour = 3_600_000L
    private val moscow = TimeZone.of("GMT+3")

    private fun instant(day: Int, hour: Int, minute: Int = 0): Long =
        LocalDateTime(2025, 1, day, hour, minute).toInstant(moscow).toEpochMilliseconds()

    private fun helper(
        train: Train,
        heavy: List<SurchargeHeavyTrains> = listOf(
            SurchargeHeavyTrains(weight = "6000", percentSurcharge = "10"),
        ),
        long: List<SurchargeLongTrains> = listOf(
            SurchargeLongTrains(conditionalLength = "80", percentSurcharge = "20"),
        ),
    ): SalaryCalculationHelper {
        val month = MonthOfYear(
            year = 2025,
            month = 0,
            tariffRate = 200.0,
            dateSetTariffRate = DateSetTariffRate(dateNewRate = 11, oldRate = 100.0),
            days = (1..31).map { Day(it, TagForDay.WORKING_DAY) },
        )
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
            trains = mutableListOf(train),
        )
        return SalaryCalculationHelper(
            userSettings = UserSettings(selectMonthOfYear = month, timeZone = 0L),
            salarySetting = SalarySetting(
                surchargeHeavyTrainsList = heavy,
                surchargeLongTrainsList = long,
            ),
            allRoutes = listOf(route),
        )
    }

    @Test
    fun heavyAndLongSurchargesUseTrainIntervalBreakPassengerAndTariffBoundary() = runTest {
        val train = Train(
            weight = "6000",
            conditionalLength = "81",
            stations = mutableListOf(
                Station(timeDeparture = instant(day = 10, hour = 22, minute = 30)),
                Station(timeArrival = instant(day = 11, hour = 2, minute = 30)),
            ),
        )
        val helper = helper(train)

        assertEquals(listOf(2 * hour), helper.getTimeListSurchargeHeavyTrainsFlow().first())
        assertEquals(listOf(35.0), helper.getMoneyListSurchargeExtendedHeavyTrainsFlow().first())
        assertEquals(listOf(2 * hour), helper.getTimeListSurchargeLongTrainsFlow().first())
        assertEquals(listOf(70.0), helper.getMoneyListSurchargeLongTrainsFlow().first())
    }

    @Test
    fun longTrainSurchargeStartsOnlyAboveConfiguredBoundary() = runTest {
        fun helperFor(length: String) = helper(
            train = Train(
                conditionalLength = length,
                stations = mutableListOf(
                    Station(timeDeparture = instant(day = 10, hour = 22, minute = 30)),
                    Station(timeArrival = instant(day = 11, hour = 2, minute = 30)),
                ),
            ),
            heavy = emptyList(),
        )

        assertEquals(
            listOf(0L),
            helperFor("80").getTimeListSurchargeLongTrainsFlow().first(),
        )
        assertEquals(
            listOf(0.0),
            helperFor("80").getMoneyListSurchargeLongTrainsFlow().first(),
        )
        assertEquals(
            listOf(2 * hour),
            helperFor("81").getTimeListSurchargeLongTrainsFlow().first(),
        )
        assertEquals(
            listOf(70.0),
            helperFor("81").getMoneyListSurchargeLongTrainsFlow().first(),
        )
    }

    @Test
    fun trainBelowConfiguredThresholdProducesZeroAccrual() = runTest {
        val train = Train(
            weight = "5999",
            conditionalLength = "79",
            stations = mutableListOf(
                Station(timeDeparture = instant(day = 10, hour = 22)),
                Station(timeArrival = instant(day = 11, hour = 2)),
            ),
        )
        val helper = helper(train)

        assertEquals(listOf(0L), helper.getTimeListSurchargeHeavyTrainsFlow().first())
        assertEquals(listOf(0.0), helper.getMoneyListSurchargeExtendedHeavyTrainsFlow().first())
        assertEquals(listOf(0L), helper.getTimeListSurchargeLongTrainsFlow().first())
        assertEquals(listOf(0.0), helper.getMoneyListSurchargeLongTrainsFlow().first())
    }

    @Test
    fun exactAndAdjacentHeavyThresholdsSelectOnlyHighestApplicableTier() = runTest {
        val tiers = listOf(
            SurchargeHeavyTrains(weight = "10000", percentSurcharge = "20"),
            SurchargeHeavyTrains(weight = "6000", percentSurcharge = "10"),
        )

        suspend fun money(weight: String): List<Double> = helper(
            train = Train(
                weight = weight,
                stations = mutableListOf(
                    Station(timeDeparture = instant(day = 10, hour = 22, minute = 30)),
                    Station(timeArrival = instant(day = 11, hour = 2, minute = 30)),
                ),
            ),
            heavy = tiers,
            long = emptyList(),
        ).getMoneyListSurchargeExtendedHeavyTrainsFlow().first()

        assertEquals(listOf(0.0, 0.0), money("5999"))
        assertEquals(listOf(35.0, 0.0), money("6000"))
        assertEquals(listOf(35.0, 0.0), money("9999"))
        assertEquals(listOf(0.0, 70.0), money("10000"))
        assertEquals(listOf(0.0, 70.0), money("10001"))
    }
}
