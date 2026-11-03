package com.z_company.domain.track

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
    fun nearestOnTrackPicksNearerEndpointOfSegmentUnderTap() {
        // Точки через ~222 м (0.002°): тап на линии в 80 м от первой и 142 м от второй - далеко от обеих точек.
        val pts = listOf(p(0, 55.0, 37.0, 10.0), p(5, 55.002, 37.0, 70.0))
        val lat = 55.0 + 80.0 / 111_195.0
        assertNull(TrackAnalysis.nearestPoint(pts, lat, 37.0, 30.0))
        assertEquals(0L, TrackAnalysis.nearestOnTrack(pts, lat, 37.0, 30.0, 48.0 + 40.0)?.timeMs)
        val lat2 = 55.0 + 150.0 / 111_195.0
        assertEquals(5_000L, TrackAnalysis.nearestOnTrack(pts, lat2, 37.0, 30.0, 88.0)?.timeMs)
    }

    @Test
    fun nearestOnTrackUsesLineToleranceSideways() {
        val pts = listOf(p(0, 55.0, 37.0), p(5, 55.002, 37.0))
        // 40 м в сторону от середины линии: в допуске линии 48 м - попадание, в допуске 30 м - нет.
        val lon = 37.0 + 40.0 / (111_195.0 * kotlin.math.cos(55.001 * kotlin.math.PI / 180))
        assertNotNull(TrackAnalysis.nearestOnTrack(pts, 55.001, lon, 30.0, 48.0))
        assertNull(TrackAnalysis.nearestOnTrack(pts, 55.001, lon, 30.0, 30.0))
        assertNull(TrackAnalysis.nearestOnTrack(emptyList(), 55.0, 37.0, 30.0, 48.0))
    }

    @Test
    fun nearestOnTrackIgnoresSegmentAcrossGap() {
        // Разрыв записи 10 мин: между точками линии нет, тап посередине не попадает.
        val pts = listOf(p(0, 55.0, 37.0), p(600, 55.002, 37.0))
        assertNull(TrackAnalysis.nearestOnTrack(pts, 55.001, 37.0, 30.0, 48.0))
        // но у самой точки - попадание по допуску точки
        assertEquals(0L, TrackAnalysis.nearestOnTrack(pts, 55.0001, 37.0, 30.0, 48.0)?.timeMs)
    }

    @Test
    fun shortSlowdownIsNotDwell() {
        val pts = listOf(p(0, v = 40.0), p(5, v = 1.0), p(30, v = 1.0), p(35, v = 30.0))
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
    fun gapOver90sBetweenSlowPointsSplitsDwell() {
        // Две «медленные» точки через 3 минуты - это потеря записи, а не стоянка на 3 минуты.
        val pts = listOf(p(0, v = 0.5), p(180, v = 0.5), p(185, v = 40.0))
        assertTrue(TrackAnalysis.detectDwells(pts).isEmpty())
    }

    @Test
    fun dataGapsAreReported() {
        val pts = listOf(p(0, v = 60.0), p(5, v = 60.0), p(185, v = 40.0), p(190, v = 40.0), p(280, v = 40.0))
        val g = TrackAnalysis.dataGaps(pts)
        assertEquals(1, g.size)
        assertEquals(5_000L, g[0].fromMs)
        assertEquals(185_000L, g[0].toMs)
    }

    // ---- Единый алгоритм: оффлайн = тот же StopDetector, времена кратны минуте ----

    @Test
    fun dwellNeedsSixtySecondsBelowTwoKmh() {
        val pts = listOf(p(0, v = 40.0), p(5, v = 1.0), p(65, v = 0.5), p(125, v = 0.0), p(130, v = 30.0), p(135, v = 30.0), p(165, v = 30.0))
        val d = TrackAnalysis.detectDwells(pts)
        assertEquals(1, d.size)
        assertEquals(0L, d[0].arrivalMs) // окно с 5 с, округление вниз до минуты
        assertEquals(120_000L, d[0].departureMs) // откат к последней точке ≈0 (125 с)
    }

    @Test
    fun sparseStopWithTransitionPointsMatchesOnlineDetector() {
        // Запись по правилам TrackThinner: первая «медленная» точка и последняя перед трогом пишутся всегда.
        val pts = listOf(
            p(300, v = 40.0), p(310, v = 20.0), p(335, v = 1.0), p(395, v = 0.0), p(455, v = 0.0), p(500, v = 0.0),
            p(505, v = 8.0), p(510, v = 12.0), p(515, v = 20.0), p(535, v = 30.0),
        )
        val d = TrackAnalysis.detectDwells(pts)
        assertEquals(1, d.size)
        assertEquals(300_000L, d[0].arrivalMs) // 335 с -> минута 300
        assertEquals(480_000L, d[0].departureMs) // последняя точка покоя 500 с -> 480
        assertEquals(3, TrackAnalysis.dwellMinutes(d[0].arrivalMs, d[0].departureMs))
    }

    @Test
    fun slowGapUpTo90sIsAllowedButLongerIsNoData() {
        val ok = listOf(p(0, v = 30.0), p(5, v = 0.5), p(65, v = 0.5), p(125, v = 0.5), p(130, v = 30.0), p(135, v = 30.0), p(165, v = 30.0))
        assertEquals(1, TrackAnalysis.detectDwells(ok).size)
        // Между «медленными» точками 100 с: окно сбрасывается, стоянки нет.
        val gap = listOf(p(0, v = 30.0), p(5, v = 0.5), p(105, v = 0.5), p(160, v = 0.5), p(170, v = 30.0), p(175, v = 30.0), p(205, v = 30.0))
        assertTrue(TrackAnalysis.detectDwells(gap).isEmpty())
    }

    @Test
    fun unknownSpeedPointsAreIgnoredLikeOnline() {
        val pts = listOf(p(0, v = 0.0), p(30, v = null), p(70, v = 0.0), p(75, v = 40.0), p(80, v = 40.0), p(110, v = 40.0))
        val d = TrackAnalysis.detectDwells(pts)
        assertEquals(1, d.size)
    }

    @Test
    fun onlineAndOfflineGiveSameStopsIncludingSignalLoss() {
        // Один и тот же «истинный» поток 1 Гц; онлайн - сырой, оффлайн - прореженный TrackThinner и записанный TrackWriter.
        val raw = ArrayList<TrackPoint>()
        for (t in 0..59) raw += p(t.toLong(), v = 60.0)            // едем
        for (t in 60..80) raw += p(t.toLong(), v = (60 - (t - 60) * 3).coerceAtLeast(0).toDouble()) // тормозим до 0
        for (t in 81..260) raw += p(t.toLong(), v = 0.0)           // стоим ~3 мин
        for (t in 261..275) raw += p(t.toLong(), v = ((t - 260) * 4.0)) // разгон
        for (t in 276..340) raw += p(t.toLong(), v = 60.0)         // едем
        // тоннель: нет данных 341..520, потом снова едем
        for (t in 521..560) raw += p(t.toLong(), v = 60.0)

        val online = ArrayList<com.z_company.domain.autorecord.StopEvent>()
        val det = com.z_company.domain.autorecord.StopDetector()
        for (q in raw) det.onSample(com.z_company.domain.autorecord.MotionSample(q.timeMs, q.latitude, q.longitude, q.speedKmh, q.accuracyM))?.let { online += it }

        val th = TrackThinner()
        val recorded = ArrayList<TrackPoint>()
        for (q in raw) if (th.accept(q)) { th.takePending()?.let { recorded += it }; recorded += q }
        val dwells = TrackAnalysis.detectDwells(recorded)

        assertEquals(2, online.size) // прибытие + отправление, тоннель ничего не добавил
        assertEquals(1, dwells.size)
        assertEquals(online[0].minuteTimeMs, dwells[0].arrivalMs)
        assertEquals(online[1].minuteTimeMs, dwells[0].departureMs)
    }
}
