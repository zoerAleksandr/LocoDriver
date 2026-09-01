package com.z_company.domain.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OvertimeSegmentCalculationTest {
    private val hour = 3_600_000L

    private fun segment(
        startHour: Long,
        endHour: Long,
        tariff: Double = 100.0,
        vararg conditions: AccrualCondition,
    ) = SalarySegment(
        interval = TimeInterval(startHour * hour, endHour * hour),
        tariffRatePerHour = tariff,
        conditions = setOf(AccrualCondition.REGULAR) + conditions,
    )

    @Test
    fun nightPercentAppliesOnlyToHalfRateSegmentWhereNightActuallyExists() {
        val result = calculateOvertimeBreakdown(
            segments = listOf(
                segment(0, 2, conditions = arrayOf(AccrualCondition.NIGHT)),
                segment(2, 4),
            ),
            halfRateDurationMillis = 2 * hour,
            conditionPercents = mapOf(AccrualCondition.NIGHT to 40.0),
        )

        assertEquals(400.0, result.ordinaryTariffMoney, 0.001)
        assertEquals(140.0, result.halfRateExtraMoney, 0.001)
        assertEquals(200.0, result.fullRateExtraMoney, 0.001)
        assertEquals(740.0, result.totalMoney, 0.001)
    }

    @Test
    fun nightPercentOnFullRateSegmentIsNotAveragedIntoHalfRateSegment() {
        val result = calculateOvertimeBreakdown(
            segments = listOf(
                segment(0, 2),
                segment(2, 4, conditions = arrayOf(AccrualCondition.NIGHT)),
            ),
            halfRateDurationMillis = 2 * hour,
            conditionPercents = mapOf(AccrualCondition.NIGHT to 40.0),
        )

        assertEquals(100.0, result.halfRateExtraMoney, 0.001)
        assertEquals(280.0, result.fullRateExtraMoney, 0.001)
    }

    @Test
    fun tariffChangeUsesEachOvertimeSegmentsOwnRate() {
        val result = calculateOvertimeBreakdown(
            segments = listOf(segment(0, 2, tariff = 100.0), segment(2, 4, tariff = 150.0)),
            halfRateDurationMillis = 2 * hour,
        )

        assertEquals(500.0, result.ordinaryTariffMoney, 0.001)
        assertEquals(100.0, result.halfRateExtraMoney, 0.001)
        assertEquals(300.0, result.fullRateExtraMoney, 0.001)
    }

    @Test
    fun halfRateBoundaryInsideSegmentSplitsWithoutChangingTotalDuration() {
        val result = calculateOvertimeBreakdown(
            segments = listOf(segment(0, 3)),
            halfRateDurationMillis = 2 * hour,
        )

        assertEquals(listOf(TimeInterval(0, 2 * hour)), result.halfRateSegments.map { it.interval })
        assertEquals(listOf(TimeInterval(2 * hour, 3 * hour)), result.fullRateSegments.map { it.interval })
        assertEquals(3 * hour, result.overtimeSegments.sumOf { it.interval.durationMillis })
    }

    @Test
    fun holidaySegmentIsExcludedFromOvertimeWithoutConsumingHalfRateHours() {
        val result = calculateOvertimeBreakdown(
            segments = listOf(
                segment(0, 2, conditions = arrayOf(AccrualCondition.HOLIDAY)),
                segment(2, 4),
            ),
            halfRateDurationMillis = 2 * hour,
        )

        assertEquals(2 * hour, result.overtimeSegments.sumOf { it.interval.durationMillis })
        assertEquals(200.0, result.ordinaryTariffMoney, 0.001)
        assertEquals(100.0, result.halfRateExtraMoney, 0.001)
        assertEquals(0.0, result.fullRateExtraMoney, 0.001)
    }

    @Test
    fun rejectsInvalidPercentDurationAndOverlappingSegments() {
        assertFailsWith<IllegalArgumentException> {
            calculateOvertimeBreakdown(listOf(segment(0, 1)), -1L)
        }
        listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> {
                calculateOvertimeBreakdown(
                    listOf(segment(0, 1)),
                    hour,
                    mapOf(AccrualCondition.NIGHT to invalid),
                )
            }
        }
        assertFailsWith<IllegalArgumentException> {
            calculateOvertimeBreakdown(
                listOf(segment(0, 2), segment(1, 3)),
                hour,
            )
        }
    }

    @Test
    fun zeroOvertimeProducesFiniteZeroMoney() {
        val result = calculateOvertimeBreakdown(emptyList(), 0L)

        assertEquals(0.0, result.totalMoney, 0.0)
        assertTrue(result.totalMoney.isFinite())
    }

    @Test
    fun latestOvertimeSelectionKeepsConditionsOfActualTailSegments() {
        val selected = selectLatestOvertimeSegments(
            workSegments = listOf(
                segment(0, 4),
                segment(4, 6, conditions = arrayOf(AccrualCondition.NIGHT)),
            ),
            overtimeDurationMillis = 3 * hour,
        )

        assertEquals(
            listOf(TimeInterval(3 * hour, 4 * hour), TimeInterval(4 * hour, 6 * hour)),
            selected.map { it.interval },
        )
        assertEquals(
            listOf(false, true),
            selected.map { AccrualCondition.NIGHT in it.conditions },
        )
        assertTrue(selected.all { AccrualCondition.OVERTIME in it.conditions })
    }

    @Test
    fun holidayAtEndDoesNotConsumeRequestedOvertimeDuration() {
        val selected = selectLatestOvertimeSegments(
            workSegments = listOf(
                segment(0, 4),
                segment(4, 6, conditions = arrayOf(AccrualCondition.HOLIDAY)),
            ),
            overtimeDurationMillis = 3 * hour,
        )

        assertEquals(listOf(TimeInterval(hour, 4 * hour)), selected.map { it.interval })
        assertEquals(3 * hour, selected.sumOf { it.interval.durationMillis })
    }

    @Test
    fun latestOvertimeSelectionPreservesTariffAtChangeBoundary() {
        val selected = selectLatestOvertimeSegments(
            workSegments = listOf(
                segment(0, 4, tariff = 100.0),
                segment(4, 8, tariff = 150.0),
            ),
            overtimeDurationMillis = 6 * hour,
        )

        assertEquals(listOf(100.0, 150.0), selected.map { it.tariffRatePerHour })
        assertEquals(listOf(2 * hour, 4 * hour), selected.map { it.interval.durationMillis })
        assertEquals(800.0, selected.sumOf(SalarySegment::tariffMoney), 0.001)
    }

    @Test
    fun latestOvertimeSelectionClampsToEligibleTimeAndRejectsInvalidInput() {
        val work = listOf(segment(0, 2))

        assertEquals(2 * hour, selectLatestOvertimeSegments(work, 10 * hour)
            .sumOf { it.interval.durationMillis })
        assertTrue(selectLatestOvertimeSegments(work, 0L).isEmpty())
        assertFailsWith<IllegalArgumentException> {
            selectLatestOvertimeSegments(work, -1L)
        }
    }

    @Test
    fun september2026CalculatesFirstTwoHoursForEachActualShift() {
        val result = calculateOvertimePremiumDurations(
            overtimeByShiftMillis = listOf(4 * hour, 0L),
            year = 2026,
            zeroBasedMonth = 8,
        )

        assertEquals(2 * hour, result.halfRateMillis)
        assertEquals(2 * hour, result.fullRateMillis)
    }

    @Test
    fun zeroToFourHoursInOneShiftAreSplitAtTwoHourBoundary() {
        (0L..4L).forEach { overtimeHours ->
            val result = calculateOvertimePremiumDurations(
                overtimeByShiftMillis = listOf(overtimeHours * hour),
                year = 2026,
                zeroBasedMonth = 8,
            )

            assertEquals(minOf(overtimeHours, 2L) * hour, result.halfRateMillis)
            assertEquals((overtimeHours - minOf(overtimeHours, 2L)) * hour, result.fullRateMillis)
        }
    }

    @Test
    fun unevenShiftOvertimeIsNotReplacedByRouteCountApproximation() {
        val result = calculateOvertimePremiumDurations(
            overtimeByShiftMillis = listOf(hour, 4 * hour, 0L),
            year = 2026,
            zeroBasedMonth = 8,
        )

        assertEquals(3 * hour, result.halfRateMillis)
        assertEquals(2 * hour, result.fullRateMillis)
    }

    @Test
    fun annual120HourBoundaryCanSplitHalfRatePartInsideShift() {
        val result = calculateOvertimePremiumDurations(
            overtimeByShiftMillis = listOf(4 * hour),
            year = 2026,
            zeroBasedMonth = 8,
            annualOvertimeBeforePeriodMillis = 119 * hour,
        )

        assertEquals(hour, result.halfRateMillis)
        assertEquals(3 * hour, result.fullRateMillis)
    }

    @Test
    fun afterAnnual120HoursAllOvertimeIsFullRateWithoutArtificial240HourCap() {
        val result = calculateOvertimePremiumDurations(
            overtimeByShiftMillis = listOf(130 * hour),
            year = 2026,
            zeroBasedMonth = 8,
            annualOvertimeBeforePeriodMillis = 120 * hour,
        )

        assertEquals(0L, result.halfRateMillis)
        assertEquals(130 * hour, result.fullRateMillis)
    }

    @Test
    fun annualBoundaryMatrixHasNoSecondHalfRateWindowAt240Hours() {
        val expectedHalfRateHours = mapOf(119L to 1L, 120L to 0L, 121L to 0L, 239L to 0L, 240L to 0L)
        expectedHalfRateHours.forEach { (alreadyWorked, expectedHalfRate) ->
            val result = calculateOvertimePremiumDurations(
                overtimeByShiftMillis = listOf(4 * hour),
                year = 2026,
                zeroBasedMonth = 8,
                annualOvertimeBeforePeriodMillis = alreadyWorked * hour,
            )
            assertEquals(expectedHalfRate * hour, result.halfRateMillis)
            assertEquals((4 - expectedHalfRate) * hour, result.fullRateMillis)
        }
    }

    @Test
    fun newCalendarYearStartsAnnualThresholdFromZero() {
        val result = calculateOvertimePremiumDurations(
            overtimeByShiftMillis = listOf(4 * hour),
            year = 2027,
            zeroBasedMonth = 0,
            annualOvertimeBeforePeriodMillis = 0L,
        )

        assertEquals(2 * hour, result.halfRateMillis)
        assertEquals(2 * hour, result.fullRateMillis)
    }

    @Test
    fun beforeSeptember2026PreservesAggregateHistoricalRule() {
        val result = calculateOvertimePremiumDurations(
            overtimeByShiftMillis = listOf(4 * hour, 0L),
            year = 2026,
            zeroBasedMonth = 7,
        )

        assertEquals(4 * hour, result.halfRateMillis)
        assertEquals(0L, result.fullRateMillis)
    }

    @Test
    fun overtimePremiumDurationsRejectNegativeInputs() {
        assertFailsWith<IllegalArgumentException> {
            calculateOvertimePremiumDurations(listOf(-1L), 2026, 8)
        }
        assertFailsWith<IllegalArgumentException> {
            calculateOvertimePremiumDurations(listOf(hour), 2026, 8, -1L)
        }
    }
}
