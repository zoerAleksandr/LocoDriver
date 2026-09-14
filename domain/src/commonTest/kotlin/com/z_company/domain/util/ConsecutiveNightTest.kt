package com.z_company.domain.util

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConsecutiveNightTest {
    private val moscow = TimeZone.of("GMT+3")
    private fun at(day: Int, hour: Int) =
        LocalDateTime(2026, 1, day, hour, 0).toInstant(moscow).toEpochMilliseconds()

    private fun periods(
        previousStart: Long?, previousEnd: Long?, turnover: Boolean,
        currentStart: Long, currentEnd: Long,
    ) = CalculateNightTime.getConsecutiveNightPeriods(
        previousStart, previousEnd, turnover, currentStart, currentEnd, 0L
    )

    @Test
    fun daytimeHomeRestDoesNotBreakTwoWorkingNights() {
        val result = periods(at(1, 22), at(2, 3), false, at(2, 22), at(3, 3))
        assertEquals(2, result.size)
        assertTrue(result.all { it.source == CalculateNightTime.ConsecutiveNightSource.WORK })
    }

    @Test
    fun turnoverRestDuringNightCountsAsPreviousNight() {
        val result = periods(at(1, 8), at(1, 20), true, at(2, 13), at(3, 1))
        assertEquals(2, result.size)
        assertEquals(CalculateNightTime.ConsecutiveNightSource.TURNOVER_REST, result.first().source)
        assertEquals(5L * 3_600_000L, result.first().intervals.sumOf { it.second - it.first })
    }

    @Test
    fun homeRestDuringNightDoesNotCountAsNight() {
        assertTrue(periods(at(1, 8), at(1, 20), false, at(2, 13), at(3, 1)).isEmpty())
    }

    @Test
    fun nonAdjacentWorkingNightsDoNotFormSequence() {
        assertTrue(periods(at(1, 22), at(2, 3), false, at(3, 22), at(4, 3)).isEmpty())
    }

    @Test
    fun oneLongRouteCanContainTwoNights() {
        val result = periods(null, null, false, at(1, 22), at(3, 3))
        assertEquals(1, result.size)
        assertEquals(2, result.single().intervals.size)
    }
}
