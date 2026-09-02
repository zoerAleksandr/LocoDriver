package com.z_company.domain.util

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SalarySegmentTest {
    private val hour = 3_600_000L

    @Test
    fun generatedSplitsAlwaysPreserveDurationConditionsAndMoney() {
        val random = Random(91_152)
        repeat(500) {
            val start = random.nextLong(0, 10 * hour)
            val duration = random.nextLong(1, 24 * hour)
            val end = start + duration
            val rate = random.nextDouble(0.0, 2_000.0)
            val conditions = setOf(AccrualCondition.REGULAR, AccrualCondition.NIGHT)
            val segment = SalarySegment(TimeInterval(start, end), rate, conditions)
            val boundaries = List(8) { random.nextLong(start - hour, end + hour) }

            val parts = segment.splitAt(boundaries)

            assertEquals(duration, parts.sumOf { it.interval.durationMillis })
            assertEquals(segment.tariffMoney, parts.sumOf(SalarySegment::tariffMoney), 0.000_001)
            assertTrue(parts.all { it.conditions == conditions })
            assertTrue(parts.zipWithNext().all { (left, right) ->
                left.interval.endMillis == right.interval.startMillis
            })
        }
    }

    @Test
    fun generatedOverlappingExclusionsNeverDuplicateOrIncreasePayableTime() {
        val random = Random(91_153)
        repeat(500) {
            val duration = random.nextLong(1, 24 * hour)
            val rate = random.nextDouble(0.0, 2_000.0)
            val segment = SalarySegment(TimeInterval(0, duration), rate)
            val exclusions = List(12) {
                val first = random.nextLong(-hour, duration + hour)
                val second = random.nextLong(first + 1, duration + 2 * hour)
                TimeInterval(first, second)
            }

            val result = segment.subtract(exclusions)
            val remainingDuration = result.sumOf { it.interval.durationMillis }

            assertTrue(remainingDuration in 0..duration)
            assertTrue(result.zipWithNext().all { (left, right) ->
                left.interval.endMillis <= right.interval.startMillis
            })
            assertEquals(
                remainingDuration.toDouble() * rate / hour,
                result.sumOf(SalarySegment::tariffMoney),
                0.000_001,
            )
        }
    }

    @Test
    fun tariffChangeUsesOldRateBeforeBoundaryAndNewRateFromBoundary() {
        val segments = TimeInterval(0, 10 * hour).applyTariffChanges(
            initialTariffRatePerHour = 100.0,
            changes = listOf(TariffChange(4 * hour, 150.0)),
        )

        assertEquals(
            listOf(
                SalarySegment(TimeInterval(0, 4 * hour), 100.0),
                SalarySegment(TimeInterval(4 * hour, 10 * hour), 150.0),
            ),
            segments,
        )
        assertEquals(1_300.0, segments.sumOf(SalarySegment::tariffMoney), 0.001)
    }

    @Test
    fun changeAtStartAppliesToWholeInterval() {
        val segments = TimeInterval(10, 20).applyTariffChanges(
            initialTariffRatePerHour = 100.0,
            changes = listOf(TariffChange(10, 200.0)),
        )
        assertEquals(listOf(SalarySegment(TimeInterval(10, 20), 200.0)), segments)
    }

    @Test
    fun changeAtEndDoesNotCreateEmptySegment() {
        val segments = TimeInterval(10, 20).applyTariffChanges(
            initialTariffRatePerHour = 100.0,
            changes = listOf(TariffChange(20, 200.0)),
        )
        assertEquals(listOf(SalarySegment(TimeInterval(10, 20), 100.0)), segments)
    }

    @Test
    fun additionalSplitWithoutConditionChangePreservesMoney() {
        val whole = SalarySegment(TimeInterval(0, 8 * hour), 125.0)
        val parts = whole.splitAt(listOf(2 * hour, 5 * hour))
        assertEquals(whole.tariffMoney, parts.sumOf(SalarySegment::tariffMoney), 0.001)
    }

    @Test
    fun rejectsNegativeNanAndInfiniteRates() {
        listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> {
                SalarySegment(TimeInterval(0, hour), invalid)
            }
        }
    }

    @Test
    fun preservesAccrualConditionsAcrossSplit() {
        val conditions = setOf(AccrualCondition.NIGHT, AccrualCondition.OVERTIME)
        val parts = SalarySegment(TimeInterval(0, 2 * hour), 100.0, conditions)
            .splitAt(listOf(hour))
        assertEquals(listOf(conditions, conditions), parts.map(SalarySegment::conditions))
    }

    @Test
    fun conditionOutsideSegmentDoesNotSplitOrTagIt() {
        val segment = SalarySegment(TimeInterval(2 * hour, 4 * hour), 100.0)

        assertEquals(
            listOf(segment),
            segment.applyCondition(
                AccrualCondition.BUSINESS_TRIP,
                listOf(TimeInterval(0, hour), TimeInterval(5 * hour, 6 * hour)),
            ),
        )
    }

    @Test
    fun conditionCoveringWholeSegmentTagsItWithoutExtraParts() {
        val segment = SalarySegment(TimeInterval(2 * hour, 4 * hour), 100.0)
        val result = segment.applyCondition(
            AccrualCondition.BUSINESS_TRIP,
            listOf(TimeInterval(hour, 5 * hour)),
        )

        assertEquals(1, result.size)
        assertTrue(AccrualCondition.BUSINESS_TRIP in result.single().conditions)
    }

    @Test
    fun partialConditionSplitsAtBothBoundariesAndTagsOnlyOverlap() {
        val result = SalarySegment(TimeInterval(0, 6 * hour), 100.0).applyCondition(
            AccrualCondition.BUSINESS_TRIP,
            listOf(TimeInterval(2 * hour, 4 * hour)),
        )

        assertEquals(
            listOf(
                TimeInterval(0, 2 * hour),
                TimeInterval(2 * hour, 4 * hour),
                TimeInterval(4 * hour, 6 * hour),
            ),
            result.map(SalarySegment::interval),
        )
        assertEquals(
            listOf(false, true, false),
            result.map { AccrualCondition.BUSINESS_TRIP in it.conditions },
        )
    }

    @Test
    fun overlappingConditionIntervalsAreMergedWithoutDuplicateSegments() {
        val result = SalarySegment(TimeInterval(0, 8 * hour), 100.0).applyCondition(
            AccrualCondition.NIGHT,
            listOf(
                TimeInterval(hour, 4 * hour),
                TimeInterval(3 * hour, 6 * hour),
                TimeInterval(6 * hour, 7 * hour),
            ),
        )

        assertEquals(
            listOf(
                TimeInterval(0, hour),
                TimeInterval(hour, 7 * hour),
                TimeInterval(7 * hour, 8 * hour),
            ),
            result.map(SalarySegment::interval),
        )
        assertEquals(8 * hour, result.sumOf { it.interval.durationMillis })
        assertEquals(800.0, result.sumOf(SalarySegment::tariffMoney), 0.001)
    }

    @Test
    fun applyingConditionsSequentiallyPreservesExistingReasons() {
        val result = listOf(SalarySegment(TimeInterval(0, 4 * hour), 100.0))
            .applyCondition(AccrualCondition.NIGHT, listOf(TimeInterval(0, 2 * hour)))
            .applyCondition(AccrualCondition.BUSINESS_TRIP, listOf(TimeInterval(hour, 3 * hour)))

        assertEquals(
            listOf(
                setOf(AccrualCondition.REGULAR, AccrualCondition.NIGHT),
                setOf(
                    AccrualCondition.REGULAR,
                    AccrualCondition.NIGHT,
                    AccrualCondition.BUSINESS_TRIP,
                ),
                setOf(AccrualCondition.REGULAR, AccrualCondition.BUSINESS_TRIP),
                setOf(AccrualCondition.REGULAR),
            ),
            result.map(SalarySegment::conditions),
        )
        assertEquals(4 * hour, result.sumOf { it.interval.durationMillis })
    }

    @Test
    fun exclusionInsideSegmentRemovesOnlyItsOverlap() {
        val conditions = setOf(AccrualCondition.REGULAR, AccrualCondition.NIGHT)
        val result = SalarySegment(TimeInterval(0, 6 * hour), 120.0, conditions)
            .subtract(listOf(TimeInterval(2 * hour, 4 * hour)))

        assertEquals(
            listOf(TimeInterval(0, 2 * hour), TimeInterval(4 * hour, 6 * hour)),
            result.map(SalarySegment::interval),
        )
        assertEquals(listOf(conditions, conditions), result.map(SalarySegment::conditions))
        assertEquals(480.0, result.sumOf(SalarySegment::tariffMoney), 0.001)
    }

    @Test
    fun exclusionPartiallyOutsideSegmentIsClipped() {
        val result = SalarySegment(TimeInterval(2 * hour, 6 * hour), 100.0)
            .subtract(listOf(TimeInterval(0, 3 * hour)))

        assertEquals(
            listOf(SalarySegment(TimeInterval(3 * hour, 6 * hour), 100.0)),
            result,
        )
    }

    @Test
    fun exclusionOutsideSegmentDoesNotChangeIt() {
        val segment = SalarySegment(TimeInterval(2 * hour, 6 * hour), 100.0)

        assertEquals(
            listOf(segment),
            segment.subtract(listOf(TimeInterval(0, hour), TimeInterval(7 * hour, 8 * hour))),
        )
    }

    @Test
    fun exclusionCoveringSegmentRemovesItCompletely() {
        val result = SalarySegment(TimeInterval(2 * hour, 6 * hour), 100.0)
            .subtract(listOf(TimeInterval(hour, 7 * hour)))

        assertTrue(result.isEmpty())
    }

    @Test
    fun overlappingExclusionsAreMergedBeforeSubtraction() {
        val result = SalarySegment(TimeInterval(0, 10 * hour), 100.0).subtract(
            listOf(
                TimeInterval(hour, 4 * hour),
                TimeInterval(3 * hour, 6 * hour),
                TimeInterval(8 * hour, 9 * hour),
            ),
        )

        assertEquals(
            listOf(
                TimeInterval(0, hour),
                TimeInterval(6 * hour, 8 * hour),
                TimeInterval(9 * hour, 10 * hour),
            ),
            result.map(SalarySegment::interval),
        )
        assertEquals(4 * hour, result.sumOf { it.interval.durationMillis })
    }

    @Test
    fun segmentPipelineCombinesBreakTariffChangeAndNightBoundaries() {
        val result = buildSalarySegments(
            workIntervals = listOf(TimeInterval(0, 8 * hour)),
            unpaidIntervals = listOf(TimeInterval(3 * hour, 4 * hour)),
            initialTariffRatePerHour = 100.0,
            tariffChanges = listOf(TariffChange(5 * hour, 200.0)),
            conditionIntervals = mapOf(
                AccrualCondition.NIGHT to listOf(TimeInterval(2 * hour, 6 * hour)),
            ),
        )

        assertEquals(
            listOf(
                TimeInterval(0, 2 * hour),
                TimeInterval(2 * hour, 3 * hour),
                TimeInterval(4 * hour, 5 * hour),
                TimeInterval(5 * hour, 6 * hour),
                TimeInterval(6 * hour, 8 * hour),
            ),
            result.map(SalarySegment::interval),
        )
        assertEquals(listOf(100.0, 100.0, 100.0, 200.0, 200.0), result.map { it.tariffRatePerHour })
        assertEquals(listOf(false, true, true, true, false), result.map {
            AccrualCondition.NIGHT in it.conditions
        })
        assertEquals(7 * hour, result.sumOf { it.interval.durationMillis })
        assertEquals(1_000.0, result.sumOf(SalarySegment::tariffMoney), 0.001)
    }

    @Test
    fun segmentPipelineFeedsOvertimeWithoutLosingActualConditionOrTariff() {
        val work = buildSalarySegments(
            workIntervals = listOf(TimeInterval(0, 8 * hour)),
            unpaidIntervals = listOf(TimeInterval(3 * hour, 4 * hour)),
            initialTariffRatePerHour = 100.0,
            tariffChanges = listOf(TariffChange(5 * hour, 200.0)),
            conditionIntervals = mapOf(
                AccrualCondition.NIGHT to listOf(TimeInterval(2 * hour, 6 * hour)),
            ),
        )
        val overtime = selectLatestOvertimeSegments(work, 3 * hour)
        val result = calculateOvertimeBreakdown(
            segments = overtime,
            halfRateDurationMillis = 2 * hour,
            conditionPercents = mapOf(AccrualCondition.NIGHT to 40.0),
        )

        assertEquals(listOf(TimeInterval(5 * hour, 6 * hour), TimeInterval(6 * hour, 8 * hour)),
            overtime.map(SalarySegment::interval))
        assertEquals(600.0, result.ordinaryTariffMoney, 0.001)
        assertEquals(240.0, result.halfRateExtraMoney, 0.001)
        assertEquals(200.0, result.fullRateExtraMoney, 0.001)
    }

    @Test
    fun segmentPipelineRejectsOverlappingWorkIntervals() {
        assertFailsWith<IllegalArgumentException> {
            buildSalarySegments(
                workIntervals = listOf(TimeInterval(0, 2 * hour), TimeInterval(hour, 3 * hour)),
                initialTariffRatePerHour = 100.0,
            )
        }
    }
}
