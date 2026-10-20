package com.z_company.domain.brake

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TurnaroundReferenceTest {
    private fun e(id: String, time: String, kind: String = "", d: Double? = null) =
        TurnaroundEntry(id = id, readinessTime = time, testKind = kind, densitySec = d)

    private val mod: (Long) -> Int = { ((it / 60_000) % 1440).toInt() }

    @Test fun chain_II_sp1_record_sp2_record() {
        val list = listOf(
            e("a", "08:00", d = 100.0),                                 // без вида до первого СП - от II (100)
            e("sp1", "09:30", TurnaroundEntry.KIND_SHORT, 90.0),         // СП: не оценивается, становится эталоном
            e("b", "10:00", d = 95.0),                                  // от СП 09:30 (90)
            e("sp2", "11:00", TurnaroundEntry.KIND_SHORT, 60.0),        // новый эталон
            e("c", "12:00", d = 62.0),                                  // от СП 11:00 (60)
        )
        val r = TurnaroundReference.resolve(list, 100.0, mod)
        assertEquals("от II", r.getValue("a").ref!!.label); assertEquals(100.0, r.getValue("a").ref!!.seconds)
        assertTrue(r.getValue("sp1").becomesReference); assertNull(r.getValue("sp1").ref)
        assertEquals("от СП 09:30", r.getValue("b").ref!!.label); assertEquals(90.0, r.getValue("b").ref!!.seconds)
        assertTrue(r.getValue("sp2").becomesReference)
        assertEquals("от СП 11:00", r.getValue("c").ref!!.label); assertEquals(60.0, r.getValue("c").ref!!.seconds)
        // оценка от эталона - граница ровно 20 %
        assertEquals(TurnaroundDensityCheck.Verdict.OK, TurnaroundDensityCheck.check(72.0, 60.0).verdict)
        assertEquals(TurnaroundDensityCheck.Verdict.EXCEEDED, TurnaroundDensityCheck.check(72.1, 60.0).verdict)
    }

    @Test fun recordWithoutKind_isNotReference_andNoIIMeansNoRef() {
        val list = listOf(e("x", "09:00", d = 50.0), e("y", "10:00", d = 55.0))
        val r = TurnaroundReference.resolve(list, null, mod)
        assertNull(r.getValue("x").ref); assertEquals(false, r.getValue("x").becomesReference)
        assertNull(r.getValue("y").ref) // запись без вида эталоном не стала
    }

    @Test fun orderByTime_notByListOrder_andSpWithoutDensityIgnored() {
        val list = listOf(
            e("late", "12:00", d = 88.0),
            e("sp", "09:00", TurnaroundEntry.KIND_FULL, 80.0),           // раньше по времени, хотя в списке позже
            e("emptySp", "11:00", TurnaroundEntry.KIND_SHORT, null),     // СП без плотности эталоном не становится
        )
        val r = TurnaroundReference.resolve(list, 120.0, mod)
        assertEquals("от ТП 09:00", r.getValue("late").ref!!.label)
        assertEquals("от ТП 09:00", r.getValue("emptySp").ref!!.label)
        assertEquals(false, r.getValue("emptySp").becomesReference)
    }

    @Test fun recalculatesWhenAnyRecordChanges() {
        val base = listOf(e("a", "08:00", d = 100.0), e("sp", "09:00", TurnaroundEntry.KIND_SHORT, 90.0), e("b", "10:00", d = 95.0))
        assertEquals(90.0, TurnaroundReference.resolve(base, 100.0, mod).getValue("b").ref!!.seconds)
        // изменили вид СП на пусто - эталон снова II
        val changed = base.map { if (it.id == "sp") it.copy(testKind = "") else it }
        assertEquals(100.0, TurnaroundReference.resolve(changed, 100.0, mod).getValue("b").ref!!.seconds)
        // изменили плотность СП
        val d2 = base.map { if (it.id == "sp") it.copy(densitySec = 70.0) else it }
        assertEquals(70.0, TurnaroundReference.resolve(d2, 100.0, mod).getValue("b").ref!!.seconds)
    }
}
