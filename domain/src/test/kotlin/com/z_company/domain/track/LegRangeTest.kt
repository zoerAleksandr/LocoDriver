package com.z_company.domain.track

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LegRangeTest {
    private val section = SectionProfile(
        id = "a", name = "Уч", fromStation = "", toStation = "", lengthKm = 100.0, axisHash = "h",
        axis = SectionAxis("a", "h", listOf(AxisPoint(0.0, 60.0, 30.0), AxisPoint(100.0, 61.0, 31.0))),
        elements = emptyList(), kmMarks = emptyList(),
        stations = listOf(
            SectionStation(0.0, "Начало"), SectionStation(20.0, "Б"), SectionStation(70.0, "В"), SectionStation(100.0, "Конец"),
        ),
    )

    @Test
    fun leg_inside_section_gives_its_range_in_any_direction() {
        val r = assertNotNull(LegMatch.axisRange(section, listOf("Б", "В")))
        assertEquals(20.0, r.start, 1e-9); assertEquals(70.0, r.endInclusive, 1e-9)
        val back = assertNotNull(LegMatch.axisRange(section, listOf("В", "Б")))
        assertEquals(20.0, back.start, 1e-9); assertEquals(70.0, back.endInclusive, 1e-9)
    }

    @Test
    fun whole_section_unknown_or_missing_leg_gives_null() {
        assertNull(LegMatch.axisRange(section, listOf("Начало", "Конец")))
        assertNull(LegMatch.axisRange(section, listOf("Б")))
        assertNull(LegMatch.axisRange(section, listOf("Б", "Нет такой")))
        assertNull(LegMatch.axisRange(section, listOf("Б", "Б")))
    }

    @Test
    fun window_stays_inside_leg_range() {
        val live = LiveProfile(section, reversed = false).also { it.legAxisRange = 20.0..70.0 }
        assertEquals(20.0, live.viewFrom, 1e-9); assertEquals(70.0, live.viewTo, 1e-9); assertEquals(50.0, live.viewSpanKm, 1e-9)
        val w = live.window(behindKm = 10.0, aheadKm = 20.0)
        assertEquals(20.0, w.fromX, 1e-9); assertEquals(50.0, w.toX, 1e-9)
        val wide = live.window(behindKm = 80.0, aheadKm = 80.0)
        assertEquals(20.0, wide.fromX, 1e-9); assertEquals(70.0, wide.toX, 1e-9)
    }

    @Test
    fun reversed_view_maps_range_from_the_other_end() {
        val live = LiveProfile(section, reversed = true).also { it.legAxisRange = 20.0..70.0 }
        // в виде ось развёрнута: 100 - 70 .. 100 - 20
        assertEquals(30.0, live.viewFrom, 1e-9); assertEquals(80.0, live.viewTo, 1e-9)
    }

    @Test
    fun without_range_whole_section_is_shown() {
        val live = LiveProfile(section, reversed = false)
        assertEquals(0.0, live.viewFrom, 1e-9); assertEquals(100.0, live.viewTo, 1e-9)
    }
}
