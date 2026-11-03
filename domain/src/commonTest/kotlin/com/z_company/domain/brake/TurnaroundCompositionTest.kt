package com.z_company.domain.brake

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TurnaroundCompositionTest {
    private fun base(catId: String, rows: List<AxleRow>, loco: LocoBrakeInput? = null) =
        BrakeCertificateInput(BrakeNorms.category(catId)!!, 3000.0, rows, loco = loco)

    private fun ch(att: Boolean, p: String, n: String) = WagonChange("c" + p + n + att, att, p, n)

    // лицевая: 200 осей по 7 тс = 1400 тс; груженый N = 33
    private val loaded = base("FREIGHT_LOADED", listOf(AxleRow(7.0, 200)))

    @Test fun detached_subtractsAxlesAndPressure() {
        val r = TurnaroundComposition.compute(loaded, 2700.0, null, listOf(ch(false, "7", "20")))!!
        assertEquals(180, r.derivedAxles)
        assertEquals(1260.0, r.actual!!, 1e-9) // 1400 - 20*7
        assertEquals(891.0, r.required!!, 1e-9) // 33*2700/100
        assertTrue(r.warnings.isEmpty())
        assertEquals(true, r.enough())
    }

    @Test fun attached_addsAxlesAndPressure() {
        val r = TurnaroundComposition.compute(loaded, 3300.0, null, listOf(ch(true, "5", "10")))!!
        assertEquals(210, r.derivedAxles)
        assertEquals(1450.0, r.actual!!, 1e-9)
        assertEquals(1089.0, r.required!!, 1e-9)
    }

    @Test fun mixedAndSeveralPressures() {
        val b = base("FREIGHT_LOADED", listOf(AxleRow(7.0, 100), AxleRow(5.0, 100))) // 700 + 500 = 1200, 200 осей
        val r = TurnaroundComposition.compute(
            b, 3000.0, null,
            listOf(ch(false, "7", "20"), ch(false, "5", "10"), ch(true, "8,5", "6"), ch(true, "3,5", "4")),
        )!!
        assertEquals(200 - 30 + 10, r.derivedAxles)
        assertEquals(1200.0 - 140 - 50 + 51 + 14, r.actual!!, 1e-9)
        assertEquals(140.0 + 50.0, r.detachedTc, 1e-9)
        assertEquals(65.0, r.attachedTc, 1e-9)
    }

    @Test fun detachMoreThanAvailable_warnsButCalculates() {
        val r = TurnaroundComposition.compute(loaded, 2000.0, null, listOf(ch(false, "7", "250")))!!
        assertTrue(r.warnings.any { "только 200" in it })
        assertEquals(-50, r.derivedAxles)
        assertTrue(r.warnings.any { "больше, чем в составе" in it })
        val none = TurnaroundComposition.compute(loaded, 2000.0, null, listOf(ch(false, "9", "4")))!!
        assertTrue(none.warnings.single().startsWith("Такого нажатия"))
        assertEquals(1400.0 - 36.0, none.actual!!, 1e-9) // вычитается как введено
    }

    @Test fun emptyOrIncompleteRowsIgnored_actualStaysManual() {
        val r = TurnaroundComposition.compute(loaded, 3000.0, 210, listOf(ch(true, "", "10"), ch(false, "7", "")))!!
        assertNull(r.derivedAxles)
        assertNull(r.actual) // без строк фактическое - ручной ввод
        assertEquals(990.0, r.required!!, 1e-9)
        assertNull(TurnaroundComposition.compute(null, 1000.0, 10, emptyList()))
        assertNull(TurnaroundComposition.compute(loaded, null, null, emptyList())!!.required)
    }

    @Test fun normChangesByAxles_forEmptyTrains() {
        val b = base("EMPTY_350_400", listOf(AxleRow(3.5, 360))) // N = 44
        val fewer = TurnaroundComposition.compute(b, 2840.0, null, listOf(ch(false, "3,5", "20")))!!
        assertEquals(340, fewer.derivedAxles)
        assertEquals(44, fewer.normBefore); assertEquals(55, fewer.normAfter); assertTrue(fewer.normChanged)
        assertEquals(1562.0, fewer.required!!, 1e-9)
        assertEquals(1190.0, fewer.actual!!, 1e-9)
        val more = TurnaroundComposition.compute(b, 3600.0, null, listOf(ch(true, "3,5", "50")))!!
        assertEquals(33, more.normAfter)
        assertEquals(1188.0, more.required!!, 1e-9)
    }

    @Test fun roundingUp_locoAndManualAxles() {
        assertEquals(1002.0, TurnaroundComposition.compute(loaded, 3035.0, 200, emptyList())!!.required!!, 1e-9) // 1001,55
        val b = base("FREIGHT_PASS", listOf(AxleRow(7.0, 100)), LocoBrakeInput(200.0, 8, 10.0)) // 700 + 80
        val r = TurnaroundComposition.compute(b, 1000.0, null, listOf(ch(true, "7", "10")))!!
        assertEquals(528.0, r.required!!, 1e-9) // 44*(1000+200)/100
        assertEquals(850.0, r.actual!!, 1e-9) // 780 + 70
        val m = TurnaroundComposition.compute(loaded, 4000.0, null, emptyList())!!
        assertEquals(18, m.manualAxlesBefore); assertEquals(24, m.requiredManualAxles)
    }

    @Test fun autoFillAndManualOverride() {
        fun c(e: TurnaroundEntry) = TurnaroundComposition.compute(loaded, e.newWeight.toDoubleOrNull(), e.newAxles.toIntOrNull(), e.changes)
        val e0 = TurnaroundEntry("a", newWeight = "2700")
        val e1 = e0.copy(changes = listOf(ch(false, "7", "20")))
        val f1 = TurnaroundComposition.autoFill(e0, e1, c(e0), c(e1))
        assertEquals("180", f1.newAxles); assertEquals("1260", f1.newActual); assertEquals("891", f1.newRequired)
        // ещё строка - пересчёт
        val e2 = f1.copy(changes = f1.changes + ch(true, "5", "10"))
        val f2 = TurnaroundComposition.autoFill(f1, e2, c(f1), c(e2))
        assertEquals("190", f2.newAxles); assertEquals("1310", f2.newActual)
        // ручная правка фактического сохраняется при смене строк
        val manual = f2.copy(newActual = "1300")
        assertTrue(TurnaroundComposition.isManual(manual.newActual, c(manual)!!.actualText))
        val e3 = manual.copy(changes = manual.changes + ch(true, "5", "2"))
        val f3 = TurnaroundComposition.autoFill(manual, e3, c(manual), c(e3))
        assertEquals("1300", f3.newActual); assertEquals("192", f3.newAxles)
        // удалили все строки: расчётные значения очищаются, вручную введённые остаются
        val e4 = f2.copy(changes = emptyList())
        val f4 = TurnaroundComposition.autoFill(f2, e4, c(f2), c(e4))
        assertEquals("", f4.newAxles); assertEquals("", f4.newActual)
        // ручной ввод осей, пока строк нет: не затирается
        val e5 = e0.copy(newAxles = "2")
        assertEquals("2", TurnaroundComposition.autoFill(e0, e5, c(e0), c(e5)).newAxles)
    }

    @Test fun oldEntriesWithoutChangesReadAsBefore() {
        val j = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val e = j.decodeFromString<TurnaroundEntry>("""{"id":"x","newWeight":"3100","newActual":"1010"}""")
        assertTrue(e.changes.isEmpty())
        assertEquals("Новый вес 3100 т. · Фактическое 1010 тс", e.compositionSummary)
        val withChanges = j.decodeFromString<TurnaroundEntry>("""{"id":"x","changes":[{"id":"c1","attached":true,"pressureText":"7","axlesText":"4"}]}""")
        assertEquals(1, withChanges.changes.size)
        assertTrue(withChanges.changes[0].isValid)
    }

    @Test fun detached_usesEnteredAxlesWhenRowsEmpty() {
        // «Всего осей» введено, а нажатие не задано - строк таблицы нет: оси лицевой берутся из введённого числа
        val b = base("FREIGHT_LOADED", emptyList()).copy(enteredAxles = 200)
        val r = TurnaroundComposition.compute(b, 3000.0, null, listOf(ch(false, "7", "4")))!!
        assertEquals(196, r.derivedAxles)
        assertEquals(200, r.frontAxles)
    }
}
