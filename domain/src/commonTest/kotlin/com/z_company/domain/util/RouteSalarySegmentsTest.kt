@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.domain.util

import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.Day
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RouteSalarySegmentsTest {
    private val hour = 3_600_000L
    private val moscow = TimeZone.of("GMT+3")
    private val context = TimeCalculationContext(localTZ = moscow, crossMonthTZ = moscow)

    private fun instant(month: Int, day: Int, hour: Int): Long =
        LocalDateTime(2025, month, day, hour, 0).toInstant(moscow).toEpochMilliseconds()

    @Test
    fun routeIsClippedToMonthAndBreakIsSubtractedOnlyInsideMonth() {
        val monthStart = instant(month = 2, day = 1, hour = 0)
        val route = Route(
            basicData = BasicData(
                timeStartWork = instant(month = 1, day = 31, hour = 22),
                timeEndWork = instant(month = 2, day = 1, hour = 6),
                timeStartBreak = instant(month = 1, day = 31, hour = 23),
                timeEndBreak = instant(month = 2, day = 1, hour = 2),
            ),
        )

        val segments = route.buildSalarySegments(
            monthOfYear = MonthOfYear(year = 2025, month = 1),
            context = context,
            initialTariffRatePerHour = 100.0,
        )

        assertEquals(listOf(TimeInterval(monthStart + 2 * hour, monthStart + 6 * hour)),
            segments.map(SalarySegment::interval))
        assertEquals(4 * hour, segments.sumOf { it.interval.durationMillis })
    }

    @Test
    fun routeSegmentsPreserveNightPassengerOnePersonAndTariffBoundary() {
        val start = instant(month = 1, day = 10, hour = 20)
        val route = Route(
            basicData = BasicData(
                isOnePersonOperation = true,
                timeStartWork = start,
                timeEndWork = start + 8 * hour,
                timeStartBreak = start + 3 * hour,
                timeEndBreak = start + 4 * hour,
            ),
            passengers = mutableListOf(
                Passenger(timeDeparture = start + hour, timeArrival = start + 2 * hour),
            ),
            trains = mutableListOf(Train(number = "2503")),
        )

        val segments = route.buildSalarySegments(
            monthOfYear = MonthOfYear(year = 2025, month = 0),
            context = context,
            initialTariffRatePerHour = 100.0,
            tariffChanges = listOf(TariffChange(start + 5 * hour, 200.0)),
            nightWindow = NightWindow(22, 0, 6, 0, 0L),
        )

        assertEquals(7 * hour, segments.sumOf { it.interval.durationMillis })
        assertEquals(1_000.0, segments.sumOf(SalarySegment::tariffMoney), 0.001)
        assertEquals(hour, segments.filter { AccrualCondition.PASSENGER in it.conditions }
            .sumOf { it.interval.durationMillis })
        assertEquals(5 * hour, segments.filter { AccrualCondition.NIGHT in it.conditions }
            .sumOf { it.interval.durationMillis })
        assertTrue(segments.all { AccrualCondition.ONE_PERSON in it.conditions })
    }

    @Test
    fun holidayConditionUsesExactDayAndExcludesIntersectingBreak() {
        val route = Route(
            basicData = BasicData(
                timeStartWork = instant(month = 1, day = 10, hour = 22),
                timeEndWork = instant(month = 1, day = 11, hour = 2),
                timeStartBreak = instant(month = 1, day = 11, hour = 0) + 30 * 60_000L,
                timeEndBreak = instant(month = 1, day = 11, hour = 1),
            ),
        )
        val month = MonthOfYear(
            year = 2025,
            month = 0,
            days = listOf(Day(dayOfMonth = 11, tag = TagForDay.HOLIDAY)),
        )

        val holidaySegments = route.buildSalarySegments(
            monthOfYear = month,
            context = context,
            initialTariffRatePerHour = 100.0,
        ).filter { AccrualCondition.HOLIDAY in it.conditions }

        assertEquals(90 * 60_000L, holidaySegments.sumOf { it.interval.durationMillis })
    }

    @Test
    fun onePersonRouteWithoutTrainUsesFreightCategory() {
        val start = instant(month = 1, day = 10, hour = 8)
        val route = Route(
            basicData = BasicData(
                isOnePersonOperation = true,
                timeStartWork = start,
                timeEndWork = start + 4 * hour,
            ),
        )

        val segments = route.buildSalarySegments(
            monthOfYear = MonthOfYear(year = 2025, month = 0),
            context = context,
            initialTariffRatePerHour = 100.0,
        )

        assertEquals(4 * hour, segments
            .filter { AccrualCondition.ONE_PERSON_FREIGHT in it.conditions }
            .sumOf { it.interval.durationMillis })
        assertTrue(segments.none { AccrualCondition.ONE_PERSON_PASSENGER in it.conditions })
    }

    @Test
    fun tieredTrainSurchargeUsesActualIntervalsBreakAndHighestOverlappingTier() {
        val start = instant(month = 1, day = 10, hour = 8)
        fun train(weight: String, fromHour: Int, toHour: Int) = Train(
            weight = weight,
            stations = mutableListOf(
                Station(timeDeparture = start + fromHour * hour),
                Station(timeArrival = start + toHour * hour),
            ),
        )
        val route = Route(
            basicData = BasicData(
                timeStartWork = start,
                timeEndWork = start + 8 * hour,
                timeStartBreak = start + 3 * hour,
                timeEndBreak = start + 4 * hour,
            ),
            trains = mutableListOf(
                train(weight = "6000", fromHour = 1, toHour = 5),
                train(weight = "10000", fromHour = 2, toHour = 6),
            ),
        )

        val tiers = route.buildTieredTrainSurchargeSegments(
            monthOfYear = MonthOfYear(year = 2025, month = 0),
            context = context,
            initialTariffRatePerHour = 100.0,
            thresholds = listOf(6000, 10000),
            condition = AccrualCondition.HEAVY_TRAIN,
            valueOf = { it.weight?.toIntOrNull() },
        )

        assertEquals(hour, tiers[0].sumOf { it.interval.durationMillis })
        assertEquals(3 * hour, tiers[1].sumOf { it.interval.durationMillis })
        assertEquals(4 * hour, tiers.flatten().sumOf { it.interval.durationMillis })
    }

    @Test
    fun tieredTrainSurchargeKeepsTariffOfEachTrainSegment() {
        val start = instant(month = 1, day = 10, hour = 8)
        val route = Route(
            basicData = BasicData(timeStartWork = start, timeEndWork = start + 6 * hour),
            trains = mutableListOf(
                Train(
                    weight = "6000",
                    stations = mutableListOf(
                        Station(timeDeparture = start + hour),
                        Station(timeArrival = start + 5 * hour),
                    ),
                ),
            ),
        )

        val tier = route.buildTieredTrainSurchargeSegments(
            monthOfYear = MonthOfYear(year = 2025, month = 0),
            context = context,
            initialTariffRatePerHour = 100.0,
            thresholds = listOf(6000),
            condition = AccrualCondition.HEAVY_TRAIN,
            tariffChanges = listOf(TariffChange(start + 3 * hour, 200.0)),
            valueOf = { it.weight?.toIntOrNull() },
        ).single()

        assertEquals(4 * hour, tier.sumOf { it.interval.durationMillis })
        assertEquals(600.0, tier.sumOf(SalarySegment::tariffMoney), 0.001)
    }

    @Test
    fun tieredTrainSurchargeRejectsAmbiguousThresholdOrder() {
        val route = Route(
            basicData = BasicData(
                timeStartWork = instant(month = 1, day = 10, hour = 8),
                timeEndWork = instant(month = 1, day = 10, hour = 9),
            ),
        )

        kotlin.test.assertFailsWith<IllegalArgumentException> {
            route.buildTieredTrainSurchargeSegments(
                monthOfYear = MonthOfYear(year = 2025, month = 0),
                context = context,
                initialTariffRatePerHour = 100.0,
                thresholds = listOf(10000, 6000),
                condition = AccrualCondition.HEAVY_TRAIN,
                valueOf = { it.weight?.toIntOrNull() },
            )
        }
    }
}
