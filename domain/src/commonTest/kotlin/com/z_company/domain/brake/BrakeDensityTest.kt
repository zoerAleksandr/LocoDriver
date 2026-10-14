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
    fun groupNotSelected_assessmentUnavailable() {
        // Группа не выбрана: даже «хорошее» время не оценивается как норма, текст просит выбрать группу
        val a = BrakeDensity.assessFreightII(120.0, null, groupSelected = false)
        assertEquals(BrakeDensity.Verdict.NO_NORM, a.verdict)
        assertTrue(a.text.contains("группу локомотива"))
        // и при переданной норме флаг «не выбрана» приоритетнее
        assertEquals(BrakeDensity.Verdict.NO_NORM, BrakeDensity.assessFreightII(120.0, 69.0, groupSelected = false).verdict)
        // длина вне таблицы - тоже недоступна, но с другим текстом
        val b = BrakeDensity.assessFreightII(120.0, null, groupSelected = true)
        assertEquals(BrakeDensity.Verdict.NO_NORM, b.verdict)
        assertTrue(!b.text.contains("группу"))
        // нет серии - группа по серии не подставляется
        assertNull(BrakeDensity.groupBySeries(""))
        assertNull(BrakeDensity.groupBySeries("  "))
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

class BrakePrefillTest {
    @Test
    fun sanitizedDropsEmptyAndZero() {
        val p = BrakePrefill(weight = "0", axles = " 0 ", trainNumber = " ", locoSeries = "ВЛ80С", locoNumber = " 123 ").sanitized()
        assertNull(p.weight)
        assertNull(p.axles)
        assertNull(p.trainNumber)
        assertEquals("ВЛ80С", p.locoSeries)
        assertEquals("123", p.locoNumber)
    }

    @Test
    fun sanitizedKeepsRealTrainData() {
        val p = BrakePrefill(weight = "3040", axles = "128", trainNumber = "2201", locoSeries = "ВЛ80", locoNumber = "5").sanitized()
        assertEquals("3040", p.weight)
        assertEquals("128", p.axles)
        assertEquals("2201", p.trainNumber)
        assertEquals("3040,5", BrakePrefill(weight = "3040,5").sanitized().weight)
    }
}
