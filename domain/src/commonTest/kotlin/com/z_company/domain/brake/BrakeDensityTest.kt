package com.z_company.domain.brake

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BrakeDensityTest {
    @Test
    fun table91_values() {
        // ВЛ80, 201-250 осей -> 46 с; ВЛ80, до 100 осей -> 98; 3М62У, 451-480 -> 45
        assertEquals(46.0, BrakeDensity.minTimeSeconds(DensityLocoGroup.G4, 230, null))
        assertEquals(98.0, BrakeDensity.minTimeSeconds(DensityLocoGroup.G4, 100, null))
        assertEquals(45.0, BrakeDensity.minTimeSeconds(DensityLocoGroup.G7, 480, null))
        assertNull(BrakeDensity.minTimeSeconds(DensityLocoGroup.G4, 481, null))
    }

    @Test
    fun table91_allRowsHaveNineColumns() {
        for (g in DensityLocoGroup.values()) assertEquals(9, g.minTimes.size, g.name)
    }

    @Test
    fun chargePressureReducesNorm() {
        assertEquals(46.0 * 0.8, BrakeDensity.minTimeSeconds(DensityLocoGroup.G4, 230, 5.7))
        assertEquals(46.0 * 0.9, BrakeDensity.minTimeSeconds(DensityLocoGroup.G4, 230, 5.4))
        assertEquals(46.0, BrakeDensity.minTimeSeconds(DensityLocoGroup.G4, 230, 5.0))
    }

    @Test
    fun smeMultipliesByReservoirVolume() {
        assertEquals(92.0, BrakeDensity.minTimeSeconds(DensityLocoGroup.G4, 230, null, locoCount = 2))
    }

    @Test
    fun groupBySeries() {
        assertEquals(DensityLocoGroup.G4, BrakeDensity.groupBySeries("ВЛ80С"))
        assertEquals(DensityLocoGroup.G4, BrakeDensity.groupBySeries("vl80t".replace("vl", "ВЛ").replace('t', 'Т')))
        assertEquals(DensityLocoGroup.G3, BrakeDensity.groupBySeries("ВЛ11"))
        assertEquals(DensityLocoGroup.G5, BrakeDensity.groupBySeries("2ТЭ10"))
        assertEquals(DensityLocoGroup.G6, BrakeDensity.groupBySeries("2ТЭ10У"))
        assertNull(BrakeDensity.groupBySeries("2ЭС5К"))
        assertNull(BrakeDensity.groupBySeries(null))
    }

    @Test
    fun assessments() {
        assertEquals(BrakeDensity.Verdict.OK, BrakeDensity.assessFreightII(46.0, 46.0).verdict)
        assertEquals(BrakeDensity.Verdict.FAIL, BrakeDensity.assessFreightII(45.9, 46.0).verdict)
        assertEquals(BrakeDensity.Verdict.NO_NORM, BrakeDensity.assessFreightII(50.0, null).verdict)
        // IV: не менее 90 % от II
        assertEquals(BrakeDensity.Verdict.OK, BrakeDensity.assessFreightIV(41.4, 46.0).verdict)
        assertEquals(BrakeDensity.Verdict.FAIL, BrakeDensity.assessFreightIV(41.0, 46.0).verdict)
        // в пути: +-20 %
        assertEquals(BrakeDensity.Verdict.OK, BrakeDensity.assessRoute(55.2, 46.0).verdict)
        assertEquals(BrakeDensity.Verdict.FAIL, BrakeDensity.assessRoute(55.3, 46.0).verdict)
        assertEquals(BrakeDensity.Verdict.FAIL, BrakeDensity.assessRoute(36.7, 46.0).verdict)
    }

    @Test
    fun passengerDensity() {
        assertEquals(BrakeDensity.Verdict.OK, BrakeDensity.assessPassenger(60.0, 0.2).verdict)
        assertEquals(BrakeDensity.Verdict.FAIL, BrakeDensity.assessPassenger(60.0, 0.25).verdict)
        assertEquals(BrakeDensity.Verdict.OK, BrakeDensity.assessPassenger(150.0, 0.5).verdict)
        assertEquals(BrakeDensity.Verdict.FAIL, BrakeDensity.assessPassenger(150.0, 0.55).verdict)
        assertEquals(BrakeDensity.Verdict.NO_NORM, BrakeDensity.assessPassenger(40.0, 0.1).verdict)
    }

    @Test
    fun tailPressure() {
        assertEquals(4.5, BrakeDensity.minTailPressure(5.0, 200))
        assertEquals(4.3, BrakeDensity.minTailPressure(5.0, 320))
        assertEquals(5.0, BrakeDensity.minTailPressure(5.4, 200))
        assertEquals(4.8, BrakeDensity.minTailPressure(5.4, 320))
        assertEquals(5.0, BrakeDensity.minTailPressure(5.7, 400))
        assertNull(BrakeDensity.minTailPressure(4.0, 100))
    }

    @Test
    fun locoCatalog() {
        val vl80 = assertNotNull(BrakeLocoCatalog.find("вл80с"))
        assertEquals(192.0, vl80.weight)
        assertEquals(8, vl80.axles)
        assertEquals(14.0, vl80.pressurePerAxle)
        assertNull(BrakeLocoCatalog.find("2ЭС5К"))
        assertTrue(BrakeLocoCatalog.items.all { it.weight > 0 && it.axles > 0 && it.pressurePerAxle > 0 })
    }
}
