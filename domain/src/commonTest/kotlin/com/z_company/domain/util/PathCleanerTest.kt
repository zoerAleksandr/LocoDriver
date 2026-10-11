package com.z_company.domain.util

import com.z_company.domain.track.TrackAnalysis
import com.z_company.domain.track.TrackPoint
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Пройденный путь без зигзагов: стоянка в парке, переход на соседний путь, прямой участок (SCREEN_SPECS §6.9). */
class PathCleanerTest {
    private val lat0 = 59.0
    private val lon0 = 32.0
    private val mLat = 1.0 / 111_195.0
    private val mLon = 1.0 / (111_195.0 * cos(lat0 * PI / 180.0))

    private fun pt(t: Long, xM: Double, yM: Double, speed: Double?, acc: Float = 2f) =
        TrackPoint(t, lat0 + yM * mLat, lon0 + xM * mLon, speed, acc)

    /** Путь парка №[n] (0..7): прямая на восток, пути через 5 м к северу. */
    private fun yardLine(n: Int, fromM: Double = -200.0, toM: Double = 2000.0): List<Pair<Double, Double>> =
        listOf((lat0 + n * 5.0 * mLat) to (lon0 + fromM * mLon), (lat0 + n * 5.0 * mLat) to (lon0 + toM * mLon))

    private fun north(p: TrackPoint) = (p.latitude - lat0) / mLat
    private fun east(p: TrackPoint) = (p.longitude - lon0) / mLon

    /** Подъезд 20 км/ч, стоянка 3 минуты с разбросом 15 м (скорость 0), отправление. */
    private fun standAtYard(): List<TrackPoint> {
        val rnd = Random(7)
        val out = ArrayList<TrackPoint>()
        var t = 0L
        var x = 0.0
        while (x < 300.0) { out += pt(t, x, 10.0, 20.0); x += 28.0; t += 5_000L }
        repeat(18) { // 3 минуты, точка раз в 10 с
            out += pt(t, 300.0 + (rnd.nextDouble() - 0.5) * 30, 10.0 + (rnd.nextDouble() - 0.5) * 30, 0.0)
            t += 10_000L
        }
        x = 300.0
        while (x < 700.0) { out += pt(t, x, 10.0, 20.0); x += 28.0; t += 5_000L }
        return out
    }

    @Test fun stoppedWithScatterDoesNotZigzag() {
        val raw = standAtYard()
        val rawLen = ActualRails.pathLengthM(raw)
        val clean = PathCleaner.clean(raw)
        val len = ActualRails.pathLengthM(clean)
        // Исходно разброс 15 м на стоянке даёт сотни метров ломаной, после очистки - почти прямая ~700 м.
        assertTrue(rawLen > 900.0, "raw=$rawLen")
        assertTrue(len < 700.0 * 1.05 + 30.0, "len=$len")
        // Центроид стоянки один (плюс копия на время отправления - в той же точке).
        val still = clean.filter { east(it) in 280.0..320.0 }
        assertTrue(still.size <= 2, "still=${still.size}")
        // Поперёк пути нет отклонений дальше разброса стоянки.
        assertTrue(clean.all { abs(north(it) - 10.0) < 16.0 })
        // Длинная стоянка не рвёт линию разрывом по времени между приездом и отправлением.
        val gaps = clean.zipWithNext().count { (a, b) -> b.timeMs - a.timeMs > TrackAnalysis.DATA_GAP_MS }
        assertTrue(gaps <= 1, "gaps=$gaps")
    }

    @Test fun stoppedYardSnapsWithoutJumpingBetweenParallelTracks() {
        val lines = (0..7).map { yardLine(it) } // 8 путей через 5 м
        val clean = PathCleaner.clean(standAtYard())
        val snapped = ActualRails.snapMarked(clean, lines)
        assertTrue(snapped.all { it.onRail })
        val norths = snapped.map { north(it.point) }
        assertTrue(norths.max() - norths.min() < 0.5, "spread=${norths.max() - norths.min()}")
    }

    @Test fun switchToNeighbourTrackOnlyAfterThreePointsAndNoZigzag() {
        val lines = listOf(yardLine(0), yardLine(1)) // пути 0 м и 5 м
        val rnd = Random(3)
        val pts = ArrayList<TrackPoint>()
        var t = 0L
        var x = 0.0
        // До 1000 м по пути 0 (шум 1 м), дальше стрелка и путь 1 (5 м к северу).
        while (x < 2000.0) {
            val y = if (x < 1000.0) 0.0 else 5.0
            pts += pt(t, x, y + (rnd.nextDouble() - 0.5) * 2, 60.0, 2f)
            x += 80.0
            t += 5_000L
        }
        val snapped = ActualRails.snapMarked(pts, lines)
        val onLine = snapped.map { if (abs(north(it.point) - 5.0) < 0.1) 1 else 0 }
        val switches = onLine.zipWithNext().count { (a, b) -> a != b }
        assertEquals(1, switches, "onLine=$onLine")
        assertEquals(0, onLine.first())
        assertEquals(1, onLine.last())
        // Реальный переход на x >= 1000 (индекс 13): смена линии не раньше чем через 3 точки.
        val firstOn1 = onLine.indexOf(1)
        assertTrue(firstOn1 >= 13 + ActualRails.SWITCH_POINTS - 1, "firstOn1=$firstOn1")
    }

