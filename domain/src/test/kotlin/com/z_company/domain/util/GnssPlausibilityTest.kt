package com.z_company.domain.util

import com.z_company.domain.gnss.GnssLevel
import com.z_company.domain.gnss.GnssQualityEvaluator
import com.z_company.domain.track.TrackPoint
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GnssPlausibilityTest {
    // Кириши и Великий Новгород (по прямой ~100+ км).
    private val kirishiLat = 59.45
    private val kirishiLon = 32.02
    private val novgorodLat = 58.52
    private val novgorodLon = 31.27

    /** Сдвиг по широте на [m] метров (1 градус ~ 111.2 км). */
    private fun northM(m: Double) = m / 111_195.0

    @Test
    fun `jump from Kirishi to Novgorod in 5 s is rejected`() {
        val f = GnssPlausibility()
        assertTrue(f.check(0, kirishiLat, kirishiLon, 0.0).accepted)
        val r = f.check(5_000, novgorodLat, novgorodLon, 0.0)
        assertFalse(r.accepted)
        assertEquals(GnssPlausibility.Reason.JUMP, r.reason)
        assertEquals(kirishiLat to kirishiLon, f.lastAccepted)
        // возврат на правдоподобную позицию принимается
        assertTrue(f.check(6_000, kirishiLat, kirishiLon, 0.0).accepted)
    }

    @Test
    fun `normal movement 120 kmh is accepted`() {
        val f = GnssPlausibility()
        var lat = kirishiLat
        for (i in 0..120) {
            assertTrue(f.check(i * 1_000L, lat, kirishiLon, 120.0).accepted, "step $i")
            lat += northM(120_000.0 / 3600.0)
        }
        assertEquals(0, f.rejectedCount(120_000))
    }

    @Test
    fun `10 min signal loss then 12 km further is accepted`() {
        val f = GnssPlausibility()
        assertTrue(f.check(0, kirishiLat, kirishiLon, 60.0).accepted)
        assertTrue(f.check(600_000, kirishiLat + northM(12_000.0), kirishiLon, 60.0).accepted) // ~72 км/ч
    }

    @Test
    fun `five consistent fixes over 60 s in a new area switch the area`() {
        val f = GnssPlausibility()
        assertTrue(f.check(0, kirishiLat, kirishiLon, 0.0).accepted)
        val results = (0..5).map { i -> f.check(10_000L + i * 12_000L, novgorodLat, novgorodLon, 0.0) }
        assertTrue(results.take(5).none { it.accepted })
        assertTrue(results[5].accepted)
        assertEquals(GnssPlausibility.Reason.NEW_AREA, results[5].reason)
        assertTrue(f.check(80_000, novgorodLat, novgorodLon, 0.0).accepted)
    }

    @Test
    fun `five consistent fixes shorter than 60 s are not enough`() {
        val f = GnssPlausibility()
        assertTrue(f.check(0, kirishiLat, kirishiLon, 0.0).accepted)
        val results = (0..9).map { i -> f.check(10_000L + i * 2_000L, novgorodLat, novgorodLon, 0.0) }
        assertTrue(results.none { it.accepted })
    }

    @Test
    fun `alternating spoofed areas never form a cluster`() {
        val f = GnssPlausibility()
        assertTrue(f.check(0, kirishiLat, kirishiLon, 0.0).accepted)
        for (i in 1..30) {
            val novg = i % 2 == 1
            val r = f.check(i * 5_000L, if (novg) novgorodLat else kirishiLat, if (novg) novgorodLon else kirishiLon, 0.0)
            assertEquals(!novg, r.accepted, "step $i")
        }
    }

    @Test
    fun `claimed speed 1200 kmh is rejected`() {
        val f = GnssPlausibility()
        assertTrue(f.check(0, kirishiLat, kirishiLon, 50.0).accepted)
        val r = f.check(1_000, kirishiLat, kirishiLon, 1200.0)
        assertFalse(r.accepted)
        assertEquals(GnssPlausibility.Reason.SPEED_CLAIM, r.reason)
    }

    @Test
    fun `spoof 150 km away with network nearby is rejected even for first fix`() {
        val f = GnssPlausibility()
        val net = GnssPlausibility.NetworkFix(kirishiLat, kirishiLon, 500f, ageMs = 20_000)
        val r = f.check(0, novgorodLat, novgorodLon, 0.0, network = net)
        assertFalse(r.accepted)
        assertEquals(GnssPlausibility.Reason.NETWORK_MISMATCH, r.reason)
        assertEquals(net, r.landmark)
        assertTrue(f.check(1_000, kirishiLat, kirishiLon, 0.0, network = net).accepted)
    }

    @Test
    fun `no network keeps old logic and stale network is ignored`() {
        val f = GnssPlausibility()
        assertTrue(f.check(0, kirishiLat, kirishiLon, 0.0, network = null).accepted)
        assertTrue(f.check(10_000, kirishiLat + northM(200.0), kirishiLon, 70.0, network = null).accepted)
        assertFalse(f.check(11_000, novgorodLat, novgorodLon, 70.0, network = null).accepted)
        val stale = GnssPlausibility.NetworkFix(novgorodLat, novgorodLon, 300f, ageMs = 5 * 60_000L)
        assertTrue(f.check(20_000, kirishiLat + northM(400.0), kirishiLon, 70.0, network = stale).accepted)
    }

    @Test
    fun `network lagging by 3 km does not reject normal movement`() {
        val f = GnssPlausibility()
        val net = GnssPlausibility.NetworkFix(kirishiLat - northM(3_000.0), kirishiLon, 800f, ageMs = 30_000)
        var lat = kirishiLat
        for (i in 0..30) {
            assertTrue(f.check(i * 1_000L, lat, kirishiLon, 100.0, network = net).accepted, "step $i")
            lat += northM(100_000.0 / 3600.0)
        }
    }

    @Test
    fun `rejected count window and interference level`() {
        val f = GnssPlausibility()
        f.check(0, kirishiLat, kirishiLon, 0.0)
        f.check(1_000, novgorodLat, novgorodLon, 0.0)
        assertEquals(1, f.rejectedCount(1_000))
        f.check(2_000, novgorodLat, novgorodLon, 0.0)
        assertEquals(2, f.rejectedCount(2_000))
        assertEquals(0, f.rejectedCount(70_000))

        val e = GnssQualityEvaluator()
        assertEquals(GnssLevel.INTERFERENCE, e.update(10_000, 0, 9_000, 5f, 8, 8, rejectedRecent = 2).level)
        assertEquals(GnssLevel.INTERFERENCE, e.update(11_000, 0, 10_900, 5f, 8, 8, rejectedRecent = 3).level)
        // вышли из помехи: улучшение сглажено (5 с стабильно хорошего значения)
        e.update(12_000, 0, 11_900, 5f, 8, 8, rejectedRecent = 0)
        assertEquals(GnssLevel.GOOD, e.update(18_000, 0, 17_900, 5f, 8, 8, rejectedRecent = 0).level)
    }

    @Test
    fun `sanitize drops spoofed points from a stored track`() {
        fun p(t: Long, lat: Double, lon: Double) = TrackPoint(t, lat, lon, 50.0, 5f)
        val pts = listOf(
            p(0, kirishiLat, kirishiLon), p(5_000, novgorodLat, novgorodLon),
            p(10_000, kirishiLat + northM(100.0), kirishiLon),
        )
        assertEquals(listOf(0L, 10_000L), GnssPlausibility.sanitize(pts).map { it.timeMs })
    }

    // --- Выезд из зоны глушения (Кириши): восстановление карты ---

    /** 20 минут в Киришах с подменой (прыжки в Новгород), затем выезд на 30 км и нормальные фиксы. */
    private fun kirishiThenLeave(f: GnssPlausibility, network: (Long) -> GnssPlausibility.NetworkFix?): Long {
        var t = 0L
        while (t < 20 * 60_000L) { // 20 мин: стоим в Киришах, каждый 3-й фикс - подмена
            val spoof = (t / 5_000L) % 3L == 1L
            f.check(t, if (spoof) novgorodLat else kirishiLat, if (spoof) novgorodLon else kirishiLon, 0.0, network = network(t))
            t += 5_000L
        }
        // Выехали: 30 км севернее, фиксы раз в секунду на ~60 км/ч (нормальные, согласованные).
        val start = t + 10_000L
        var firstAccepted = -1L
        for (i in 0..120) {
            val ts = start + i * 1_000L
            val lat = kirishiLat + northM(30_000.0 + i * 16.7)
            val r = f.check(ts, lat, kirishiLon, 60.0, network = network(ts))
            if (r.accepted && firstAccepted < 0) firstAccepted = ts - start
        }
        return firstAccepted
    }

    @Test
    fun `leaving Kirishi after 20 min of spoofing new area is accepted within 60 s`() {
        val accepted = kirishiThenLeave(GnssPlausibility()) { null }
        assertTrue(accepted in 0..60_000L, "accepted at $accepted")
    }

    @Test
    fun `stale network stuck in Kirishi does not block forever`() {
        // Сеть залипла: свежая по времени, но показывает Кириши (старая область), точность разумная.
        val net = { _: Long -> GnssPlausibility.NetworkFix(kirishiLat, kirishiLon, 800f, ageMs = 10_000) }
        val accepted = kirishiThenLeave(GnssPlausibility(), net)
        assertTrue(accepted in 0..130_000L, "accepted at $accepted")
    }

    @Test
    fun `network that does not confirm the last accepted position does not block`() {
        val f = GnssPlausibility()
        assertTrue(f.check(0, kirishiLat, kirishiLon, 0.0).accepted)
        val net = GnssPlausibility.NetworkFix(novgorodLat, novgorodLon, 800f, ageMs = 10_000) // ни там, ни там
        val far = kirishiLat + northM(30_000.0)
        val results = (0..5).map { i -> f.check(10_000L + i * 12_000L, far, kirishiLon, 0.0, network = net) }
        assertTrue(results.last().accepted) // сработал обычный NEW_AREA, сеть не мешает
    }

    @Test
    fun `network with poor accuracy is ignored`() {
        val f = GnssPlausibility()
        val net = GnssPlausibility.NetworkFix(kirishiLat, kirishiLon, 8_000f, ageMs = 10_000)
        assertTrue(f.check(0, novgorodLat, novgorodLon, 0.0, network = net).accepted)
    }

    @Test
    fun `long silence over 5 min drops the sticky anchor`() {
        val f = GnssPlausibility()
        assertTrue(f.check(0, kirishiLat, kirishiLon, 0.0).accepted)
        // 6 мин молчания, затем сразу 150 км (нереально быстро для якоря, но якорь уже снят)
        val r = f.check(6 * 60_000L + 1_000L, novgorodLat, novgorodLon, 0.0)
        assertTrue(r.accepted)
        assertEquals(GnssPlausibility.Reason.FIRST, r.reason)
    }

    @Test
    fun `time sanity and outliers`() {
        val now = 1_700_000_000_000L
        assertTrue(GnssPlausibility.isTimeSane(now + 60_000, now))
        assertFalse(GnssPlausibility.isTimeSane(now + 396L * 24 * 3_600_000L, now))
        fun p(t: Long) = TrackPoint(t, kirishiLat, kirishiLon, 50.0, 5f)
        val pts = listOf(p(now), p(now + 5_000), p(now + 396L * 24 * 3_600_000L), p(now + 10_000))
        assertEquals(listOf(now, now + 5_000, now + 10_000), GnssPlausibility.dropTimeOutliers(pts).map { it.timeMs })
    }
}
