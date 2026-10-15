package com.z_company.loco_driver.widget

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * TextClock в поясе [WidgetRender.elapsedClockZone] должен показывать «Ч:ММ» прошедшего с явки времени.
 */
class ElapsedClockZoneTest {

    private fun clockText(zoneId: String, now: Long): String {
        val format = SimpleDateFormat("H:mm", Locale.US)
        format.timeZone = TimeZone.getTimeZone(zoneId)
        return format.format(Date(now))
    }

    private fun elapsedText(start: Long, now: Long): String {
        val minutes = (now - start) / 60_000L
        return "%d:%02d".format(minutes / 60, minutes % 60)
    }

    @Test
    fun clockShowsElapsedTimeForVariousStarts() {
        val minute = 60_000L
        // явка на минутной границе — разные «времена суток» и длительности смены до 23:59
        val starts = listOf(
            1_760_000_000_000L - 1_760_000_000_000L % minute,
            1_760_000_000_000L - 1_760_000_000_000L % minute + 7 * 3_600_000L + 13 * minute,
            1_760_000_000_000L - 1_760_000_000_000L % minute + 19 * 3_600_000L + 59 * minute,
        )
        for (start in starts) {
            for (elapsedMin in listOf(0L, 1L, 59L, 60L, 61L, 4 * 60L + 53, 12 * 60L, 23 * 60L + 59)) {
                val now = start + elapsedMin * minute
                val zone = WidgetRender.elapsedClockZone(start)
                assertEquals(elapsedText(start, now), clockText(zone, now), "start=$start elapsed=$elapsedMin zone=$zone")
            }
        }
    }
}
