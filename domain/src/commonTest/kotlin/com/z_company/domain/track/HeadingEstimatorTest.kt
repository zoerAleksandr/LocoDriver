package com.z_company.domain.track

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class HeadingEstimatorTest {
    @Test fun providerBearingIsUsedFirst() {
        val h = HeadingEstimator()
        assertEquals(90.0, h.update(55.0, 37.0, 40.0, 90f)!!, 1e-6)
    }

    @Test fun bearingFromTwoPointsNeedsFiveMeters() {
        val h = HeadingEstimator()
        assertNull(h.update(55.0, 37.0, 40.0, null)) // опорная точка
        assertNull(h.update(55.00001, 37.0, 40.0, null)) // около 1 м — мало
        val v = h.update(55.00001, 37.0005, 40.0, null) // на восток около 30 м
        assertEquals(90.0, v!!, 3.0)
    }

    @Test fun slowSpeedKeepsLastHeadingAndIgnoresDrift() {
        val h = HeadingEstimator()
        h.update(55.0, 37.0, 40.0, 120f)
        assertEquals(120.0, h.update(55.0, 37.0, 1.0, 300f)!!, 1e-6)
        assertEquals(120.0, h.update(55.0001, 37.0001, 0.0, null)!!, 1e-6)
    }

    @Test fun turnIsSmoothedAlongShortestArc() {
        val h = HeadingEstimator()
        h.update(0.0, 0.0, 40.0, 350f)
        val v = h.update(0.0, 0.0, 40.0, 10f)!!
        assertEquals(0.0, if (v > 180) v - 360 else v, 1e-6) // половина дуги 350 -> 10 через 0
        assertEquals(350.0, HeadingEstimator.smooth(350.0, 350.0, 0.5), 1e-9)
        assertEquals(5.0, HeadingEstimator.smooth(350.0, 20.0, 0.5), 1e-9)
        assertEquals(355.0, HeadingEstimator.smooth(10.0, 340.0, 0.5), 1e-9)
    }

    @Test fun bearingBetweenCardinals() {
        assertEquals(0.0, HeadingEstimator.bearingBetween(0.0, 0.0, 1.0, 0.0), 1e-6)
        assertEquals(90.0, HeadingEstimator.bearingBetween(0.0, 0.0, 0.0, 1.0), 1e-6)
        assertEquals(180.0, HeadingEstimator.bearingBetween(1.0, 0.0, 0.0, 0.0), 1e-6)
        assertEquals(270.0, HeadingEstimator.bearingBetween(0.0, 1.0, 0.0, 0.0), 1e-6)
        assertNotNull(HeadingEstimator().update(0.0, 0.0, null, 45f)) // скорость неизвестна - не «стоим»
    }
}
