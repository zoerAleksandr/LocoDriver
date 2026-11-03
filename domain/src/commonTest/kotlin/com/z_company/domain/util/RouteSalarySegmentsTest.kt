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

    private fun instant(month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        LocalDateTime(2025, month, day, hour, minute).toInstant(moscow).toEpochMilliseconds()

    @Test
    fun zeroDurationRouteProducesNoSalarySegments() {
        val at = instant(month = 1, day = 10, hour = 8)
        val route = Route(basicData = BasicData(timeStartWork = at, timeEndWork = at))

        assertTrue(route.buildSalarySegments(
            monthOfYear = MonthOfYear(year = 2025, month = 0),
            context = context,
            initialTariffRatePerHour = 100.0,
        ).isEmpty())
    }

    @Test
    fun oneMinuteRoutePreservesExactDurationAndFractionalMoney() {
        val start = instant(month = 1, day = 10, hour = 8)
        val route = Route(basicData = BasicData(
            timeStartWork = start,
            timeEndWork = start + 60_000L,
        ))

        val segments = route.buildSalarySegments(
            monthOfYear = MonthOfYear(year = 2025, month = 0),
            context = context,
            initialTariffRatePerHour = 100.0,
        )

        assertEquals(60_000L, segments.sumOf { it.interval.durationMillis })
        assertEquals(100.0 / 60.0, segments.sumOf(SalarySegment::tariffMoney), 0.000_001)
    }

    @Test
    fun partiallyIntersectingBreakSubtractsOnlyItsOverlap() {
        val start = instant(month = 1, day = 10, hour = 8)
        val route = Route(basicData = BasicData(
            timeStartWork = start,
            timeEndWork = start + hour,
            timeStartBreak = start - 30 * 60_000L,
            timeEndBreak = start + 30 * 60_000L,
        ))

        val duration = route.buildSalarySegments(
            monthOfYear = MonthOfYear(year = 2025, month = 0),
            context = context,
            initialTariffRatePerHour = 100.0,
        ).sumOf { it.interval.durationMillis }

        assertEquals(30 * 60_000L, duration)
    }

    @Test
    fun breakOutsideRouteDoesNotReducePayableDuration() {
        val start = instant(month = 1, day = 10, hour = 8)
        val route = Route(basicData = BasicData(
            timeStartWork = start,
            timeEndWork = start + hour,
            timeStartBreak = start + 2 * hour,
            timeEndBreak = start + 3 * hour,
        ))

        val duration = route.buildSalarySegments(
            monthOfYear = MonthOfYear(year = 2025, month = 0),
            context = context,
            initialTariffRatePerHour = 100.0,
        ).sumOf { it.interval.durationMillis }

        assertEquals(hour, duration)
    }

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
        // Одно лицо — вся смена без перерыва и следования пассажиром. Первый час
        // смены примыкает к отправлению пассажиром, но работы на локомотиве до
        // него не было — это не ожидание 018M, а обычная работа.
        assertEquals(6 * hour, segments.filter { AccrualCondition.ONE_PERSON in it.conditions }
            .sumOf { it.interval.durationMillis })
        assertTrue(segments.none { AccrualCondition.PASSENGER_WAITING in it.conditions })
        assertTrue(segments.none {
            AccrualCondition.PASSENGER in it.conditions && AccrualCondition.ONE_PERSON in it.conditions
        })
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
    fun tieredTrainSurchargeUsesWholeShiftBreakAndHighestTierOfRoute() {
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
                // Строгие пороги: 6001 > 6000 (1-я ступень), 10001 > 10000 (2-я).
                train(weight = "6001", fromHour = 1, toHour = 5),
                train(weight = "10001", fromHour = 2, toHour = 6),
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

        // База — вся смена (8 ч) без перерыва (1 ч); одна ступень — самого
        // тяжёлого поезда маршрута.
        assertEquals(0L, tiers[0].sumOf { it.interval.durationMillis })
        assertEquals(7 * hour, tiers[1].sumOf { it.interval.durationMillis })
    }

    @Test
    fun tieredTrainSurchargeKeepsTariffOfEachTrainSegment() {
        val start = instant(month = 1, day = 10, hour = 8)
        val route = Route(
            basicData = BasicData(timeStartWork = start, timeEndWork = start + 6 * hour),
            trains = mutableListOf(
                Train(
                    weight = "6001",
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

        assertEquals(6 * hour, tier.sumOf { it.interval.durationMillis })
        assertEquals(900.0, tier.sumOf(SalarySegment::tariffMoney), 0.001)
    }

    @Test
    fun strictTrainThresholdStartsOnlyAboveEnteredConditionalLength() {
        val start = instant(month = 1, day = 10, hour = 8)
        fun train(length: String, fromHour: Int, toHour: Int) = Train(
            conditionalLength = length,
            stations = mutableListOf(
                Station(timeDeparture = start + fromHour * hour),
                Station(timeArrival = start + toHour * hour),
            ),
        )
        val route = Route(
            basicData = BasicData(timeStartWork = start, timeEndWork = start + 4 * hour),
            trains = mutableListOf(
                train(length = "80", fromHour = 0, toHour = 2),
                train(length = "81", fromHour = 2, toHour = 4),
            ),
        )

        val tier = route.buildTieredTrainSurchargeSegments(
            monthOfYear = MonthOfYear(year = 2025, month = 0),
            context = context,
            initialTariffRatePerHour = 100.0,
            thresholds = listOf(80),
            condition = AccrualCondition.LONG_TRAIN,
            thresholdIsInclusive = false,
            valueOf = { it.conditionalLength?.toIntOrNull() },
        ).single()

        // 81 > 80 — вся смена; 80 само по себе доплату не включает.
        assertEquals(4 * hour, tier.sumOf { it.interval.durationMillis })
    }

    @Test
    fun heavyTrainThresholdIsStrictByDefaultLikeLongTrains() {
        val start = instant(month = 1, day = 10, hour = 8)
        fun train(weight: String, fromHour: Int, toHour: Int) = Train(
            weight = weight,
            stations = mutableListOf(
                Station(timeDeparture = start + fromHour * hour),
                Station(timeArrival = start + toHour * hour),
            ),
        )
        val route = Route(
            basicData = BasicData(timeStartWork = start, timeEndWork = start + 4 * hour),
            trains = mutableListOf(
                // Равен порогу — тяжеловесным НЕ считается; 6001 — считается.
                train(weight = "6000", fromHour = 0, toHour = 2),
                train(weight = "6001", fromHour = 2, toHour = 4),
            ),
        )

        val tier = route.buildTieredTrainSurchargeSegments(
            monthOfYear = MonthOfYear(year = 2025, month = 0),
            context = context,
            initialTariffRatePerHour = 100.0,
            thresholds = listOf(6000),
            condition = AccrualCondition.HEAVY_TRAIN,
            valueOf = { it.weight?.toIntOrNull() },
        ).single()

        assertEquals(4 * hour, tier.sumOf { it.interval.durationMillis })
    }

    @Test
    fun trainEqualToThresholdProducesNoSegments() {
        val start = instant(month = 1, day = 10, hour = 8)
        val route = Route(
            basicData = BasicData(timeStartWork = start, timeEndWork = start + 4 * hour),
            trains = mutableListOf(Train(weight = "6000")),
        )

        val tier = route.buildTieredTrainSurchargeSegments(
            monthOfYear = MonthOfYear(year = 2025, month = 0),
            context = context,
            initialTariffRatePerHour = 100.0,
            thresholds = listOf(6000),
            condition = AccrualCondition.HEAVY_TRAIN,
            valueOf = { it.weight?.toIntOrNull() },
        ).single()

        assertTrue(tier.isEmpty())
    }

    @Test
    fun trainWithoutStationTimesStillGetsWholeShiftSurcharge() {
        val start = instant(month = 1, day = 10, hour = 8)
        val route = Route(
            basicData = BasicData(
                timeStartWork = start,
                timeEndWork = start + 5 * hour,
                timeStartBreak = start + 2 * hour,
                timeEndBreak = start + 3 * hour,
            ),
            // Станции не заполнены или конечная без времени — доплата не теряется.
            trains = mutableListOf(
                Train(
                    weight = "6001",
                    stations = mutableListOf(
                        Station(timeDeparture = start + hour),
                        Station(),
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
            valueOf = { it.weight?.toIntOrNull() },
        ).single()

        assertEquals(4 * hour, tier.sumOf { it.interval.durationMillis })
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

    // ── «Явка по прибытию» со смещением: зазор [прибытие; явка) > 1 мин = ожидание 018M ──

    private fun arrivalRoute(arrival: Long, offsetMinutes: Int, shiftHours: Int = 8) = Route(
        basicData = BasicData(
            timeStartWork = arrival + offsetMinutes * 60_000L,
            timeEndWork = arrival + offsetMinutes * 60_000L + shiftHours * hour,
        ),
        passengers = mutableListOf(
            Passenger(
                timeDeparture = arrival - 2 * hour,
                timeArrival = arrival,
                isWorkStartByArrival = true,
                workStartOffsetMinutes = offsetMinutes,
            ),
        ),
    )

    private fun segmentsOf(route: Route, nightWindow: NightWindow? = null) =
        route.buildSalarySegments(
            monthOfYear = MonthOfYear(year = 2025, month = 0),
            context = context,
            initialTariffRatePerHour = 100.0,
            nightWindow = nightWindow,
        )

    @Test
    fun workStartGapOfOneMinuteIsNotPaid() {
        val arrival = instant(month = 1, day = 10, hour = 8)
        val segments = segmentsOf(arrivalRoute(arrival, offsetMinutes = 1))

        assertEquals(8 * hour, segments.sumOf { it.interval.durationMillis })
        assertTrue(segments.none { AccrualCondition.PASSENGER_WAITING in it.conditions })
    }

    @Test
    fun workStartGapOverOneMinuteIsPassengerWaiting() {
        val arrival = instant(month = 1, day = 10, hour = 8)
        val segments = segmentsOf(arrivalRoute(arrival, offsetMinutes = 5))

        val waiting = segments.filter { AccrualCondition.PASSENGER_WAITING in it.conditions }
        assertEquals(5 * 60_000L, waiting.sumOf { it.interval.durationMillis })
        assertEquals(arrival, waiting.minOf { it.interval.startMillis })
        assertEquals(arrival + 5 * 60_000L, waiting.maxOf { it.interval.endMillis })
        // Смена + ожидание оплачиваются по тарифу.
        assertEquals(8 * hour + 5 * 60_000L, segments.sumOf { it.interval.durationMillis })
    }

    @Test
    fun workStartWaitingAtNightIsNightToo() {
        val arrival = instant(month = 1, day = 10, hour = 21, minute = 50)
        val segments = segmentsOf(
            arrivalRoute(arrival, offsetMinutes = 20, shiftHours = 2),
            nightWindow = NightWindow(22, 0, 6, 0, 0L),
        )

        val waiting = segments.filter { AccrualCondition.PASSENGER_WAITING in it.conditions }
        assertEquals(20 * 60_000L, waiting.sumOf { it.interval.durationMillis })
        // Ночь с 22:00: из ожидания 21:50–22:10 ночных 10 минут.
        assertEquals(
            10 * 60_000L,
            waiting.filter { AccrualCondition.NIGHT in it.conditions }
                .sumOf { it.interval.durationMillis },
        )
    }

    @Test
    fun routeWithoutArrivalWorkStartHasNoWorkStartWaiting() {
        val start = instant(month = 1, day = 10, hour = 8)
        val route = Route(
            basicData = BasicData(timeStartWork = start, timeEndWork = start + 8 * hour),
            passengers = mutableListOf(
                Passenger(timeDeparture = start - 2 * hour, timeArrival = start - 10 * 60_000L),
            ),
        )
        assertTrue(segmentsOf(route).none { AccrualCondition.PASSENGER_WAITING in it.conditions })
    }
}
