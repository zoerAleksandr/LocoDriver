package com.z_company.domain.gnss

import com.z_company.domain.track.TrackAnalysis
import com.z_company.domain.track.TrackPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GnssDeadZonesTest {
    private val lat0 = 59.45
    private val lon0 = 32.0

    // 60 км/ч на север: 5 с = 83,3 м = 0,00075°.
    private fun p(tSec: Long, v: Double? = 60.0, acc: Float? = 5f, lat: Double = lat0 + tSec * 0.00015, lon: Double = lon0) =
        TrackPoint(timeMs = 1_000_000L + tSec * 1000, latitude = lat, longitude = lon, speedKmh = v, accuracyM = acc)

    private fun moving(fromSec: Long, toSec: Long) = (fromSec..toSec step 5).map { p(it) }

    // Плавное торможение с 60 км/ч (по 20 км/ч за 5 с) после moving(0, 120): точки 125, 130 - потом стоянка с 135 с.
    private val standLat = lat0 + 130 * 0.00015
    private fun brake() = listOf(p(125, 40.0), p(130, 20.0, lat = standLat))

    @Test
    fun normalTrackHasNoSpans() {
        assertTrue(GnssDeadZoneDetector.detect(moving(0, 600)).isEmpty())
    }

    @Test
    fun normalTrackWithStopHasNoSpans() {
        val pts = moving(0, 120) + brake() +
            (135L..795L step 60).map { p(it, v = 0.0, lat = standLat) } +
            // Разгон плавный: 10, 20, 30, 40, 60 км/ч с шагом 5 с.
            listOf(10.0, 20.0, 30.0, 40.0, 60.0).mapIndexed { i, v -> p(800L + i * 5, v = v, lat = standLat) }
        assertTrue(GnssDeadZoneDetector.detect(pts).isEmpty())
    }

    @Test
    fun twoMinuteGapWhileStandingIsGap() {
        val pts = moving(0, 120) + brake() +
            listOf(p(135, 0.0, lat = standLat), p(195, 0.0, lat = standLat), p(255, 0.0, lat = standLat)) +
            listOf(p(375, 0.0, lat = standLat), p(435, 0.0, lat = standLat))
        val spans = GnssDeadZoneDetector.detect(pts)
        assertEquals(1, spans.size)
        val s = spans.single()
        assertEquals(GnssBadKind.GAP, s.kind)
        assertEquals(120_000L, s.durationMs)
        assertEquals(2, s.anchors.size)
    }

    @Test
    fun standingSpacingOfSixtyToNinetySecondsIsNotGap() {
        val pts = listOf(p(0, 0.0, lat = lat0), p(60, 0.0, lat = lat0), p(150, 0.0, lat = lat0))
        assertTrue(GnssDeadZoneDetector.detect(pts).isEmpty())
    }

    @Test
    fun gapWhileMovingOverThirtySeconds() {
        val gap = moving(0, 100) + moving(145, 200)
        assertEquals(listOf(GnssBadKind.GAP), GnssDeadZoneDetector.detect(gap).map { it.kind })
        val shortGap = moving(0, 100) + moving(125, 200)
        assertTrue(GnssDeadZoneDetector.detect(shortGap).isEmpty())
    }

    @Test
    fun veryLongGapIsSessionBreakNotZone() {
        val pts = moving(0, 100) + moving(100 + 21 * 60, 100 + 21 * 60 + 60)
        assertTrue(GnssDeadZoneDetector.detect(pts).isEmpty())
    }

    @Test
    fun poorAccuracyRunIsFlaggedSingleBadFixIsNot() {
        val base = moving(0, 100).map { it }.toMutableList()
        val run = base.map { if (it.timeMs in 1_040_000L..1_060_000L) it.copy(accuracyM = 120f) else it }
        val spans = GnssDeadZoneDetector.detect(run)
        assertEquals(listOf(GnssBadKind.POOR_ACCURACY), spans.map { it.kind })
        assertTrue(spans.single().durationMs >= GnssDeadZoneConfig.POOR_MIN_DURATION_MS)

        val single = base.map { if (it.timeMs == 1_050_000L) it.copy(accuracyM = 120f) else it }
        assertTrue(GnssDeadZoneDetector.detect(single).isEmpty())
    }

    @Test
    fun poorAccuracyAtTrackStartIsColdStartNotZone() {
        val pts = moving(0, 100).map { if (it.timeMs <= 1_030_000L) it.copy(accuracyM = 200f) else it }
        assertTrue(GnssDeadZoneDetector.detect(pts).isEmpty())
    }

    @Test
    fun teleportIsSpeedJumpAnchoredOnTrustedPoint() {
        val pts = moving(0, 100).map { if (it.timeMs == 1_050_000L) it.copy(latitude = lat0 + 0.05) else it }
        val spans = GnssDeadZoneDetector.detect(pts)
        assertEquals(1, spans.size)
        assertEquals(GnssBadKind.SPEED_JUMP, spans.single().kind)
        // Ложные координаты скачка в опорные точки не попадают.
        assertTrue(spans.single().anchors.all { it.latitude < lat0 + 0.02 })
    }

    @Test
    fun impossibleReportedSpeedAndAccelerationAreJumps() {
        val fast = moving(0, 100).map { if (it.timeMs == 1_050_000L) it.copy(speedKmh = 400.0) else it }
        assertEquals(listOf(GnssBadKind.SPEED_JUMP), GnssDeadZoneDetector.detect(fast).map { it.kind })
        val accel = moving(0, 100).map {
            when {
                it.timeMs < 1_050_000L -> it.copy(speedKmh = 40.0)
                it.timeMs == 1_050_000L -> it.copy(speedKmh = 140.0)
                else -> it.copy(speedKmh = 40.0)
            }
        }
        assertEquals(listOf(GnssBadKind.SPEED_JUMP), GnssDeadZoneDetector.detect(accel).map { it.kind })
    }

    @Test
    fun outOfOrderTimestampsAreIgnoredAndShortInputIsSafe() {
        assertTrue(GnssDeadZoneDetector.detect(emptyList()).isEmpty())
        assertTrue(GnssDeadZoneDetector.detect(listOf(p(0))).isEmpty())
        val pts = moving(0, 100) + listOf(p(10)) + moving(105, 200)
        assertTrue(GnssDeadZoneDetector.detect(pts).isEmpty())
    }

    @Test
    fun gridCellContainsItsCenterAndNearbyPoints() {
        val c = GnssGrid.cellOf(59.4512, 32.0123)
        val center = GnssGrid.center(c)
        assertEquals(c, GnssGrid.cellOf(center.latitude, center.longitude))
        val d = TrackAnalysis.distanceM(59.4512, 32.0123, center.latitude, center.longitude)
        assertTrue(d < 250.0, "центр ячейки не дальше размера ячейки: $d")
        // Соседняя по широте точка через ~300 м - другая ячейка.
        assertTrue(GnssGrid.cellOf(59.4512 + 0.0027, 32.0123) != c)
    }

    @Test
    fun gridIsStableAcrossHemispheresAndAntimeridian() {
        for ((la, lo) in listOf(-33.9 to 151.2, 0.0 to 0.0, 69.0 to -179.99, 43.1 to 131.9)) {
            val c = GnssGrid.cellOf(la, lo)
            val ctr = GnssGrid.center(c)
            assertEquals(c, GnssGrid.cellOf(ctr.latitude, ctr.longitude))
        }
    }

    @Test
    fun gapChordCoversIntermediateCells() {
        val span = GnssBadSpan(GnssBadKind.GAP, 0, 100_000, listOf(GnssPos(lat0, lon0), GnssPos(lat0 + 0.009, lon0))) // ~1 км
        assertTrue(GnssDeadZones.cellsOf(span).size >= 4)
        val far = GnssBadSpan(GnssBadKind.GAP, 0, 100_000, listOf(GnssPos(lat0, lon0), GnssPos(lat0 + 0.2, lon0)))
        assertEquals(2, GnssDeadZones.cellsOf(far).size)
    }

    @Test
    fun aggregateCountsDistinctTracksOnce() {
        val gapTrack = moving(0, 120) + brake() + listOf(p(135, 0.0, lat = standLat), p(195, 0.0, lat = standLat), p(375, 0.0, lat = standLat), p(435, 0.0, lat = standLat))
        val spans = GnssDeadZoneDetector.detect(gapTrack)
        // Один трек с двумя одинаковыми срабатываниями считается один раз.
        val one = GnssDeadZones.aggregate(listOf(GnssTrackSpans("a", spans + spans)))
        assertTrue(one.isNotEmpty())
        assertTrue(one.all { it.tracks == 1 })
        val two = GnssDeadZones.aggregate(listOf(GnssTrackSpans("a", spans), GnssTrackSpans("b", spans)))
        assertTrue(two.any { it.tracks == 2 })
        assertTrue(GnssDeadZones.aggregate(listOf(GnssTrackSpans("a", spans)), minTracks = 2).isEmpty())
        assertTrue(two.all { GnssBadKind.GAP in it.kinds })
    }

    @Test
    fun texts() {
        assertEquals("Замечено в 1 поездке", GnssDeadZoneText.trips(1))
        assertEquals("Замечено в 2 поездках", GnssDeadZoneText.trips(2))
        assertEquals("Замечено в 11 поездках", GnssDeadZoneText.trips(11))
        assertEquals("до 45 с", GnssDeadZoneText.duration(45_000))
        assertEquals("до 2 мин", GnssDeadZoneText.duration(120_000))
        assertEquals("Нет данных GPS, скачки положения", GnssDeadZoneText.kinds(setOf(GnssBadKind.SPEED_JUMP, GnssBadKind.GAP)))
    }
}
