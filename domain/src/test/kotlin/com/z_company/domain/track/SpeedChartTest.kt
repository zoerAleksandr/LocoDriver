package com.z_company.domain.track

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpeedChartTest {
    // 0.001° широты ≈ 111 м; точка раз в 10 с.
    private fun pts(n: Int, v: (Int) -> Double? = { 60.0 }) =
        (0 until n).map { TrackPoint(it * 10_000L, 55.0 + it * 0.001, 37.0, v(it), 5f) }

    @Test
    fun seriesAccumulatesDistance() {
        val s = SpeedChart.buildSeries(pts(11))
        assertEquals(0.0, s.first().distM)
        assertEquals(1112.0, s.last().distM, 15.0)
        assertTrue(s.zipWithNext().all { (a, b) -> b.distM >= a.distM })
        assertEquals(100_000L, SpeedChart.total(s, ChartAxis.TIME).toLong())
        assertEquals(10, s.last().index)
    }

    @Test
    fun gapDoesNotAddDistance() {
        val a = pts(3)
        val b = pts(3).map { it.copy(timeMs = it.timeMs + 3_600_000L, latitude = it.latitude + 1.0) }
        val s = SpeedChart.buildSeries(a + b)
        assertEquals(s[2].distM, s[3].distM, 1e-9)
        assertTrue(s[5].distM > s[3].distM)
    }

    @Test
    fun zoomHalvesSpanAroundAnchor() {
        val total = 10_000.0
        val w = SpeedChart.zoom(SpeedChart.fullWindow(total), total, 2.0, 2_500.0, 300.0)
        assertEquals(5_000.0, w.span, 1e-6)
        assertEquals(1_250.0, w.start, 1e-6) // якорь на 25% окна остаётся на 25%
    }

    @Test
    fun zoomAnchorStaysUnderFinger() {
        val total = 10_000.0
        val w1 = SpeedChart.zoom(ChartWindow(2_000.0, 8_000.0), total, 2.0, 5_000.0, 300.0)
        assertEquals(3_000.0, w1.span, 1e-6)
        assertEquals(3_500.0, w1.start, 1e-6)
    }

    @Test
    fun zoomLimits() {
        val total = 10_000.0
        val tiny = SpeedChart.zoom(SpeedChart.fullWindow(total), total, 1e9, 5_000.0, 300.0)
        assertEquals(300.0, tiny.span, 1e-6)
        val out = SpeedChart.zoom(ChartWindow(1_000.0, 2_000.0), total, 1e-9, 1_500.0, 300.0)
        assertEquals(ChartWindow(0.0, total), out)
    }

    @Test
    fun panClampsToSeries() {
        val total = 10_000.0
        val w = ChartWindow(2_000.0, 4_000.0)
        assertEquals(ChartWindow(3_000.0, 5_000.0), SpeedChart.pan(w, total, 1_000.0))
        assertEquals(ChartWindow(0.0, 2_000.0), SpeedChart.pan(w, total, -9_000.0))
        assertEquals(ChartWindow(8_000.0, 10_000.0), SpeedChart.pan(w, total, 99_000.0))
    }

    @Test
    fun visibleDecimatesButKeepsExtremes() {
        val s = SpeedChart.buildSeries(pts(2000) { if (it == 1000) 120.0 else if (it == 1500) 0.0 else 60.0 })
        val total = SpeedChart.total(s, ChartAxis.PATH)
        val out = SpeedChart.visible(s, ChartAxis.PATH, SpeedChart.fullWindow(total), 100)
        assertTrue(out.size <= 100 * 4 + 4)
        assertTrue(out.any { it.speedKmh == 120.0 })
        assertTrue(out.any { it.speedKmh == 0.0 })
        assertEquals(0, out.first().index)
        assertEquals(1999, out.last().index)
    }

    @Test
    fun visibleWindowIncludesNeighbours() {
        val s = SpeedChart.buildSeries(pts(100))
        val out = SpeedChart.visible(s, ChartAxis.PATH, ChartWindow(s[50].distM, s[60].distM), 1000)
        assertEquals(49, out.first().index)
        assertEquals(60, out.last().index)
    }

    @Test
    fun nearestAndIndexAtTime() {
        val s = SpeedChart.buildSeries(pts(11))
        assertEquals(3, SpeedChart.nearest(s, ChartAxis.TIME, 31_000.0))
        assertEquals(4, SpeedChart.nearest(s, ChartAxis.TIME, 36_000.0))
        assertEquals(0, SpeedChart.nearest(s, ChartAxis.PATH, -50.0))
        assertEquals(10, SpeedChart.nearest(s, ChartAxis.PATH, 1e9))
        assertEquals(2, SpeedChart.indexAtTime(s, 24_000L))
        assertEquals(-1, SpeedChart.nearest(emptyList(), ChartAxis.PATH, 0.0))
    }

    @Test
    fun stopsMapToAxes() {
        val s = SpeedChart.buildSeries(pts(11))
        val stops = listOf(
            TrainStopMark(55.0, 37.0, "Депо", 30_000L, 60_000L),
            TrainStopMark(55.0, 37.0, null, 9_999_999L, null), // вне записи
        )
        val onTime = SpeedChart.stopsOnAxis(s, stops, ChartAxis.TIME)
        assertEquals(1, onTime.size)
        assertEquals(30_000.0, onTime[0].x)
        assertEquals(60_000.0, onTime[0].xEnd)
        val onPath = SpeedChart.stopsOnAxis(s, stops, ChartAxis.PATH)
        assertEquals(s[3].distM, onPath[0].x)
        assertEquals(onPath[0].x, onPath[0].xEnd)
    }

    @Test
    fun ceilingAndTicks() {
        assertEquals(40.0, SpeedChart.speedCeiling(emptyList()))
        val s = SpeedChart.buildSeries(pts(5) { 83.0 })
        assertEquals(100.0, SpeedChart.speedCeiling(s))
        assertEquals(listOf(0.0, 20.0, 40.0, 60.0, 80.0, 100.0), SpeedChart.niceTicks(0.0, 100.0, 6))
        assertTrue(SpeedChart.niceTicks(0.0, 0.0, 5).isEmpty())
    }

    @Test
    fun timeTicksAreRoundWallClock() {
        val h = 3_600_000L
        val t0 = 10 * h + 27 * 60_000L // 10:27 UTC, пояс +3 → 13:27
        val ticks = SpeedChart.timeTicks(t0, 0.0, 2.0 * h, 6, 3 * h)
        assertTrue(ticks.isNotEmpty())
        // каждая метка в местном времени кратна шагу (30 мин)
        ticks.forEach { assertEquals(0L, (t0 + it.toLong() + 3 * h) % (30 * 60_000L)) }
        assertTrue(ticks.all { it >= 0.0 && it <= 2.0 * h })
        assertEquals(30 * 60_000.0, ticks[1] - ticks[0])
        assertTrue(SpeedChart.timeTicks(t0, 5.0, 5.0, 6, 0).isEmpty())
    }

    @Test
    fun speedScaleEndpointsAndUnknown() {
        assertEquals(0xD32F2F, SpeedScale.rgb(0.0))
        assertEquals(0x1E88E5, SpeedScale.rgb(200.0))
        assertEquals(SpeedScale.UNKNOWN_RGB, SpeedScale.rgb(null))
        assertEquals(0xF57C00, SpeedScale.rgb(30.0))
        val mid = SpeedScale.rgb(15.0)
        assertTrue(mid != 0xD32F2F && mid != 0xF57C00)
    }
}
