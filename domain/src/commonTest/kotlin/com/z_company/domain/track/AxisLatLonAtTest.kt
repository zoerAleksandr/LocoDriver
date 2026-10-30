package com.z_company.domain.track

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AxisLatLonAtTest {
    private val axis = SectionAxis("s", "h", listOf(AxisPoint(0.0, 10.0, 30.0), AxisPoint(2.0, 10.0, 32.0), AxisPoint(6.0, 12.0, 32.0)))

    @Test fun interpolatesAndClamps() {
        val mid = axis.latLonAt(1.0)
        assertNotNull(mid)
        assertEquals(10.0, mid.first, 1e-9)
        assertEquals(31.0, mid.second, 1e-9)
        assertEquals(11.0, axis.latLonAt(4.0)!!.first, 1e-9)
        assertEquals(30.0, axis.latLonAt(-5.0)!!.second, 1e-9)
        assertEquals(12.0, axis.latLonAt(99.0)!!.first, 1e-9)
    }
}
