package com.z_company.domain.gnss

import com.z_company.domain.track.TrackPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GnssSharedZonesTest {
    private val lat0 = 59.45
    private val lon0 = 32.0

    private fun p(tSec: Long) = TrackPoint(
        timeMs = 1_700_000_000_000L + tSec * 1000, latitude = lat0 + tSec * 0.00015, longitude = lon0,
        speedKmh = 60.0, accuracyM = 5f,
    )

    private fun moving(from: Long, to: Long) = (from..to step 5).map { p(it) }

    @Test
    fun kindMaskRoundTrips() {
        assertEquals(1, GnssKindMask.of(GnssBadKind.GAP))
        assertEquals(2, GnssKindMask.of(GnssBadKind.POOR_ACCURACY))
        assertEquals(4, GnssKindMask.of(GnssBadKind.SPEED_JUMP))
        assertEquals(5, GnssKindMask.toMask(setOf(GnssBadKind.GAP, GnssBadKind.SPEED_JUMP)))
        assertEquals(setOf(GnssBadKind.GAP, GnssBadKind.POOR_ACCURACY), GnssKindMask.fromMask(3))
        assertTrue(GnssKindMask.fromMask(0).isEmpty())
    }

    @Test
    fun gridMatchesServerVectors() {
        // Те же формулы, что backend/src/services/gnss_grid.py: строка = floor((lat+90)/0.00225), шаг по долготе - целые микроградусы.
        for ((la, lo) in listOf(-33.9 to 151.2, 0.0 to 0.0, 69.0 to -179.99, 43.1 to 131.9, 59.4512 to 32.0123)) {
            val c = GnssGrid.cellOf(la, lo)
            val ctr = GnssGrid.center(c)
            assertEquals(c, GnssGrid.cellOf(ctr.latitude, ctr.longitude))
        }
        assertEquals(kotlin.math.floor((59.4512 + 90) / 0.00225).toInt(), GnssGrid.cellOf(59.4512, 32.0123).iy)
        assertTrue(GnssGrid.cellOf(0.0, 0.0).iy in 0 until 80_000)
    }

    @Test
    fun builderTurnsGapIntoCellsWithMaskAndCount() {
        val pts = moving(0, 100) + moving(145, 200)
        val cells = GnssContribBuilder.fromPoints(pts)
        assertTrue(cells.isNotEmpty())
        assertTrue(cells.all { it.kinds == GnssKindMask.GAP && it.count == 1 })
        assertTrue(cells.size <= GnssContribBuilder.MAX_CELLS)
        // Отсортировано и без повторов: порядок поездки не раскрывается.
        assertEquals(cells.map { it.cell }.distinct().sortedWith(compareBy({ it.iy }, { it.ix })), cells.map { it.cell })
    }

    @Test
    fun cleanTrackGivesEmptyContribution() {
        assertTrue(GnssContribBuilder.fromPoints(moving(0, 600)).isEmpty())
        assertTrue(GnssContribBuilder.fromPoints(emptyList()).isEmpty())
    }

    @Test
    fun builderMergesKindsAndCountsOverlappingSpans() {
        val anchors = listOf(GnssPos(lat0, lon0), GnssPos(lat0 + 0.0005, lon0))
        val spans = listOf(
            GnssBadSpan(GnssBadKind.GAP, 0, 60_000, anchors),
            GnssBadSpan(GnssBadKind.SPEED_JUMP, 70_000, 90_000, anchors),
        )
        val cells = GnssContribBuilder.fromSpans(spans)
        assertTrue(cells.all { it.kinds == (GnssKindMask.GAP or GnssKindMask.JUMP) && it.count == 2 })
    }

    @Test
    fun utcDayFormat() {
        assertEquals("1970-01-01", GnssContribBuilder.utcDay(0L))
        assertEquals("2023-11-14", GnssContribBuilder.utcDay(1_700_000_000_000L))
        assertEquals("1969-12-31", GnssContribBuilder.utcDay(-1L))
    }

    @Test
    fun sessionBufferThinsAndCaps() {
        val b = GnssSessionBuffer(maxPoints = 3)
        // Точки раз в 1 с в движении прореживаются до одной в 5 с.
        for (t in 0..9L) b.add(p(t).copy(timeMs = 1_000L * t))
        assertEquals(2, b.size)
        val b2 = GnssSessionBuffer(maxPoints = 2)
        for (t in 0..60L step 5) b2.add(p(t))
        assertEquals(2, b2.size)
        assertEquals(2, b2.drain().size)
        assertEquals(0, b2.size)
    }

    @Test
    fun contribTextRoundTrips() {
        val s = GnssContribSession("abc-1", 5L, listOf(GnssContribCell(GnssCell(40133, 123456), 3, 2), GnssContribCell(GnssCell(1, 2), 4, 1)))
        val back = GnssContribText.decode(GnssContribText.encode(s))!!
        assertEquals(s.id, back.id)
        assertEquals(s.createdAtMs, back.createdAtMs)
        assertEquals(s.cells, back.cells)
        assertNull(GnssContribText.decode("garbage"))
        assertNull(GnssContribText.decode("G1,x,5\n1,2,9,1\n"))
    }

    @Test
    fun tileTextRoundTrips() {
        val t = GnssTileCache("\"abc\"", 77L, listOf(GnssServerCell(GnssCell(5, 6), 2, 3)))
        assertEquals(t, GnssTileText.decode(GnssTileText.encode(t)))
        val noEtag = GnssTileCache(null, 1L, emptyList())
        assertEquals(noEtag, GnssTileText.decode(GnssTileText.encode(noEtag)))
        assertNull(GnssTileText.decode("T1,x"))
    }

    private class MemTag : GnssTagStorage {
        var t: String? = null
        var at = 0L
        override fun tag() = t
        override fun createdAtMs() = at
        override fun save(tag: String, createdAtMs: Long) { t = tag; at = createdAtMs }
        override fun clear() { t = null; at = 0 }
    }

    @Test
    fun tagRotatesEvery30DaysAndIsStableBefore() {
        val storage = MemTag()
        var n = 0
        val tag = GnssContribTag(storage, newTag = { "tag-${++n}" })
        val day = 24L * 60 * 60 * 1000
        val t0 = 10_000_000_000L
        assertEquals("tag-1", tag.current(t0))
        assertEquals("tag-1", tag.current(t0 + 29 * day))
        assertEquals("tag-2", tag.current(t0 + 30 * day))
        // Часы ушли назад - тоже ротация, tag не живёт вечно.
        assertEquals("tag-3", tag.current(t0))
        tag.reset()
        assertNull(storage.t)
        assertEquals("tag-4", tag.current(t0))
    }

    @Test
    fun randomTagLooksLikeUuidAndDiffers() {
        val a = GnssContribTag.randomTag()
        assertEquals(36, a.length)
        assertNotEquals(a, GnssContribTag.randomTag())
    }

    private fun localZone(cell: GnssCell, tracks: Int): GnssZone {
        val pos = GnssGrid.center(cell)
        return GnssZone(cell, pos.latitude, pos.longitude, tracks, setOf(GnssBadKind.GAP), 1L, 60_000L)
    }

    @Test
    fun mergeUnitesLocalAndServerInOneLayer() {
        val a = GnssCell(100, 200)
        val b = GnssCell(100, 201)
        val c = GnssCell(100, 202)
        val merged = GnssZoneMerge.merge(
            local = listOf(localZone(a, 1), localZone(b, 2)),
            server = listOf(GnssServerCell(b, 3, 2), GnssServerCell(c, 1, 5)),
        )
        assertEquals(listOf(a, b, c), merged.map { it.cell })
        val ma = merged[0]
        val mb = merged[1]
        val mc = merged[2]
        assertFalse(ma.isShared)
        assertEquals(0.25, ma.weight, 1e-9)
        assertTrue(mb.isShared && mb.local != null)
        assertEquals(3, mb.serverLevel)
        assertEquals(1.0, mb.weight, 1e-9)
        assertNull(mc.local)
        assertEquals(setOf(GnssBadKind.GAP, GnssBadKind.SPEED_JUMP), mc.serverKinds)
        assertEquals(0.5, mc.weight, 1e-9)
        val ctr = GnssGrid.center(c)
        assertEquals(ctr.latitude, mc.latitude, 1e-12)
    }

    @Test
    fun mergeDedupesServerDuplicatesAndIgnoresBadLevels() {
        val a = GnssCell(7, 8)
        val m = GnssZoneMerge.merge(
            emptyList(),
            listOf(GnssServerCell(a, 1, 1), GnssServerCell(a, 2, 2), GnssServerCell(GnssCell(9, 9), 0, 1), GnssServerCell(GnssCell(9, 10), 4, 1)),
        )
        assertEquals(1, m.size)
        assertEquals(2, m.single().serverLevel)
        assertEquals(setOf(GnssBadKind.GAP, GnssBadKind.POOR_ACCURACY), m.single().serverKinds)
    }

    @Test
    fun serverOnlyOrLocalOnlyAndLevelWords() {
        assertTrue(GnssZoneMerge.merge(emptyList(), emptyList()).isEmpty())
        assertEquals("изредка", GnssSharedZoneText.level(1))
        assertEquals("нередко", GnssSharedZoneText.level(2))
        assertEquals("часто", GnssSharedZoneText.level(3))
    }
}
