package com.z_company.domain.util

import com.z_company.domain.track.InMemoryTrackRepository
import com.z_company.domain.track.TrackPoint
import com.z_company.domain.track.TrackWriter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DriftFilterTest {
    private val lat0 = 59.0
    private val lon0 = 32.0

    private fun lat(northM: Double) = lat0 + northM / 111_195.0
    private fun lon(eastM: Double) = lon0 + eastM / (111_195.0 * cos(lat0 * PI / 180.0))
    private fun distM(la: Double, lo: Double, northM: Double, eastM: Double) =
        GnssPlausibility.distanceM(la, lo, lat(northM), lon(eastM))

    private fun fix(tSec: Int, north: Double, east: Double, speed: Double?, acc: Float = 4f, bearing: Float? = null) =
        DriftFilter.Fix(tSec * 1000L, lat(north), lon(east), speed, acc, bearing)

    /** Стоянка [fromSec, toSec) с гладким дрейфом (амплитуда ~40 м по каждой оси, период 3-4 мин), скорость 0. */
    private fun standFix(t: Int, rnd: Random, jump: Pair<Double, Double>? = null): DriftFilter.Fix {
        val n = 40.0 * sin(2 * PI * t / 240.0) + rnd.nextDouble(-2.0, 2.0)
        val e = 35.0 * sin(2 * PI * t / 170.0 + 1.0) + rnd.nextDouble(-2.0, 2.0)
        val (jn, je) = jump ?: (0.0 to 0.0)
        return fix(t, n + jn, e + je, 0.0, acc = rnd.nextDouble(2.0, 7.0).toFloat())
    }

    @Test
    fun `stand for 10 minutes with drift and one 280 m jump stays pinned and writes few points`() {
        val f = DriftFilter()
        val rnd = Random(1)
        val repo = InMemoryTrackRepository()
        val writer = TrackWriter(repo, newTrackId = { "t" })
        var maxDev = 0.0
        var held = 0
        val outs = mutableListOf<DriftFilter.Result>()
        for (t in 0 until 600) {
            // один выброс на 280 м в течение 3 фиксов на 300-й секунде
            val jump = if (t in 300..302) 280.0 to 0.0 else null
            val x = standFix(t, rnd, jump)
            val r = f.process(x)
            outs += r
            if (r.held) held++
            if (t >= 20) maxDev = max(maxDev, distM(r.latitude, r.longitude, 0.0, 0.0))
            writer.onPoint("train", TrackPoint(x.timeMs, r.latitude, r.longitude, x.speedKmh, x.accuracyM), consent = true, held = r.held)
        }
        // после входа в STILL (15 с) позиция всегда у якоря, в пределах 50 м от истинной стоянки (сырой дрейф и выброс 280 м не прошли)
        assertTrue(maxDev < 50.0, "maxDev=$maxDev")
        assertTrue(held >= 570, "held=$held")
        // раз в минуту одна точка: за 10 минут не больше ~15 точек вместо 600
        val written = repo.points(repo.tracksForTrain("train").first().trackId).size
        assertTrue(written <= 15, "written=$written")
        // позиции записанных точек не разбросаны (дрейф не попал в трек)
        val pts = repo.points(repo.tracksForTrain("train").first().trackId).drop(1)
        val anchor = pts.first()
        assertTrue(pts.all { GnssPlausibility.distanceM(it.latitude, it.longitude, anchor.latitude, anchor.longitude) < 1.0 })
    }

    @Test
    fun `persistent relocation at speed zero is accepted only after a consistent series`() {
        val f = DriftFilter()
        val rnd = Random(2)
        for (t in 0 until 60) f.process(standFix(t, rnd))
        // 280 м на север, и приёмник «залип» там
        val results = (60 until 120).map { t ->
            t to f.process(fix(t, 280.0 + rnd.nextDouble(-3.0, 3.0), rnd.nextDouble(-3.0, 3.0), 0.0))
        }
        // первые ~20 с - у старого якоря (новая область не подтверждена)
        for ((t, r) in results.filter { it.first < 70 }) {
            assertTrue(distM(r.latitude, r.longitude, 0.0, 0.0) < 60.0, "t=$t")
        }
        // потом якорь переехал
        val last = results.last().second
        assertTrue(distM(last.latitude, last.longitude, 280.0, 0.0) < 10.0)
        assertTrue(results.any { it.second.reason == DriftFilter.Reason.NEW_AREA })
    }

    @Test
    fun `slow movement 3 to 8 kmh with 10 m noise is confirmed fast and passes through`() {
        val f = DriftFilter()
        val rnd = Random(3)
        for (t in 0 until 40) f.process(fix(t, rnd.nextDouble(-2.0, 2.0), rnd.nextDouble(-2.0, 2.0), 0.0, acc = 5f))
        assertTrue(f.isStill)
        var north = 0.0
        var heldAfterStart = 0
        var confirmedAt = -1
        val diffs = mutableListOf<Double>()
        val kmh = 3.0
        // скорость плавно растёт с 3 до 8 км/ч
        for (i in 0 until 150) {
            val v = kmh + 5.0 * i / 150.0
            north += v / 3.6
            val t = 40 + i
            val trueN = north
            val x = fix(t, trueN + rnd.nextDouble(-10.0, 10.0), rnd.nextDouble(-10.0, 10.0), v + rnd.nextDouble(-0.3, 0.3), acc = 10f, bearing = 0f)
            val r = f.process(x)
            if (r.held) heldAfterStart++ else if (confirmedAt < 0) confirmedAt = i
            if (!r.held) diffs += GnssPlausibility.distanceM(r.latitude, r.longitude, x.latitude, x.longitude)
        }
        assertTrue(confirmedAt in 0..10, "confirmedAt=$confirmedAt")
        assertTrue(heldAfterStart <= 10, "heldAfterStart=$heldAfterStart")
        assertTrue(diffs.all { it < 0.01 }) // принятые позиции исходные, не изменены
        assertTrue(!f.isStill)
    }

    @Test
    fun `shunting reverse keeps every point`() {
        val f = DriftFilter()
        val rnd = Random(4)
        var north = 0.0
        var held = 0
        var t = 0
        // 30 с вперёд 6 км/ч, 4 с стоим (разворот), 30 с назад 6 км/ч, всё в одном движении без стоянки >15 с
        repeat(30) { north += 6 / 3.6; if (f.process(fix(t++, north + rnd.nextDouble(-8.0, 8.0), rnd.nextDouble(-8.0, 8.0), 6.0, 10f, 0f)).held) held++ }
        repeat(4) { if (f.process(fix(t++, north + rnd.nextDouble(-8.0, 8.0), rnd.nextDouble(-8.0, 8.0), 0.0, 10f)).held) held++ }
        repeat(30) { north -= 6 / 3.6; if (f.process(fix(t++, north + rnd.nextDouble(-8.0, 8.0), rnd.nextDouble(-8.0, 8.0), 6.0, 10f, 180f)).held) held++ }
        assertEquals(0, held)
    }

    @Test
    fun `reverse after a long stand is confirmed with opposite bearing`() {
        val f = DriftFilter()
        val rnd = Random(5)
        for (t in 0 until 60) f.process(fix(t, rnd.nextDouble(-2.0, 2.0), rnd.nextDouble(-2.0, 2.0), 0.0, 5f))
        var east = 0.0
        var held = 0
        for (i in 0 until 40) {
            east -= 8 / 3.6 // назад: на запад
            val r = f.process(fix(60 + i, rnd.nextDouble(-2.0, 2.0), east + rnd.nextDouble(-2.0, 2.0), 8.0, 5f, 270f))
            if (r.held) held++
        }
        assertTrue(held <= 8, "held=$held")
        assertTrue(!f.isStill)
    }

    @Test
    fun `acceleration from stand does not lose the departure`() {
        val f = DriftFilter()
        val rnd = Random(6)
        for (t in 0 until 120) f.process(standFix(t, rnd))
        assertTrue(f.isStill)
        var along = 0.0
        var v = 0.0
        var firstFree = -1
        val kept = mutableListOf<Pair<Int, DriftFilter.Result>>()
        for (i in 0 until 60) {
            v = minOf(50.0, v + 1.5) // ~0.4 м/с^2
            along += v / 3.6
            val r = f.process(fix(120 + i, along, 0.0, v, 3f, 0f))
            if (!r.held && firstFree < 0) firstFree = i
            kept += i to r
        }
        // допплер >= 3 км/ч с 2-го фикса; подтверждение за несколько секунд, после чего все точки исходные
        assertTrue(firstFree in 1..8, "firstFree=$firstFree")
        assertTrue(kept.filter { it.first >= firstFree }.all { !it.second.held })
        // последняя позиция - реальная, а не у якоря
        val last = kept.last().second
        assertTrue(distM(last.latitude, last.longitude, along, 0.0) < 1.0)
    }

    @Test
    fun `gps loss and return while moving passes through immediately`() {
        val f = DriftFilter()
        var north = 0.0
        for (t in 0 until 60) { north += 50 / 3.6; f.process(fix(t, north, 0.0, 50.0, 3f, 0f)) }
        // потеря сигнала на 3 минуты, поезд проехал ещё 2.5 км
        north += 2_500.0
        val r = f.process(fix(60 + 180, north, 0.0, 50.0, 3f, 0f))
        assertTrue(!r.held)
        assertEquals(lat(north), r.latitude, 1e-9)
    }

    @Test
    fun `gps loss at stand resets the anchor so a train that left is not pulled back`() {
        val f = DriftFilter()
        val rnd = Random(7)
        for (t in 0 until 60) f.process(standFix(t, rnd))
        assertTrue(f.isStill)
        // 5 минут тишины, возврат в 2 км от старой стоянки; скорость 0 (встали на следующей станции)
        val r = f.process(fix(360, 2_000.0, 0.0, 0.0, 4f))
        assertTrue(!f.isStill)
        assertEquals(lat(2_000.0), r.latitude, 1e-9)
    }

    @Test
    fun `doppler spike with chaotic positions is not confirmed`() {
        val f = DriftFilter()
        val rnd = Random(8)
        for (t in 0 until 60) f.process(standFix(t, rnd))
        // 6 фиксов: допплер 4 км/ч, но позиции прыгают на десятки метров в разные стороны
        var confirmed = false
        for (i in 0 until 6) {
            val sgn = if (i % 2 == 0) 1.0 else -1.0
            val r = f.process(fix(60 + i, 70.0 * sgn, 70.0 * sgn, 4.0, 4f, 90f))
            if (r.reason == DriftFilter.Reason.CONFIRMED_MOVE) confirmed = true
        }
        assertTrue(!confirmed)
        assertTrue(f.isStill)
    }

    @Test
    fun `unknown speed far from anchor eventually releases the position`() {
        val f = DriftFilter()
        val rnd = Random(9)
        for (t in 0 until 60) f.process(fix(t, rnd.nextDouble(-2.0, 2.0), rnd.nextDouble(-2.0, 2.0), 0.0, 5f))
        // скорость датчиком не сообщается, поезд уходит на восток 36 км/ч: страховка отпускает не позже ~30 с
        var east = 0.0
        var last: DriftFilter.Result? = null
        for (i in 0 until 40) {
            east += 10.0
            last = f.process(fix(60 + i, 0.0, east, null, 5f))
        }
        assertTrue(!last!!.held)
        assertEquals(lon(east), last.longitude, 1e-9)
    }

    @Test
    fun `constant fast movement is never held`() {
        val f = DriftFilter()
        var north = 0.0
        for (t in 0 until 300) {
            north += 20.0
            assertTrue(!f.process(fix(t, north, 0.0, 72.0, 3f, 0f)).held, "t=$t")
        }
    }
}
