package com.z_company.domain.util

import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.entities.setting.ServicePhasePoint
import com.z_company.domain.entities.setting.ServicePhaseStation
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

    @Test fun nextAheadWithoutFixIsNull() {
        assertNull(TrainMapRoute.nextAheadIndex(TrainMapRoute.stops(phase()), null))
    }

    @Test fun nextAheadByPosition() {
        val s = TrainMapRoute.stops(phase())
        assertEquals(0, TrainMapRoute.nextAheadIndex(s, GeoFix(0.0, -0.5)))
        assertEquals(1, TrainMapRoute.nextAheadIndex(s, GeoFix(0.01, 0.5)))
        assertEquals(2, TrainMapRoute.nextAheadIndex(s, GeoFix(0.01, 1.5)))
        assertEquals(2, TrainMapRoute.nextAheadIndex(s, GeoFix(0.0, 2.5)))
    }

    @Test fun cameraPointsIncludeFixAndPreferLine() {
        val s = TrainMapRoute.stops(phase())
        val line = listOf(0.0 to 0.0, 0.1 to 1.0, 0.0 to 2.0, 0.2 to 2.0)
        assertEquals(5, TrainMapRoute.cameraPoints(s, line, GeoFix(1.0, 1.0)).size)
        assertEquals(3, TrainMapRoute.cameraPoints(s, null, null).size)
    }
}
