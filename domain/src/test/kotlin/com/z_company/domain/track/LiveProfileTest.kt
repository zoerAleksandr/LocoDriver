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

    @Test
    fun calibration_shifts_position_and_km() {
        val lp = LiveProfile(section(), reversed = false)
        lp.onFix(1_000, lat0, lon(2.0), 60.0, 5f)
        // GPS показывает 102 км; пользователь на самом деле у 102 км пк 6 (+0,5 км)
        val c = lp.calibrationFor(102.5) as Calibration.Ok
        assertEquals(0.5, c.shiftKm, 0.02)
        assertEquals(0.5, c.diffKm, 0.02)
        lp.applyCalibration(c.shiftKm)
        assertEquals(2.5, lp.position!!.kmAxis, 0.02)
        assertEquals("102 км пк 6", lp.position!!.kmText?.substringBefore(" +"))
        // следующие фиксы тоже со сдвигом
        val p = lp.onFix(61_000, lat0, lon(3.0), 60.0, 5f)
        assertEquals(3.5, p!!.kmAxis, 0.02)
        lp.applyCalibration(0.0)
        assertEquals(3.0, lp.position!!.kmAxis, 0.02)
    }

    @Test
    fun calibration_edge_cases() {
        val lp = LiveProfile(section(), reversed = false)
        assertEquals(Calibration.NoPosition, lp.calibrationFor(102.0))
        lp.onFix(1_000, lat0, lon(2.0), 60.0, 5f)
        assertEquals(Calibration.NotOnSection, lp.calibrationFor(500.0))
        // за пределами участка по нумерации (до 100 км и после 110 км) тоже нет
        assertEquals(Calibration.NotOnSection, lp.calibrationFor(99.0))
    }

    @Test
    fun repeated_numbering_picks_nearest_to_current() {
        val s = section().copy(kmMarks = listOf(KmMark(0.0, 100.0, 1), KmMark(5.0, 100.0, 1)))
        val lp = LiveProfile(s, reversed = false)
        lp.onFix(1_000, lat0, lon(6.0), 60.0, 5f)
        val c = lp.calibrationFor(100.5) as Calibration.Ok
        assertEquals(5.5, 5.5 + 0.0 * c.shiftKm, 1e-9) // кандидаты: 0.5 и 5.5 - ближайший к 6.0 это 5.5
        assertEquals(0.5, c.diffKm, 0.02)
    }

    @Test
    fun train_path_runs_back_from_head_and_clamps_at_start() {
        val lp = LiveProfile(section(), reversed = false)
        lp.onFix(1_000, lat0, lon(5.0), 60.0, 5f)
        val path = lp.trainPath(0.8)
        assertTrue(path.size >= 2)
        // хвост на 0,8 км западнее головы, всё на одной широте
        assertEquals(lon(4.2), path.first().second, 1e-5)
        assertEquals(lon(5.0), path.last().second, 1e-5)
        assertTrue(path.all { kotlin.math.abs(it.first - lat0) < 1e-9 })
        // у начала участка обрезается по участку
        val near = LiveProfile(section(), reversed = false)
        near.onFix(1_000, lat0, lon(0.3), 60.0, 5f)
        assertEquals(lon(0.0), near.trainPath(0.8).first().second, 1e-5)
    }

    @Test
    fun train_path_reversed_tail_is_at_larger_km() {
        val lp = LiveProfile(section(), reversed = true)
        lp.onFix(1_000, lat0, lon(5.0), 60.0, 5f)
        val path = lp.trainPath(0.8)
        // едем к началу оси: хвост восточнее (больший км), порядок хвост -> голова
        assertEquals(lon(5.8), path.first().second, 1e-5)
        assertEquals(lon(5.0), path.last().second, 1e-5)
    }

    @Test
    fun train_path_empty_without_position_or_off_route_or_zero_length() {
        val lp = LiveProfile(section(), reversed = false)
        assertTrue(lp.trainPath(0.5).isEmpty())
        lp.onFix(1_000, lat0, lon(5.0), 60.0, 5f)
        assertTrue(lp.trainPath(0.0).isEmpty())
        assertTrue(AxisSlice.between(section().axis.points, 3.0, 3.0).isEmpty())
    }

    @Test
    fun train_path_ignores_manual_calibration() {
        val lp = LiveProfile(section(), reversed = false)
        lp.onFix(1_000, lat0, lon(5.0), 60.0, 5f)
        lp.applyCalibration(0.4)
        // на профиле положение сдвинуто, а линия на карте по-прежнему заканчивается у GPS
        assertEquals(5.4, lp.position!!.kmAxis, 0.02)
        assertEquals(lon(5.0), lp.trainPath(0.8).last().second, 1e-5)
    }

    @Test
    fun smooth_slice_follows_curve_and_keeps_ends() {
        // ось с поворотом на 90 градусов в точке km=1; хорда срезала бы угол, сплайн идёт плавно, но через точки оси
        val pts = listOf(
            AxisPoint(0.0, 55.0, 37.0), AxisPoint(0.5, 55.0, 37.007), AxisPoint(1.0, 55.0, 37.014),
            AxisPoint(1.5, 55.004, 37.014), AxisPoint(2.0, 55.009, 37.014),
        )
        val smooth = AxisSlice.smooth(pts, 0.2, 1.8)
        val straight = AxisSlice.between(pts, 0.2, 1.8)
        assertTrue(smooth.size > straight.size)
        // концы - ровно на заданных км
        assertEquals(AxisSlice.between(pts, 0.2, 0.2001).first().first, smooth.first().first, 1e-6)
        // сплайн проходит через вершину оси (km=1.0)
        val atCorner = smooth.minByOrNull { (it.first - 55.0) * (it.first - 55.0) + (it.second - 37.014) * (it.second - 37.014) }!!
        assertEquals(55.0, atCorner.first, 3e-4)
        assertEquals(37.014, atCorner.second, 3e-4)
        // шаг мелкий: соседние точки не дальше ~25 м
        smooth.zipWithNext().forEach { (a, b) ->
            val dy = (b.first - a.first) * FlatMetric.M_PER_DEG_LAT
            val dx = (b.second - a.second) * FlatMetric.kx(55.0)
            assertTrue(kotlin.math.sqrt(dx * dx + dy * dy) < 30.0)
        }
        assertTrue(AxisSlice.smooth(pts, 1.0, 1.0).isEmpty())
    }

    @Test
    fun abs_km_at_point_uses_marks_and_calibration() {
        val lp = LiveProfile(section(), reversed = false)
        lp.onFix(1_000, lat0, lon(2.0), 60.0, 5f)
        assertEquals(102.0, lp.absKmAt(lat0, lon(2.0))!!, 0.02)
        lp.applyCalibration(0.5)
        assertEquals(102.5, lp.absKmAt(lat0, lon(2.0))!!, 0.02)
        assertNull(lp.absKmAt(lat0 + 0.01, lon(2.0)))              // дальше 300 м от оси
        assertNull(LiveProfile(section(withMarks = false), false).absKmAt(lat0, lon(2.0)))
    }
}
