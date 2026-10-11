package com.z_company.domain.util

import com.z_company.domain.track.TrackAnalysis
import com.z_company.domain.track.TrackPoint
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Оптимизации карты поезда (перенос/кэширование чистых функций) не должны менять результат. */
class MapPerformanceLogicTest {

    /** Эталон: прежняя полная проверка всех отрезков без отсева и без кэша. */
    private fun bruteProject(line: List<Pair<Double, Double>>, lat: Double, lon: Double, from: Int = 0): TrainMapRoute.LineProjection? {
        if (line.size < 2) return null
        val cum = DoubleArray(line.size)
        for (i in 1 until line.size) cum[i] = cum[i - 1] + TrackAnalysis.distanceM(line[i - 1].first, line[i - 1].second, line[i].first, line[i].second)
        val kx = kotlin.math.cos(lat * kotlin.math.PI / 180.0)
        var best: TrainMapRoute.LineProjection? = null
        for (i in from.coerceAtLeast(0) until line.size - 1) {
            val (aLat, aLon) = line[i]
            val (bLat, bLon) = line[i + 1]
            val dx = (bLon - aLon) * kx
            val dy = bLat - aLat
            val len2 = dx * dx + dy * dy
            val t = if (len2 == 0.0) 0.0 else ((((lon - aLon) * kx) * dx + (lat - aLat) * dy) / len2).coerceIn(0.0, 1.0)
            val pLat = aLat + dy * t
            val pLon = aLon + (bLon - aLon) * t
            val off = TrackAnalysis.distanceM(lat, lon, pLat, pLon)
            if (best == null || off < best.offsetM) best = TrainMapRoute.LineProjection(cum[i] + (cum[i + 1] - cum[i]) * t, off, pLat, pLon, i)
        }
        return best
    }

    private fun wanderingLine(n: Int, seed: Int): List<Pair<Double, Double>> {
        val rnd = Random(seed)
        var lat = 59.0
        var lon = 32.0
        return List(n) {
            lat += (rnd.nextDouble() - 0.4) * 0.0008
            lon += (rnd.nextDouble() - 0.3) * 0.0012
            lat to lon
        }
    }

    @Test
    fun projectWithPruningMatchesBruteForce() {
        for (seed in 1..6) {
            val line = wanderingLine(1500, seed)
            val cum = TrainMapRoute.cumulativeM(line)
            val rnd = Random(seed * 31)
            repeat(60) {
                val base = line[rnd.nextInt(line.size)]
                val lat = base.first + (rnd.nextDouble() - 0.5) * 0.002
                val lon = base.second + (rnd.nextDouble() - 0.5) * 0.003
                val from = if (it % 5 == 0) rnd.nextInt(line.size / 2) else 0
                val expected = bruteProject(line, lat, lon, from)
                val actual = TrainMapRoute.project(line, cum, lat, lon, from)
                assertNotNull(actual)
                assertEquals(expected!!.segment, actual.segment)
                assertEquals(expected.offsetM, actual.offsetM, 1e-6)
                assertEquals(expected.alongM, actual.alongM, 1e-6)
            }
        }
    }

    @Test
    fun cumulativeIsCachedByIdentityAndCorrect() {
        val line = wanderingLine(500, 3)
        val a = TrainMapRoute.cumulativeM(line)
        val b = TrainMapRoute.cumulativeM(line)
        assertSame(a, b)
        // Другой список с теми же значениями - свой результат, но равный по значениям.
        val copy = line.toList()
        val c = TrainMapRoute.cumulativeM(copy)
        assertTrue(a.contentEquals(c))
        // Короткие линии не кэшируются, но считаются верно.
        val short = listOf(59.0 to 32.0, 59.001 to 32.0)
        assertEquals(TrackAnalysis.distanceM(59.0, 32.0, 59.001, 32.0), TrainMapRoute.cumulativeM(short).last(), 1e-9)
    }

