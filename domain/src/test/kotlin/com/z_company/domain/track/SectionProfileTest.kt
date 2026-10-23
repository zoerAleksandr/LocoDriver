package com.z_company.domain.track

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SectionProfileTest {
    private fun near(expected: Double, actual: Double?, eps: Double = 1e-6) {
        assertNotNull(actual)
        assertTrue(kotlin.math.abs(expected - actual) < eps, "expected $expected, got $actual")
    }

    @Test
    fun pickets_match_server_text() {
        assertEquals("142 км пк 5 +30 м", KmMarks.position(142.43).text())
        assertEquals("142 км пк 1", KmMarks.position(142.0).text())
        assertEquals("143 км пк 1", KmMarks.position(142.9996).text())
        assertEquals("5 км пк 10 +99 м", KmMarks.position(5.999).text())
    }

    @Test
    fun marks_increasing_and_renumber() {
        val marks = listOf(KmMark(0.0, 100.0, 1), KmMark(10.0, 4.0, -1))
        near(105.0, KmMarks.toAbs(marks, 5.0))
        near(4.0, KmMarks.toAbs(marks, 10.0))
        near(1.0, KmMarks.toAbs(marks, 13.0))
        assertNull(KmMarks.toAbs(emptyList(), 1.0))
    }

    @Test
    fun marks_before_first_continue_backwards() {
        val marks = listOf(KmMark(2.0, 50.0, 1))
        near(49.0, KmMarks.toAbs(marks, 1.0))
    }

    @Test
    fun toRel_inverse_of_toAbs() {
        val marks = listOf(KmMark(0.0, 100.0, 1), KmMark(10.0, 4.0, -1))
        near(5.0, KmMarks.toRel(marks, 105.0))
        near(13.0, KmMarks.toRel(marks, 1.0))
        assertNull(KmMarks.toRel(marks, 500.0))
    }

    @Test
    fun elevation_integrates_grades() {
        val line = ElevationLine.fromElements(
            listOf(ProfileElement(0.0, 2.0, 10.0), ProfileElement(2.0, 3.0, -5.0), ProfileElement(4.0, 5.0, 0.0)),
        )
        near(20.0, line.heightAt(2.0))
        near(15.0, line.heightAt(3.0))
        near(15.0, line.heightAt(3.5)) // зазор - уклон 0
        near(10.0, line.heightAt(1.0))
        near(15.0, line.heightAt(99.0))
        assertEquals(0.0, line.minHeight)
        assertEquals(20.0, line.maxHeight)
    }

    @Test
    fun element_lookup() {
        val e = listOf(ProfileElement(0.0, 2.0, 1.0), ProfileElement(2.0, 3.0, 2.0))
        assertEquals(1.0, e.at(0.5)?.gradePermille)
        assertEquals(2.0, e.at(2.5)?.gradePermille)
        assertNull(e.at(4.0))
    }

    private fun axis(id: String, lat0: Double, lon0: Double): SectionAxis {
        // ось ~3 км на восток от (lat0, lon0)
        val kx = FlatMetric.kx(lat0)
        val pts = (0..30).map { AxisPoint(it * 0.1, lat0, lon0 + it * 100.0 / kx) }
        return SectionAxis(id, "hash$id", pts)
    }

    @Test
    fun nearest_sorted_by_distance_and_filters_far() {
        val a = axis("a", 59.9, 30.3)
        val b = axis("b", 59.9005, 30.3) // ~55 м севернее
        val far = axis("far", 55.0, 37.0)
        val res = SectionPicker.nearest(listOf(far, b, a), 59.9, 30.31)
        assertEquals(listOf("a", "b"), res.map { it.sectionId })
        assertTrue(res[0].distanceM < 5.0)
        near(0.01 * FlatMetric.kx(59.9) / 1000.0, res[0].kmOnAxis, 0.01)
    }

    @Test
    fun station_names_compare_loosely() {
        assertTrue(SectionPicker.sameStation("Санкт-Петербург-Глав.", "санкт петербург глав"))
        assertTrue(SectionPicker.sameStation("Ёлкино", "елкино"))
        assertTrue(!SectionPicker.sameStation("", ""))
    }

    @Test
    fun ticks_increasing_numbering() {
        val marks = listOf(KmMark(0.0, 142.0, 1))
        val t = KmGrid.ticks(marks, 0.25, 0.75)
        assertEquals(listOf(142.3, 142.4, 142.5, 142.6, 142.7), t.map { it.absKm })
        near(0.3, t[0].axisKm)
        assertTrue(t.none { it.isPost })
        val withPost = KmGrid.ticks(marks, 0.9, 1.1)
        assertEquals(listOf(true), withPost.filter { it.absKm == 143.0 }.map { it.isPost })
    }

    @Test
    fun ticks_decreasing_numbering_and_renumber() {
        val marks = listOf(KmMark(0.0, 100.0, 1), KmMark(10.0, 4.0, -1))
        val t = KmGrid.ticks(marks, 9.8, 10.3)
        assertTrue(t.any { it.absKm == 109.9 })
        assertTrue(t.any { it.absKm == 3.9 })
        assertTrue(t.zipWithNext().all { (a, b) -> a.axisKm <= b.axisKm })
    }

    @Test
    fun cells_have_km_and_picket() {
        val marks = listOf(KmMark(0.0, 142.0, 1))
        val c = KmGrid.cells(marks, 0.0, 1.05)
        assertEquals(142, c.first().km); assertEquals(1, c.first().picket)
        assertEquals(142, c.first { it.picket == 10 }.km)
        assertEquals(1, c.first { it.km == 143 }.picket)
        val down = KmGrid.cells(listOf(KmMark(0.0, 143.0, -1)), 0.0, 0.25)
        assertEquals(142, down.first().km); assertEquals(10, down.first().picket)
    }

    @Test
    fun box_distance_and_parsing() {
        val box = SectionBox.of(listOf(55.0, 37.0, 55.1, 37.2))!!
        assertEquals(0.0, box.distanceM(55.05, 37.1), 1e-9)
        // севернее на 0,01° = ~1105 м
        near(1105.4, box.distanceM(55.11, 37.1), 1.0)
        // восточнее на 0,01° на широте 55,05 = ~640 м
        near(0.01 * FlatMetric.kx(55.05), box.distanceM(55.05, 37.21), 1.0)
        assertNull(SectionBox.of(null))
        assertNull(SectionBox.of(listOf(1.0, 2.0, 3.0)))
        assertNull(SectionBox.of(listOf(56.0, 37.0, 55.0, 38.0)))
        assertNull(SectionBox.of(listOf(Double.NaN, 37.0, 55.0, 38.0)))
    }

    @Test
    fun no_data_zone_keeps_height_and_hasProfile_needs_real_data() {
        val line = ElevationLine.fromElements(
            listOf(ProfileElement(0.0, 1.0, 10.0), ProfileElement(1.0, 3.0, 99.0, noData = true), ProfileElement(3.0, 4.0, -5.0)),
        )
        near(10.0, line.heightAt(1.0)); near(10.0, line.heightAt(3.0)); near(5.0, line.heightAt(4.0))
        val axis = SectionAxis("s", "h", listOf(AxisPoint(0.0, 55.0, 37.0), AxisPoint(1.0, 55.0, 37.01)))
        fun sec(els: List<ProfileElement>) = SectionProfile("s", "n", "a", "b", 4.0, "h", axis, els, emptyList(), emptyList())
        assertTrue(sec(listOf(ProfileElement(0.0, 1.0, 1.0))).hasProfile)
        assertTrue(!sec(listOf(ProfileElement(0.0, 1.0, 0.0, noData = true))).hasProfile)
    }
}
