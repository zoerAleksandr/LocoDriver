package com.z_company.domain.track

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AltitudeContributionTest {
    private val mPerDeg = 111_320.0

    /** Ось по меридиану: км k -> широта 55 + k*1000/111320, долгота 37. */
    private fun axis(lengthKm: Int = 10, hash: String = "a".repeat(32)) = SectionAxis(
        "sec-1", hash,
        (0..lengthKm).map { AxisPoint(it.toDouble(), 55.0 + it * 1000 / mPerDeg, 37.0) },
    )

    private fun sample(kmM: Double, offEastM: Double = 0.0, alt: Double? = 100.0, baro: Double? = null, acc: Float = 5f): AltitudeSample {
        val lat = 55.0 + kmM / mPerDeg
        val lon = 37.0 + offEastM / (mPerDeg * kotlin.math.cos(lat * 0.017453292519943295))
        return AltitudeSample(lat, lon, alt, baro, acc)
    }

    @Test
    fun projectionGivesKmAndOffset() {
        val lat = 55.0 + 2500 / mPerDeg
        val lon = 37.0 + 12.0 / (mPerDeg * kotlin.math.cos(lat * 0.017453292519943295))
        val pr = assertNotNull(AxisProjector.project(axis(), lat, lon))
        assertTrue(abs(pr.kmM - 2500.0) < 2.0, "km=${pr.kmM}")
        assertTrue(abs(pr.offsetM - 12.0) < 0.5, "off=${pr.offsetM}")
    }

    @Test
    fun projectionClampsToAxisEnds() {
        val pr = assertNotNull(AxisProjector.project(axis(), 55.0 - 100 / mPerDeg, 37.0))
        assertEquals(0.0, pr.kmM, 0.01)
        assertTrue(abs(pr.offsetM - 100.0) < 1.0)
    }

    @Test
    fun qualityFilter() {
        fun p(acc: Float?, speed: Double?, alt: Double?, baro: Double? = null) =
            TrackPoint(0, 55.0, 37.0, speed, acc, alt, baro)
        assertNotNull(AltitudeContribBuilder.accept(p(20f, 60.0, 100.0)))
        assertNull(AltitudeContribBuilder.accept(p(20.5f, 60.0, 100.0)))
        assertNull(AltitudeContribBuilder.accept(p(null, 60.0, 100.0)))
        assertNull(AltitudeContribBuilder.accept(p(5f, null, 100.0)))
        assertNull(AltitudeContribBuilder.accept(p(5f, 60.0, null)))
        assertNull(AltitudeContribBuilder.accept(p(5f, 60.0, 99999.0)))
        assertNotNull(AltitudeContribBuilder.accept(p(5f, 0.0, null, 80.0)))
    }

    @Test
    fun buildsCellsWithNullsAndOffsets() {
        // Точки через 100 м с 1000 до 2000 м, высота растёт на 1 м на точку, отклонение 4 м.
        val samples = (0..10).map { sample(1000.0 + it * 100, offEastM = 4.0, alt = 100.0 + it) }
        val frags = AltitudeContribBuilder.build(axis(), samples)
        assertEquals(1, frags.size)
        val f = frags.single()
        assertEquals(1000, f.kmFromM)
        assertEquals(25, f.stepM)
        assertEquals("gnss", f.source)
        assertEquals(41, f.altsDm.size) // 1000..2000 м, шаг 25 м
        assertEquals(f.altsDm.size, f.offsetM.size)
        assertNotNull(f.altsDm[0]); assertNull(f.altsDm[1]); assertNull(f.altsDm[2]); assertNull(f.altsDm[3]); assertNotNull(f.altsDm[4])
        assertEquals(f.altsDm.map { it == null }, f.offsetM.map { it == null })
        assertTrue(abs(f.offsetM[0]!! - 4.0) < 0.5)
        assertEquals(5.0, f.meanAccM!!, 0.001)
    }

    @Test
    fun dropsPointsFartherThan30mAndShortFragments() {
        val far = (0..10).map { sample(1000.0 + it * 100, offEastM = 31.0) }
        assertTrue(AltitudeContribBuilder.build(axis(), far).isEmpty())
        val few = (0..2).map { sample(1000.0 + it * 100) }
        assertTrue(AltitudeContribBuilder.build(axis(), few).isEmpty())
    }

    @Test
    fun prefersBaroWhenHalfOrMorePointsHaveIt() {
        val s = (0..9).map { sample(1000.0 + it * 100, alt = 500.0, baro = 120.0) }
        val f = AltitudeContribBuilder.build(axis(), s).single()
        assertEquals("baro", f.source)
        assertEquals(1200L, f.altsDm.first { it != null })
        val onlyGps = (0..9).map { sample(1000.0 + it * 100, alt = 500.0, baro = if (it == 0) 120.0 else null) }
        assertEquals("gnss", AltitudeContribBuilder.build(axis(), onlyGps).single().source)
    }

    @Test
    fun smoothingSuppressesSpike() {
        val s = (0..9).map { sample(1000.0 + it * 25, alt = if (it == 5) 400.0 else 100.0) }
        val f = AltitudeContribBuilder.build(axis(), s).single()
        assertTrue(f.altsDm.all { it == 1000L }, f.altsDm.toString())
    }

    @Test
    fun splitsOnLongGap() {
        val a = (0..9).map { sample(500.0 + it * 25) }
        val b = (0..9).map { sample(8000.0 + it * 25) }
        val frags = AltitudeContribBuilder.build(axis(), a + b)
        assertEquals(2, frags.size)
        assertEquals(500, frags[0].kmFromM)
        assertEquals(8000, frags[1].kmFromM)
    }

    @Test
    fun codecMatchesServerRawBytes() {
        // Эталон получен из backend/src/services/track_codec.py (encode_contrib до zlib).
        val alts = listOf(1234L, null, null, 1240L, 1200L, -50L, null, 90000L)
        val expected = intArrayOf(165, 19, 0, 0, 13, 80, 196, 19, 0, 133, 255, 10)
        val raw = AltitudeContribCodec.encodeRaw(alts)
        assertContentEquals(expected, raw.map { it.toInt() and 0xFF }.toIntArray())
        assertEquals(alts, AltitudeContribCodec.decodeRaw(raw))
        val packed = AltitudeContribCodec.encode(alts, JvmTrackBlobTools)
        assertEquals(alts, AltitudeContribCodec.decodeRaw(JvmTrackBlobTools.inflate(packed, 1000)))
    }

    @Test
    fun contribIdIsDeterministicUuidShaped() {
        val f = ContribFragment("sec-1", "a".repeat(32), 1000, 25, listOf(1L), listOf(0.0), "gnss", null)
        val id1 = AltitudeContribCodec.contribId("sess", f, JvmTrackBlobTools)
        assertEquals(id1, AltitudeContribCodec.contribId("sess", f, JvmTrackBlobTools))
        assertTrue(Regex("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}").matches(id1), id1)
        assertTrue(id1 != AltitudeContribCodec.contribId("sess2", f, JvmTrackBlobTools))
        assertTrue(id1 != AltitudeContribCodec.contribId("sess", f.copy(kmFromM = 1025), JvmTrackBlobTools))
    }

    @Test
    fun bufferThinsFiltersAndTextRoundTrips() {
        val buf = AltitudeSessionBuffer()
        fun p(t: Long, acc: Float = 5f) = TrackPoint(t, 55.5, 37.5, 60.0, acc, 123.4, null)
        assertTrue(buf.add(p(0)))
        assertTrue(!buf.add(p(1000))) // прорежена
        assertTrue(!buf.add(p(6000, acc = 50f))) // плохая точность
        assertTrue(buf.add(p(6000)))
        val list = buf.drain()
        assertEquals(2, list.size)
        assertEquals(0, buf.size)
        val s = PendingContribSession("id-1", 42L, list + AltitudeSample(1.0, 2.0, null, 3.0, null))
        assertEquals(s.samples, AltitudeContribText.decodeSession(AltitudeContribText.encodeSession(s))!!.samples)
        val ax = axis()
        val back = AltitudeContribText.decodeAxis(AltitudeContribText.encodeAxis(ax))!!
        assertEquals(ax.axisHash, back.axisHash)
        assertEquals(ax.points, back.points)
    }
}