    @Test
    fun snapMarkedMatchesBruteForceProjectionOnLongLine() {
        val line = wanderingLine(2000, 9)
        val rnd = Random(5)
        val pts = List(300) { i ->
            val b = line[(i * 6) % line.size]
            TrackPoint(i * 5_000L, b.first + (rnd.nextDouble() - 0.5) * 0.0004, b.second + (rnd.nextDouble() - 0.5) * 0.0006, 40.0, 3f)
        }
        val snapped = ActualRails.snapMarked(pts, listOf(line))
        assertEquals(pts.size, snapped.size)
        snapped.forEachIndexed { i, s ->
            val ref = bruteProject(line, pts[i].latitude, pts[i].longitude)!!
            if (ref.offsetM <= TrainMapRoute.SNAP_TOLERANCE_M) {
                assertTrue(s.onRail)
                assertTrue(abs(s.point.latitude - ref.latitude) < 1e-9 && abs(s.point.longitude - ref.longitude) < 1e-9)
            } else {
                assertTrue(!s.onRail)
            }
        }
    }

    @Test
    fun sameNameDuplicatesKeepsOneNearestToCenter() {
        val labels = listOf(
            LabelDedup.GeoLabel(59.0000, 32.0, "Будогощь", 1),
            LabelDedup.GeoLabel(59.0010, 32.0, "будогощь", 1),
            LabelDedup.GeoLabel(59.0500, 32.0, "Будогощь", 1), // далеко - отдельная подпись
            LabelDedup.GeoLabel(59.0000, 32.0, "Луга", 1),
            LabelDedup.GeoLabel(59.0001, 32.0, "Луга I", 1), // «Луга» входит в «Луга I»
            LabelDedup.GeoLabel(59.0002, 32.0, "", 1),
        )
        val hidden = LabelDedup.sameNameDuplicates(labels, 59.0001, 32.0)
        // Ближе к центру остаются «Будогощь» (0) и «Луга I» (4); «будогощь» (1) и «Луга» (3) скрыты.
        assertEquals(setOf(1, 3), hidden)
    }

    @Test
    fun duplicatesWithBaseLabelsUsesRadiusAndName() {
        val app = listOf(
            AppMapLabel(100.0, 100.0, "Ёлкино"),
            AppMapLabel(100.0, 100.0, "Берёзки"),
            AppMapLabel(500.0, 500.0, "Ёлкино"),
            AppMapLabel(100.0, 100.0, ""),
        )
        val base = listOf(BaseMapLabel(110.0, 100.0, "елкино"), BaseMapLabel(100.0, 400.0, "Берёзки"))
        assertEquals(setOf(0), LabelDedup.duplicates(app, base, 40.0))
        assertEquals(emptySet(), LabelDedup.duplicates(app, emptyList(), 40.0))
    }

    @Test
    fun clusterStopsRunningSumMatchesDirectAverage() {
        val rnd = Random(2)
        val pts = ArrayList<TrackPoint>()
        var t = 0L
        repeat(400) {
            pts += TrackPoint(t, 59.0 + (rnd.nextDouble() - 0.5) * 0.0002, 32.0 + (rnd.nextDouble() - 0.5) * 0.0003, 0.5, 3f)
            t += 5_000L
        }
        val out = PathCleaner.clusterStops(pts)
        assertEquals(2, out.size) // один центроид + копия на время последней точки
        assertEquals(pts.sumOf { it.latitude } / pts.size, out[0].latitude)
        assertEquals(pts.sumOf { it.longitude } / pts.size, out[0].longitude)
    }

    @Test
    fun pathCleanCacheReturnsSameResultForSameInput() {
        val pts = List(50) { TrackPoint(it * 5_000L, 59.0 + it * 0.0003, 32.0, 40.0, 3f) }
        val cache = PathCleanCache()
        val a = cache.clean(pts)
        assertSame(a, cache.clean(pts.toList()))
        assertEquals(PathCleaner.clean(pts), a)
        val more = pts + TrackPoint(50 * 5_000L, 59.02, 32.0, 40.0, 3f)
        assertEquals(PathCleaner.clean(more), cache.clean(more))
    }
}
