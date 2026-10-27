package com.z_company.domain.track

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WarningPassTrackerTest {
    private fun speed(from: Double, to: Double, type: WarningType = WarningType.SPEED) = ResolvedWarning(
        TrackWarning("w", "s", null, null, from, to, type, if (type == WarningType.SPEED) 40 else null, "", "ПЕР"),
        from, to,
    )

    @Test
    fun speed_limit_enter_by_head_exit_by_tail() {
        val t = WarningPassTracker()
        val r = speed(10.0, 11.0)
        val len = 0.8 // поезд 800 м
        assertEquals(emptyList(), t.update(r, 9.0, 1, len))
        assertEquals(listOf(WarningEvent.ENTER), t.update(r, 10.05, 1, len))
        assertEquals(emptyList(), t.update(r, 10.9, 1, len))
        // голова за зоной, хвост ещё внутри (11.2 - 0.8 = 10.4)
        assertEquals(emptyList(), t.update(r, 11.2, 1, len))
        assertEquals(emptyList(), t.update(r, 11.7, 1, len))
        assertEquals(listOf(WarningEvent.EXIT), t.update(r, 11.8, 1, len))
        assertEquals(emptyList(), t.update(r, 12.5, 1, len))
    }

    @Test
    fun reverse_direction_mirrors() {
        val t = WarningPassTracker()
        val r = speed(10.0, 11.0)
        assertEquals(emptyList(), t.update(r, 12.0, -1, 0.5))
        assertEquals(listOf(WarningEvent.ENTER), t.update(r, 10.95, -1, 0.5))
        assertEquals(emptyList(), t.update(r, 9.8, -1, 0.5))
        assertEquals(listOf(WarningEvent.EXIT), t.update(r, 9.5, -1, 0.5))
    }

    @Test
    fun other_types_only_enter() {
        val t = WarningPassTracker()
        val r = speed(10.0, 11.0, WarningType.PANTOGRAPH)
        assertEquals(emptyList(), t.update(r, 9.0, 1, 0.5))
        assertEquals(listOf(WarningEvent.ENTER), t.update(r, 10.1, 1, 0.5))
        assertEquals(emptyList(), t.update(r, 11.9, 1, 0.5))
    }

    @Test
    fun short_zone_jump_gives_both_events_and_no_length_means_head_exit() {
        val t = WarningPassTracker()
        val r = speed(10.0, 10.1)
        assertEquals(emptyList(), t.update(r, 9.9, 1, null))
        assertEquals(listOf(WarningEvent.ENTER, WarningEvent.EXIT), t.update(r, 10.2, 1, null))
    }

    @Test
    fun first_sight_inside_announces_enter_after_zone_silent() {
        val a = WarningPassTracker()
        assertEquals(listOf(WarningEvent.ENTER), a.update(speed(10.0, 11.0), 10.5, 1, 0.3))
        val b = WarningPassTracker()
        assertEquals(emptyList(), b.update(speed(10.0, 11.0), 12.0, 1, 0.3))
        assertEquals(emptyList(), b.update(speed(10.0, 11.0), 13.0, 1, 0.3))
    }

    @Test
    fun reset_starts_over_and_texts() {
        val t = WarningPassTracker()
        val r = speed(10.0, 11.0)
        t.update(r, 10.5, 1, 0.2)
        t.reset()
        assertEquals(listOf(WarningEvent.ENTER), t.update(r, 10.6, 1, 0.2))
        assertEquals("Начало ограничения скорости 40 километров в час", r.warning.spokenEnter())
        assertEquals("Конец ограничения скорости 40 километров в час", r.warning.spokenExit())
        val p = speed(10.0, 11.0, WarningType.PANTOGRAPH).warning
        assertEquals("Опустите токоприёмники", p.spokenEnter())
        assertNull(p.spokenExit())
    }
}
