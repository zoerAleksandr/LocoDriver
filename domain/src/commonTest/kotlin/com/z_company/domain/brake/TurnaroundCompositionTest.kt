package com.z_company.domain.brake

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TurnaroundCompositionTest {
    private fun base(catId: String, rows: List<AxleRow>, loco: LocoBrakeInput? = null) =
        BrakeCertificateInput(BrakeNorms.category(catId)!!, 3000.0, rows, loco = loco)

    @Test fun increaseAndDecreaseWeight_sameNorm() {
        val b = base("FREIGHT_LOADED", listOf(AxleRow(7.0, 200)))
        val up = TurnaroundComposition.compute(b, 3500.0, 220)!!
        assertEquals(33, up.normBefore); assertEquals(33, up.normAfter)
        assertEquals(1155.0, up.required, 1e-9) // 33*3500/100
        assertEquals(1540.0, up.actual!!, 1e-9) // 220*7
        val down = TurnaroundComposition.compute(b, 2000.0, 150)!!
        assertEquals(660.0, down.required, 1e-9)
        assertEquals(1050.0, down.actual!!, 1e-9)
        assertEquals(true, down.enough())
    }

    @Test fun normChangesByAxles_forEmptyTrains() {
        val b = base("EMPTY_350_400", listOf(AxleRow(3.5, 360))) // N = 44
        val fewer = TurnaroundComposition.compute(b, 2840.0, 340)!!
        assertEquals(44, fewer.normBefore); assertEquals(55, fewer.normAfter); assertTrue(fewer.normChanged)
        assertEquals(1562.0, fewer.required, 1e-9) // 55*2840/100
        val more = TurnaroundComposition.compute(b, 3600.0, 410)!!
        assertEquals(33, more.normAfter)
        assertEquals(1188.0, more.required, 1e-9)
        // груженый: норма от осей не зависит
        assertEquals(33, TurnaroundComposition.compute(base("FREIGHT_LOADED", listOf(AxleRow(7.0, 200))), 3000.0, 600)!!.normAfter)
    }

    @Test fun roundingUp() {
        val b = base("FREIGHT_LOADED", listOf(AxleRow(7.0, 200)))
        assertEquals(1002.0, TurnaroundComposition.compute(b, 3035.0, 200)!!.required, 1e-9) // 33*3035/100 = 1001,55
    }

    @Test fun manualAxlesRecalculatedByNewWeight() {
        val b = base("FREIGHT_LOADED", listOf(AxleRow(7.0, 200)))
        val r = TurnaroundComposition.compute(b, 4000.0, 250)!!
        assertEquals(18, r.manualAxlesBefore) // 0,6*3000/100
        assertEquals(24, r.requiredManualAxles) // 0,6*4000/100
    }

    @Test fun locoIsIncluded() {
        val b = base("FREIGHT_PASS", listOf(AxleRow(7.0, 100)), LocoBrakeInput(200.0, 8, 10.0))
        val r = TurnaroundComposition.compute(b, 1000.0, 80)!!
        assertEquals(528.0, r.required, 1e-9) // 44*(1000+200)/100
        assertEquals(640.0, r.actual!!, 1e-9) // 80*7 + 8*10
    }

    @Test fun noBaseOrNoWeight_null_andNoPressuresMeansNoActual() {
        assertNull(TurnaroundComposition.compute(null, 1000.0, 10))
        assertNull(TurnaroundComposition.compute(base("FREIGHT_LOADED", emptyList()), null, 10))
        val r = TurnaroundComposition.compute(base("FREIGHT_LOADED", emptyList()), 1000.0, 10)!!
        assertNull(r.actual)
        assertNull(r.enough())
    }

    @Test fun autoFillKeepsManualValues() {
        val b = base("FREIGHT_LOADED", listOf(AxleRow(7.0, 200)))
        fun c(e: TurnaroundEntry) = TurnaroundComposition.compute(b, e.newWeight.replace(',', '.').toDoubleOrNull(), e.newAxles.toIntOrNull())
        val e0 = TurnaroundEntry("a")
        val e1 = e0.copy(newWeight = "3000", newAxles = "200")
        val f1 = TurnaroundComposition.autoFill(e0, e1, c(e0), c(e1))
        assertEquals("990", f1.newRequired); assertEquals("1400", f1.newActual)
        // вес изменился - расчётные значения обновляются
        val e2 = f1.copy(newWeight = "3500", newAxles = "220")
        val f2 = TurnaroundComposition.autoFill(f1, e2, c(f1), c(e2))
        assertEquals("1155", f2.newRequired); assertEquals("1540", f2.newActual)
        // ручная правка потребного сохраняется
        val manual = f2.copy(newRequired = "1200")
        assertTrue(TurnaroundComposition.isManual(manual.newRequired, c(manual)!!.requiredText))
        val e3 = manual.copy(newWeight = "4000")
        val f3 = TurnaroundComposition.autoFill(manual, e3, c(manual), c(e3))
        assertEquals("1200", f3.newRequired)
        assertEquals("1540", f3.newActual) // оси не менялись, фактическое прежнее
        assertNotNull(f3)
    }
}
