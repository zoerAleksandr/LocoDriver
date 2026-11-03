package com.z_company.domain.entities.setting

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServicePhaseVerifiedTest {
    private fun phase() = ServicePhase(
        departureStation = "А", arrivalStation = "В", distance = 120,
        intermediateStations = listOf(ServicePhaseStation(name = "Б", latitude = 59.5, longitude = 30.25, kmFromStart = 60.0)),
        departurePoint = ServicePhasePoint(59.0, 30.0), arrivalPoint = ServicePhasePoint(60.123456, -30.5),
    )

    @Test fun signatureFormat() {
        assertEquals("59.00000,30.00000;59.50000,30.25000;60.12346,-30.50000", phase().coordinateSignature())
    }

    @Test fun verifiedOnlyWhenMeasuredByCurrentCoordinates() {
        val p = phase()
        assertFalse(p.isMapVerified)
        val m = p.copy(measuredSignature = p.measuredSignatureFor(120))
        assertTrue(m.isMapVerified)
        assertFalse(m.copy(distance = 121).isMapVerified)
        assertFalse(m.copy(departurePoint = ServicePhasePoint(59.1, 30.0)).isMapVerified)
    }

    @Test fun notVerifiedWithoutIntermediateStationsOrCoordinates() {
        val p = phase()
        assertFalse(p.copy(intermediateStations = emptyList(), measuredSignature = p.measuredSignatureFor(120)).isMapVerified)
        val noCoords = p.copy(intermediateStations = listOf(ServicePhaseStation(name = "Б")))
        assertFalse(noCoords.copy(measuredSignature = noCoords.measuredSignatureFor(120)).isMapVerified)
    }

    @Test fun signatureRoundsLikeJavaFormat() {
        // Кикерино: 59.465865 -> `%.5f` даёт 59.46587 (половина вверх по десятичной записи), а не 59.46586.
        val p = phase().copy(intermediateStations = listOf(ServicePhaseStation(name = "К", latitude = 59.465865, longitude = 29.627752)))
        assertEquals("59.00000,30.00000;59.46587,29.62775;60.12346,-30.50000", p.coordinateSignature())
    }
}
