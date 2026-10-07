package com.z_company.domain.util

import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.entities.setting.ServicePhasePoint
import com.z_company.domain.entities.setting.ServicePhaseStation
import com.z_company.domain.track.TrackAnalysis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TrainMapRouteTest {
    private fun phase(dep: Boolean = true, arr: Boolean = true, midCoords: Boolean = true) = ServicePhase(
        id = "p", departureStation = "A", arrivalStation = "C", distance = 100,
        intermediateStations = listOf(
            ServicePhaseStation(name = "B", latitude = if (midCoords) 0.0 else null, longitude = if (midCoords) 1.0 else null),
        ),
        departurePoint = if (dep) ServicePhasePoint(0.0, 0.0) else null,
        arrivalPoint = if (arr) ServicePhasePoint(0.0, 2.0) else null,
    )

    @Test fun stopsKeepOrderAndSkipMissing() {
        assertEquals(listOf("A", "B", "C"), TrainMapRoute.stops(phase()).map { it.name })
        assertEquals(listOf("B", "C"), TrainMapRoute.stops(phase(dep = false)).map { it.name })
        assertEquals(listOf("A", "C"), TrainMapRoute.stops(phase(midCoords = false)).map { it.name })
    }

    @Test fun routeNeedsTwoStops() {
        assertTrue(TrainMapRoute.hasRoute(TrainMapRoute.stops(phase())))
        assertFalse(TrainMapRoute.hasRoute(TrainMapRoute.stops(phase(dep = false, midCoords = false))))
    }

    // Рельсовая линия с обходом: A(0,0) → B(0,1) → петля севернее → C(0,2)
    private val loop = listOf(0.0 to 0.0, 0.0 to 1.0, 0.1 to 1.0, 0.1 to 2.0, 0.0 to 2.0)

    @Test fun nextAheadNeedsFixAndLine() {
        val s = TrainMapRoute.stops(phase())
        assertNull(TrainMapRoute.nextAhead(s, loop, null))
        assertNull(TrainMapRoute.nextAhead(s, null, GeoFix(0.0, 0.5)))
        assertNull(TrainMapRoute.nextAhead(s, listOf(0.0 to 0.0), GeoFix(0.0, 0.5)))
    }

    @Test fun nextAheadAlongLineNotStraightSegments() {
        val s = TrainMapRoute.stops(phase())
        assertEquals(0, TrainMapRoute.nextAhead(s, loop, GeoFix(0.0, -0.5))?.index)
        assertEquals(1, TrainMapRoute.nextAhead(s, loop, GeoFix(0.0, 0.5))?.index)
        // На петле (по прямой B–C это «середина»), по линии — уже после B, до C
        assertEquals(2, TrainMapRoute.nextAhead(s, loop, GeoFix(0.1, 1.5))?.index)
        assertEquals(2, TrainMapRoute.nextAhead(s, loop, GeoFix(0.0, 2.0))?.index)
    }

    @Test fun distanceIsAlongLine() {
        val s = TrainMapRoute.stops(phase())
        val ahead = TrainMapRoute.nextAhead(s, loop, GeoFix(0.0, 0.5))!!
        val direct = TrackAnalysis.distanceM(0.0, 0.5, 0.0, 1.0)
        assertTrue(kotlin.math.abs(ahead.distanceM - direct) < 50.0)
        // от точки на петле до C — по линии (≈ 0,5° + 0,1° + 0,1° ... > прямой по долготе)
        val onLoop = TrainMapRoute.nextAhead(s, loop, GeoFix(0.1, 1.5))!!
        assertTrue(onLoop.distanceM > TrackAnalysis.distanceM(0.1, 1.5, 0.0, 2.0))
    }

    @Test fun snapToLineOnlyWithinTolerance() {
        val near = com.z_company.domain.track.TrackPoint(1_000, 0.0001, 0.5, 40.0, 5f) // ≈11 м от линии
        val far = com.z_company.domain.track.TrackPoint(2_000, 0.01, 0.5, 40.0, 5f) // ≈1,1 км
        val out = TrainMapRoute.snapToLine(listOf(near, far), loop)
        assertEquals(0.0, out[0].latitude, 1e-9)
        assertEquals(0.5, out[0].longitude, 1e-6)
        assertEquals(1_000L, out[0].timeMs)
        assertEquals(40.0, out[0].speedKmh)
        assertEquals(far, out[1])
        assertEquals(listOf(near, far), TrainMapRoute.snapToLine(listOf(near, far), null))
    }

    @Test fun cameraPointsIncludeFixAndPreferLine() {
        val s = TrainMapRoute.stops(phase())
        val line = listOf(0.0 to 0.0, 0.1 to 1.0, 0.0 to 2.0, 0.2 to 2.0)
        assertEquals(5, TrainMapRoute.cameraPoints(s, line, GeoFix(1.0, 1.0)).size)
        assertEquals(3, TrainMapRoute.cameraPoints(s, null, null).size)
    }
}
