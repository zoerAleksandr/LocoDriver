package com.z_company.domain.track

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TrackAnalysisTest {
    private fun p(t: Long, lat: Double = 55.0, lon: Double = 37.0, v: Double? = 50.0) = TrackPoint(t * 1000, lat, lon, v, 5f)

    @Test
    fun nearestPointWithinTolerance() {
        val pts = listOf(p(0, 55.0, 37.0), p(5, 55.001, 37.0), p(10, 55.002, 37.0))
        assertEquals(5_000L, TrackAnalysis.nearestPoint(pts, 55.0011, 37.0, 30.0)?.timeMs)
        assertNull(TrackAnalysis.nearestPoint(pts, 55.01, 37.0, 30.0))
        assertNull(TrackAnalysis.nearestPoint(emptyList(), 55.0, 37.0, 30.0))
    }

    @Test
    fun dwellNeedsSixtySecondsBelowTwoKmh() {
        val pts = listOf(p(0, v = 40.0), p(5, v = 1.0), p(65, v = 0.5), p(125, v = 0.0), p(130, v = 30.0))
        val d = TrackAnalysis.detectDwells(pts)
        assertEquals(1, d.size)
        assertEquals(5_000L, d[0].arrivalMs)
        assertEquals(125_000L, d[0].departureMs)
    }

    @Test
    fun shortSlowdownIsNotDwell() {
        val pts = listOf(p(0, v = 40.0), p(5, v = 1.0), p(30, v = 1.0), p(35, v = 30.0))
        assertTrue(TrackAnalysis.detectDwells(pts).isEmpty())
    }

    @Test
    fun unknownSpeedBreaksDwell() {
        val pts = listOf(p(0, v = 0.0), p(30, v = null), p(70, v = 0.0), p(1000, v = 0.0))
        assertTrue(TrackAnalysis.detectDwells(pts).isEmpty())
    }

    @Test
    fun decimateKeepsEndsAndDropsCollinear() {
        val pts = (0..100).map { p(it.toLong() * 5, 55.0 + it * 0.0001, 37.0, 50.0) }
        val out = TrackAnalysis.decimate(pts)
        assertEquals(pts.first(), out.first())
        assertEquals(pts.last(), out.last())
        assertEquals(2, out.size)
    }

    @Test
    fun decimateKeepsCornerAndSpeedChange() {
        val pts = listOf(p(0, 55.0, 37.0), p(5, 55.001, 37.0), p(10, 55.001, 37.001), p(15, 55.001, 37.002, v = 100.0))
        val out = TrackAnalysis.decimate(pts)
        assertEquals(4, out.size)
    }

    @Test
    fun decimateRespectsMaxPoints() {
        val pts = (0..3000).map { p(it.toLong() * 5, 55.0 + (it % 2) * 0.0003, 37.0 + it * 0.0001) }
        assertTrue(TrackAnalysis.decimate(pts, maxPoints = 500).size <= 502)
    }

    @Test
    fun stopMarksMergeStationAndDwell() {
        val pts = listOf(p(0, v = 40.0), p(10, 55.5, 37.5, 0.0), p(100, 55.5, 37.5, 0.0), p(110, 56.0, 38.0, 40.0))
        val dw = TrackAnalysis.detectDwells(pts)
        val marks = TrackAnalysis.stopMarks(pts, dw, listOf(StationTimes("Тверь", 12_000, 98_000)))
        assertEquals(1, marks.size)
        assertEquals("Тверь", marks[0].name)
        assertEquals(55.5, marks[0].latitude)
        val anon = TrackAnalysis.stopMarks(pts, dw, emptyList())
        assertEquals(1, anon.size)
        assertNull(anon[0].name)
    }

    @Test
    fun stationWithoutDwellLocatedByTime() {
        val pts = listOf(p(0, 55.0, 37.0, 40.0), p(60, 55.1, 37.1, 40.0))
        val marks = TrackAnalysis.stopMarks(
            pts, emptyList(),
            listOf(StationTimes("Клин", 58_000, 70_000), StationTimes("Проход", 5_000, null, true)),
        )
        assertEquals(1, marks.size)
        assertEquals(55.1, marks[0].latitude)
    }

    @Test
    fun clockFormatsInZone() {
        assertEquals("03:00:05", TrackAnalysis.clock(5_000, "GMT+3", true))
        assertEquals("03:00", TrackAnalysis.clock(5_000, "GMT+3"))
        assertEquals(3, TrackAnalysis.dwellMinutes(0, 170_000))
    }

    @Test
    fun sparseThreePointStopIsDetected() {
        val pts = listOf(p(-10, v = 40.0), p(-5, v = 20.0), p(0, v = 0.0), p(60, v = 0.0), p(110, v = 0.0), p(115, v = 12.0), p(120, v = 30.0))
        val d = TrackAnalysis.detectDwells(pts)
        assertEquals(1, d.size)
        assertEquals(0L, d[0].arrivalMs)
        assertEquals(110_000L, d[0].departureMs)
    }

    @Test
    fun sparseSinglePointStopBetweenMovingPointsIsDetected() {
        // Запись прореживает стоянку до точки раз в 60 с: между движущимися точками осталась одна с нулём.
        val pts = listOf(p(0, v = 30.0), p(5, v = 8.0), p(65, v = 0.0), p(115, v = 6.0), p(120, v = 25.0))
        val d = TrackAnalysis.detectDwells(pts)
        assertEquals(1, d.size)
        assertEquals(5_000L, d[0].arrivalMs)
        assertEquals(115_000L, d[0].departureMs)
        assertEquals(2, TrackAnalysis.dwellMinutes(d[0].arrivalMs, d[0].departureMs))
    }

    @Test
    fun sparseTwoPointStopUnder60sSpanIsDetected() {
        val pts = listOf(p(0, v = 30.0), p(5, v = 9.0), p(60, v = 0.0), p(112, v = 0.0), p(117, v = 10.0))
        assertEquals(1, TrackAnalysis.detectDwells(pts).size)
    }

    @Test
    fun denseSlowdownNearSparsePointIsNotDwell() {
        val pts = listOf(p(0, v = 30.0), p(5, v = 1.0), p(10, v = 30.0), p(70, v = 30.0))
        assertTrue(TrackAnalysis.detectDwells(pts).isEmpty())
    }

    @Test
    fun recordingGapIsNotDwellExtension() {
        val pts = listOf(p(0, v = 30.0), p(5, v = 1.0), p(600, v = 30.0))
        assertTrue(TrackAnalysis.detectDwells(pts).isEmpty())
    }

    @Test
    fun signalLossBetweenMovingPointsIsNotDwell() {
        // Ехал, тоннель 3 минуты, первая точка после выхода с малой скоростью: раньше (разрыв ≤ 5 мин) считалось стоянкой.
        val pts = listOf(p(0, v = 60.0), p(5, v = 60.0), p(185, v = 1.0), p(190, v = 40.0))
        assertTrue(TrackAnalysis.detectDwells(pts).isEmpty())
    }

    @Test
    fun gapUpTo90sStillExtendsDwell() {
        val pts = listOf(p(0, v = 30.0), p(5, v = 8.0), p(65, v = 0.0), p(115, v = 6.0), p(120, v = 25.0))
        assertEquals(1, TrackAnalysis.detectDwells(pts).size)
    }

    @Test
    fun gapOver90sBetweenSlowPointsSplitsDwell() {
        // Две «медленные» точки через 3 минуты - это потеря записи, а не стоянка на 3 минуты.
        val pts = listOf(p(0, v = 0.5), p(180, v = 0.5), p(185, v = 40.0))
        assertTrue(TrackAnalysis.detectDwells(pts).isEmpty())
    }

    @Test
    fun denseStopSurvivesOneMinuteSpacing() {
        val pts = listOf(p(0, v = 30.0), p(5, v = 0.5), p(65, v = 0.5), p(125, v = 0.5), p(185, v = 0.5), p(190, v = 30.0))
        val d = TrackAnalysis.detectDwells(pts)
        assertEquals(1, d.size)
        assertEquals(5_000L, d[0].arrivalMs)
    }

    @Test
    fun dataGapsAreReported() {
        val pts = listOf(p(0, v = 60.0), p(5, v = 60.0), p(185, v = 40.0), p(190, v = 40.0), p(280, v = 40.0))
        val g = TrackAnalysis.dataGaps(pts)
        assertEquals(1, g.size)
        assertEquals(5_000L, g[0].fromMs)
        assertEquals(185_000L, g[0].toMs)
    }
}