    @Test fun singleOutlierOntoNeighbourTrackIsIgnored() {
        val lines = listOf(yardLine(0), yardLine(1))
        val pts = (0 until 20).map { i -> pt(i * 5_000L, i * 80.0, if (i == 10) 5.0 else 0.0, 60.0, 2f) }
        val snapped = ActualRails.snapMarked(pts, lines)
        assertTrue(snapped.all { abs(north(it.point)) < 0.1 })
    }

    @Test fun straightSectionIsUnchanged() {
        val pts = (0 until 25).map { i -> pt(i * 5_000L, i * 83.0, 0.0, 60.0) }
        val clean = PathCleaner.clean(pts)
        assertEquals(pts.first(), clean.first())
        assertEquals(pts.last(), clean.last())
        assertEquals(ActualRails.pathLengthM(pts), ActualRails.pathLengthM(clean), 0.5)
        val snapped = ActualRails.snapMarked(clean, listOf(yardLine(0)))
        assertTrue(snapped.all { it.onRail && abs(north(it.point)) < 0.1 })
    }

    @Test fun spikeOutAndBackIsRemoved() {
        val pts = listOf(
            pt(0, 0.0, 0.0, 40.0), pt(5_000, 80.0, 0.0, 40.0), pt(10_000, 100.0, 100.0, 40.0),
            pt(15_000, 120.0, 0.0, 40.0), pt(20_000, 200.0, 0.0, 40.0),
        )
        val clean = PathCleaner.clean(pts)
        assertTrue(clean.all { abs(north(it)) < 1.0 }, "clean=${clean.map { north(it) }}")
    }

    @Test fun gapSplitsSections() {
        val a = (0 until 5).map { pt(it * 5_000L, it * 80.0, 0.0, 40.0) }
        val b = (0 until 5).map { pt(2_000_000L + it * 5_000L, 5000.0 + it * 80.0, 0.0, 40.0) }
        val clean = PathCleaner.clean(a + b)
        assertEquals(4, clean.size) // по два конца у каждого участка
        assertTrue(clean.zipWithNext().any { (p, q) -> q.timeMs - p.timeMs > TrackAnalysis.GAP_MS })
    }

    // --- Линия по факту: петли и объезды отклоняются ---

    @Test fun coherentLineAcceptedLoopRejected() {
        val straight = (0..20).map { lat0 to (lon0 + it * 50.0 * mLon) }
        assertTrue(ActualRails.isCoherentLine(straight))
        // «Восьмёрка»: самопересечение.
        val fig8 = listOf(
            lat0 to lon0, (lat0 + 100 * mLat) to (lon0 + 100 * mLon), lat0 to (lon0 + 100 * mLon),
            (lat0 + 100 * mLat) to lon0,
        )
        assertTrue(ActualRails.selfIntersects(fig8))
        assertFalse(ActualRails.isCoherentLine(fig8))
        // Объезд: длина > 1.3 x хорда + 100 м.
        val detour = listOf(lat0 to lon0, (lat0 + 600 * mLat) to (lon0 + 300 * mLon), lat0 to (lon0 + 600 * mLon))
        assertFalse(ActualRails.isCoherentLine(detour))
    }

    @Test fun runScatteredAcrossParallelTracksIsNotRequested() {
        // Участок «ушёл» от линии, но via-точки мечутся между путями: линию по факту не строим.
        val route = listOf(lat0 to lon0, lat0 to (lon0 + 3000 * mLon))
        val pts = (0 until 12).map { i -> pt(i * 5_000L, 300.0 + (i % 3) * 40.0, if (i % 2 == 0) 40.0 else 90.0, 30.0) }
        assertFalse(ActualRails.isCoherentRun(pts))
        assertNull(ActualRails.deviationRun(pts, listOf(route)))
    }

    @Test fun sameNameLabelsWithin300mKeepOne() {
        val labels = listOf(
            LabelDedup.GeoLabel(lat0, lon0, "Будогощь"),
            LabelDedup.GeoLabel(lat0 + 100 * mLat, lon0, "Будогощь"),
            LabelDedup.GeoLabel(lat0, lon0 + 200 * mLon, "Будогощь"),
            LabelDedup.GeoLabel(lat0, lon0 + 5000 * mLon, "Будогощь"), // далеко - своя подпись
            LabelDedup.GeoLabel(lat0, lon0, "Другая"),
        )
        // Центр карты ближе всего ко второй метке.
        val hidden = LabelDedup.sameNameDuplicates(labels, lat0 + 100 * mLat, lon0)
        assertEquals(setOf(0, 2), hidden)
        // Крайняя станция (приоритет 0) важнее ближней к центру.
        val withTerminal = labels.toMutableList().also { it[2] = it[2].copy(priority = 0) }
        assertEquals(setOf(0, 1), LabelDedup.sameNameDuplicates(withTerminal, lat0 + 100 * mLat, lon0))
    }
}
