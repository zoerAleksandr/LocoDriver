package com.z_company.domain.track

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LiveProfileTest {
    private val lat0 = 55.0
    private val lon0 = 37.0
    private val kx = FlatMetric.kx(lat0)

    // Ось 10 км на восток, пикет: на 0 км ось = 100 км, нумерация растёт
    private fun section(withMarks: Boolean = true): SectionProfile {
        val pts = (0..100).map { AxisPoint(it * 0.1, lat0, lon0 + it * 100.0 / kx) }
        return SectionProfile(
            id = "s", name = "А — Б", fromStation = "А", toStation = "Б", lengthKm = 10.0, axisHash = "h",
            axis = SectionAxis("s", "h", pts),
            elements = listOf(ProfileElement(0.0, 5.0, 4.0), ProfileElement(5.0, 10.0, -2.0)),
            kmMarks = if (withMarks) listOf(KmMark(0.0, 100.0, 1)) else emptyList(),
            stations = listOf(SectionStation(0.0, "Александровка"), SectionStation(10.0, "Борки")),
        )
    }

    private fun lon(km: Double) = lon0 + km * 1000.0 / kx

    @Test
    fun forward_position_has_km_text_and_element() {
        val lp = LiveProfile(section(), reversed = false)
        val p = lp.onFix(1_000, lat0, lon(2.05), 60.0, 5f)
        assertNotNull(p)
        assertEquals(2.05, p.kmAxis, 0.01)
        assertEquals("102 км пк 1 +50 м", p.kmText)
        assertEquals(4.0, p.element?.gradePermille)
        assertTrue(p.locked && !p.offRoute)
    }

    @Test
    fun no_marks_gives_null_text() {
        val lp = LiveProfile(section(withMarks = false), reversed = false)
        assertNull(lp.onFix(1_000, lat0, lon(1.0), 60.0, 5f)?.kmText)
    }

    @Test
    fun reversed_flips_view_and_grade() {
        val lp = LiveProfile(section(), reversed = true)
        lp.onFix(1_000, lat0, lon(2.0), 60.0, 5f)
        assertEquals(8.0, lp.toView(lp.position!!.kmAxis), 0.01)
        assertEquals(-4.0, lp.gradeInView(ProfileElement(0.0, 1.0, 4.0)))
        assertEquals(2.0, lp.toAxis(8.0), 1e-9)
    }

    @Test
    fun window_clamped_to_edges_keeps_width() {
        val lp = LiveProfile(section(), reversed = false)
        lp.onFix(1_000, lat0, lon(0.2), 60.0, 5f)
        assertEquals(ProfileWindow(0.0, 6.0), lp.window(1.0, 5.0))
        val lp2 = LiveProfile(section(), reversed = false)
        lp2.onFix(1_000, lat0, lon(9.5), 60.0, 5f)
        assertEquals(ProfileWindow(4.0, 10.0), lp2.window(1.0, 5.0))
    }

    @Test
    fun far_from_axis_is_off_route_after_three_fixes() {
        val lp = LiveProfile(section(), reversed = false)
        lp.onFix(1_000, lat0, lon(1.0), 60.0, 5f)
        var p: LivePosition? = null
        for (i in 0 until 3) p = lp.onFix(2_000L + i * 1_000, lat0 + 0.01, lon(1.0), 60.0, 5f)
        assertTrue(p!!.offRoute)
    }

    @Test
    fun leg_match_and_direction() {
        val s = section()
        assertEquals(false, LegMatch.reversed(s, listOf("александровка", "Борки")))
        assertEquals(true, LegMatch.reversed(s, listOf("Борки", "Промежуточная", "Александровка")))
        assertNull(LegMatch.reversed(s, listOf("Борки")))
        assertNull(LegMatch.reversed(s, listOf("Нет такой", "Борки")))
        assertEquals(true, LegMatch.fits(s, listOf("Борки", "Александровка")))
        assertEquals(false, LegMatch.fits(s, listOf("Москва", "Борки")))
        assertNull(LegMatch.fits(s, emptyList()))
        assertEquals(listOf("s"), LegMatch.sections(listOf(s), listOf("Александровка", "Борки")).map { it.id })
    }
}
