package com.z_company.route.ui.settings

import com.z_company.route.viewmodel.TrackHistoryStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrackHistoryFormatTest {
    @Test
    fun durationReadsLikeHumans() {
        assertEquals("3 ч 20 мин", TrackHistoryFormat.duration((3 * 60 + 20) * 60_000L))
        assertEquals("2 ч", TrackHistoryFormat.duration(2 * 3_600_000L))
        assertEquals("45 мин", TrackHistoryFormat.duration(45 * 60_000L))
        assertEquals("1 мин", TrackHistoryFormat.duration(5_000L))
    }

    @Test
    fun distanceKeepsDecimalOnlyForShortTracks() {
        assertEquals("5,4 км", TrackHistoryFormat.distance(5.43))
        assertEquals("132 км", TrackHistoryFormat.distance(132.7))
    }

    @Test
    fun dateUsesDisplayZone() {
        // 2026-10-12 05:15 UTC = 08:15 МСК
        val ms = 1_791_782_100_000L
        assertTrue(TrackHistoryFormat.dateTime(ms, "GMT+3").endsWith("08:15"))
        assertTrue(TrackHistoryFormat.dateTime(ms, "GMT+5").endsWith("10:15"))
    }

    @Test
    fun everyStatusHasUserText() {
        TrackHistoryStatus.entries.forEach { assertTrue(TrackHistoryFormat.status(it).isNotBlank()) }
    }
}
