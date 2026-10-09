package com.z_company.domain.track

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AltitudeMathTest {
    @Test
    fun sanitizeKeepsPlausibleAndDropsGarbage() {
        assertEquals(120.5, AltitudeMath.sanitize(120.5))
        assertEquals(-20.0, AltitudeMath.sanitize(-20.0))
        assertNull(AltitudeMath.sanitize(null))
        assertNull(AltitudeMath.sanitize(Double.NaN))
        assertNull(AltitudeMath.sanitize(Double.POSITIVE_INFINITY))
        assertNull(AltitudeMath.sanitize(-1000.0))
        assertNull(AltitudeMath.sanitize(20000.0))
    }

    @Test
    fun pressurePlausibility() {
        assertTrue(AltitudeMath.isPlausiblePressureHpa(1013.25f))
        assertFalse(AltitudeMath.isPlausiblePressureHpa(null))
        assertFalse(AltitudeMath.isPlausiblePressureHpa(0f))
        assertFalse(AltitudeMath.isPlausiblePressureHpa(Float.NaN))
        assertFalse(AltitudeMath.isPlausiblePressureHpa(5000f))
    }

    @Test
    fun pointCarriesBothAltitudes() {
        val p = TrackPoint(1L, 55.0, 37.0, 10.0, 5f, altitudeM = 130.0, baroAltitudeM = 128.5)
        assertEquals(130.0, p.altitudeM)
        assertEquals(128.5, p.baroAltitudeM)
        assertNull(TrackPoint(1L, 55.0, 37.0, 10.0, 5f).baroAltitudeM)
    }
}
