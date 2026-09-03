package com.z_company.domain.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class TimeIntervalTest {
    @Test
    fun rejectsReversedInterval() {
        assertFailsWith<IllegalArgumentException> { TimeInterval(2, 1) }
    }

    @Test
    fun touchingHalfOpenIntervalsDoNotOverlap() {
        assertNull(TimeInterval(0, 10).intersect(TimeInterval(10, 20)))
    }

    @Test
    fun intersectionReturnsOnlySharedDuration() {
        assertEquals(TimeInterval(5, 10), TimeInterval(0, 10).intersect(TimeInterval(5, 15)))
    }

    @Test
    fun subtractSplitsIntervalIntoTwoParts() {
        assertEquals(
            listOf(TimeInterval(0, 3), TimeInterval(7, 10)),
            TimeInterval(0, 10).subtract(TimeInterval(3, 7)),
        )
    }

    @Test
    fun subtractAllMergesOverlappingExclusions() {
        assertEquals(
            listOf(TimeInterval(0, 2), TimeInterval(8, 10)),
            TimeInterval(0, 10).subtractAll(
                listOf(TimeInterval(2, 6), TimeInterval(4, 8)),
            ),
        )
    }

    @Test
    fun mergeIsOrderIndependentAndJoinsTouchingIntervals() {
        assertEquals(
            listOf(TimeInterval(0, 10), TimeInterval(12, 14)),
            listOf(
                TimeInterval(5, 10),
                TimeInterval(12, 14),
                TimeInterval(0, 5),
            ).mergeTimeIntervals(),
        )
    }

    @Test
    fun durationsRemainAdditiveAfterSplit() {
        val parts = TimeInterval(0, 10).subtract(TimeInterval(4, 6))
        assertEquals(8, parts.sumOf(TimeInterval::durationMillis))
    }

    @Test
    fun splitIgnoresDuplicateAndOutsideBoundaries() {
        assertEquals(
            listOf(TimeInterval(0, 3), TimeInterval(3, 7), TimeInterval(7, 10)),
            TimeInterval(0, 10).splitAt(listOf(-1, 3, 7, 3, 10, 20)),
        )
    }

    @Test
    fun splitPreservesTotalDurationWithoutOverlap() {
        val original = TimeInterval(100, 1_000)
        val parts = original.splitAt(listOf(250, 500, 750))

        assertEquals(original.durationMillis, parts.sumOf(TimeInterval::durationMillis))
        assertEquals(parts.dropLast(1).map(TimeInterval::endMillis), parts.drop(1).map(TimeInterval::startMillis))
    }
}
