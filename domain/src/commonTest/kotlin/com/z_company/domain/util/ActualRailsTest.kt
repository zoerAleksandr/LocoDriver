package com.z_company.domain.util

import com.z_company.domain.track.TrackPoint
import kotlin.math.cos
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ActualRailsTest {
    private val lat0 = 55.0
    private val mLat = 1.0 / 111_195.0
    private val mLon = 1.0 / (111_195.0 * cos(lat0 * PI / 180.0))

    /** Прямой путь на восток длиной [lengthM] со смещением [northM] к северу от базового. */
    private fun track(northM: Double, lengthM: Double = 3000.0, stepM: Double = 100.0): List<Pair<Double, Double>> =
        (0..(lengthM / stepM).toInt()).map { (lat0 + northM * mLat) to (30.0 + it * stepM * mLon) }

    private fun drive(northM: Double, fromM: Double = 500.0, toM: Double = 2500.0, speed: Double = 60.0): List<TrackPoint> {
        val out = ArrayList<TrackPoint>()
        var x = fromM
        var t = 0L
        while (x <= toM) {
            out += TrackPoint(t, lat0 + northM * mLat, 30.0 + x * mLon, speed, 5f)
            x += 50.0
            t += 3_000L
        }
        return out
    }

    private val route = track(0.0)

    @Test fun parallelTrackWithin15mIsOnRailAndNoDeviation() {
        for (d in listOf(4.0, 10.0, 15.0)) {
            val pts = drive(d - 0.1)
            assertTrue(ActualRails.snapMarked(pts, listOf(route)).all { it.onRail }, "d=$d")
            assertNull(ActualRails.deviationRun(pts, listOf(route)), "d=$d")
        }
    }

    @Test fun multiTrackSectionPicksNearestTrackAndSnapsToIt() {
        val b = track(35.0) // соседний путь через 35 м (стрелочная горловина)
        val pts = drive(35.0)
        // Только линия плеча: 35 м - ещё «на путях», но привязка тянет на ЧУЖУЮ линию.
        val oldSnap = ActualRails.snapMarked(pts, listOf(route))
        assertTrue(oldSnap.all { it.onRail })
        assertTrue(oldSnap.all { kotlin.math.abs((it.point.latitude - lat0) / mLat) < 1.0 })
        // С линией по факту привязка к ближайшему реальному пути.
        val snap = ActualRails.snapMarked(pts, listOf(route, b))
        assertTrue(snap.all { it.onRail })
        assertTrue(snap.all { kotlin.math.abs((it.point.latitude - lat0) / mLat - 35.0) < 1.0 })
        assertEquals(1, ActualRails.project(listOf(route, b), lat0 + 33.0 * mLat, 30.01)!!.lineIndex)
        assertEquals(0, ActualRails.project(listOf(route, b), lat0 + 3.0 * mLat, 30.01)!!.lineIndex)
    }

    @Test fun deviationRunTriggersOn30mParallelAndStopsAfterFactLineAdded() {
        val pts = drive(30.0)
        val run = ActualRails.deviationRun(pts, listOf(route))
        assertNotNull(run)
        assertTrue(run.size >= ActualRails.MIN_RUN_POINTS)
        val fact = track(30.0)
        assertTrue(ActualRails.accept(run, fact))
        assertNull(ActualRails.deviationRun(pts, listOf(route, fact)))
    }

    @Test fun realFieldExitIsOffRailAndFactLineIsRejected() {
        val pts = drive(400.0) // ушёл в поле на 400 м
        val snap = ActualRails.snapMarked(pts, listOf(route))
        assertTrue(snap.none { it.onRail })
        assertTrue((ActualRails.offsetFromAny(listOf(route), pts[0].latitude, pts[0].longitude) ?: 0.0) > 50.0)
        val run = ActualRails.deviationRun(pts, listOf(route))!!
        // Сервер вернул линию плеча (не проходит по точкам) - не принимаем, «вне путей» остаётся.
        assertFalse(ActualRails.accept(run, route))
        // Объезд втрое длиннее участка - не принимаем.
        val detour = listOf(run.first().latitude to run.first().longitude, lat0 + 5000 * mLat to 30.02, run.last().latitude to run.last().longitude)
        assertFalse(ActualRails.accept(run, detour))
    }

    @Test fun shortOrSlowDeviationsAreIgnored() {
        assertNull(ActualRails.deviationRun(drive(30.0, 500.0, 600.0), listOf(route))) // выброс GPS: коротко
        assertNull(ActualRails.deviationRun(drive(30.0, speed = 1.0), listOf(route))) // стоянка рядом
        // Разрыв по времени делит участок: каждая половина короче порога.
        val a = drive(30.0, 500.0, 650.0)
        val b = drive(30.0, 700.0, 850.0).map { it.copy(timeMs = it.timeMs + 3_600_000L) }
        assertNull(ActualRails.deviationRun(a + b, listOf(route)))
    }

    @Test fun deviationRunKeepsLatestRunAndViaPointsAreBounded() {
        val onRoute = drive(0.0, 0.0, 400.0)
        val off = drive(30.0, 450.0, 2450.0).map { it.copy(timeMs = it.timeMs + 100_000L) }
        val run = ActualRails.deviationRun(onRoute + off, listOf(route))!!
        assertEquals(off.size, run.size)
        val via = ActualRails.viaPoints(run)
        assertTrue(via.size in 2..ActualRails.MAX_VIA)
        assertEquals(run.first().latitude to run.first().longitude, via.first())
        assertEquals(run.last().latitude to run.last().longitude, via.last())
        assertEquals(2, ActualRails.viaPoints(run.take(5)).size.coerceAtMost(2))
    }

    @Test fun alongRouteFixProjectsOntoRouteWhenOnFactLine() {
        val fact = track(80.0)
        val fix = GeoFix(lat0 + 80.0 * mLat, 30.02)
        val projected = ActualRails.alongRouteFix(route, listOf(fact), fix)!!
        assertTrue(kotlin.math.abs((projected.latitude - lat0) / mLat) < 1.0)
        assertEquals(fix.longitude, projected.longitude, 1e-6)
        // Без линии по факту (или в поле) положение не меняется - остаётся «вне путей».
        assertEquals(fix, ActualRails.alongRouteFix(route, emptyList(), fix))
        val field = GeoFix(lat0 + 400.0 * mLat, 30.02)
        assertEquals(field, ActualRails.alongRouteFix(route, listOf(fact), field))
        // Рядом с линией плеча - без изменений.
        val near = GeoFix(lat0 + 10.0 * mLat, 30.02)
        assertEquals(near, ActualRails.alongRouteFix(route, listOf(fact), near))
    }

    @Test fun factLinesAreBounded() {
        var lines = emptyList<List<Pair<Double, Double>>>()
        repeat(10) { lines = ActualRails.withFactLine(lines, track(it.toDouble())) }
        assertEquals(ActualRails.MAX_FACT_LINES, lines.size)
        assertEquals(track(9.0), lines.last())
    }
}
