package com.z_company.domain.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocationSensitivityTest {
    @Test
    fun defaultLevelKeepsPreviousFilterConfig() {
        val c = LocationSensitivity.driftConfig(LocationSensitivity.DEFAULT)
        val old = DriftFilter.Config()
        assertEquals(old.radiusM, c.radiusM)
        assertEquals(old.stillSpeedKmh, c.stillSpeedKmh)
        assertEquals(old.confirmSpeedKmh, c.confirmSpeedKmh)
        assertEquals(old.continueSpeedKmh, c.continueSpeedKmh)
    }

    @Test
    fun higherSensitivityMeansLessSmoothing() {
        for (l in LocationSensitivity.MIN until LocationSensitivity.MAX) {
            assertTrue(LocationSensitivity.driftRadiusM(l + 1) < LocationSensitivity.driftRadiusM(l))
            assertTrue(LocationSensitivity.stillSpeedKmh(l + 1) < LocationSensitivity.stillSpeedKmh(l))
        }
    }

    @Test
    fun outOfRangeIsClamped() {
        assertEquals(LocationSensitivity.MIN, LocationSensitivity.clamp(-4))
        assertEquals(LocationSensitivity.MAX, LocationSensitivity.clamp(99))
    }
}
