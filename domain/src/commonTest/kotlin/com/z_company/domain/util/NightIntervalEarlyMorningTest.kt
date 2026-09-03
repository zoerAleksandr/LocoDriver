package com.z_company.domain.util

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals

class NightIntervalEarlyMorningTest {
    private val moscow = TimeZone.of("GMT+3")

    private fun instant(day: Int, hour: Int): Long =
        LocalDateTime(2025, 1, day, hour, 0)
            .toInstant(moscow)
            .toEpochMilliseconds()

    @Test
    fun routeStartingAfterMidnightUsesNightWindowStartedPreviousDay() {
        val start = instant(day = 10, hour = 0)
        val end = instant(day = 10, hour = 8)

        val intervals = CalculateNightTime.getNightIntervals(
            startMillis = start,
            endMillis = end,
            hourStart = 22,
            minuteStart = 0,
            hourEnd = 6,
            minuteEnd = 0,
            offsetInMoscow = 0L,
        )

        assertEquals(listOf(start to instant(day = 10, hour = 6)), intervals)
        assertEquals(
            1,
            CalculateNightTime.getNightWindowsCount(
                startMillis = start,
                endMillis = end,
                hourStart = 22,
                minuteStart = 0,
                hourEnd = 6,
                minuteEnd = 0,
                offsetInMoscow = 0L,
            ),
        )
    }

    @Test
    fun routeStartingAtNightEndHasNoNightInterval() {
        val start = instant(day = 10, hour = 6)
        val end = instant(day = 10, hour = 8)

        assertEquals(
            emptyList(),
            CalculateNightTime.getNightIntervals(
                startMillis = start,
                endMillis = end,
                hourStart = 22,
                minuteStart = 0,
                hourEnd = 6,
                minuteEnd = 0,
                offsetInMoscow = 0L,
            ),
        )
    }
}
