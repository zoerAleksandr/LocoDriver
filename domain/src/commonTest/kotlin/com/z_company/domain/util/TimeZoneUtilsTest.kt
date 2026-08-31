package com.z_company.domain.util

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TimeZoneUtilsTest {
    @Test
    fun wholeHourOffsetsKeepExistingFormat() {
        assertEquals("GMT+3", getTimeZone(0L))
        assertEquals("GMT+0", getTimeZone(-3 * 3_600_000L))
        assertEquals("GMT-3", getTimeZone(-6 * 3_600_000L))
    }

    @Test
    fun halfAndQuarterHourOffsetsPreserveMinutes() {
        // Настройка хранит смещение относительно Москвы (UTC+3).
        assertEquals("GMT+05:30", getTimeZone(2 * 3_600_000L + 30 * 60_000L))
        assertEquals("GMT+05:45", getTimeZone(2 * 3_600_000L + 45 * 60_000L))
        assertEquals("GMT-03:30", getTimeZone(-6 * 3_600_000L - 30 * 60_000L))

        // Получившиеся идентификаторы обязаны приниматься kotlinx-datetime.
        assertNotNull(TimeZone.of("GMT+05:30"))
        assertNotNull(TimeZone.of("GMT+05:45"))
    }
}
