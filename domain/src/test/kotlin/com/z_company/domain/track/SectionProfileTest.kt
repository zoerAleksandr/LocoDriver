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
}
