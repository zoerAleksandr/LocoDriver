package com.z_company.domain.brake

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BrakeStatusTest {
    private fun freight() = BrakeNorms.categories.first { it.isLoadedFreight }

    private fun calc(per100: Double, composite: CompositeShare = CompositeShare.NONE, cat: BrakeCategory = freight()): BrakeCertificateResult {
        // вес 1000 тс, 100 осей: нажатие на ось = per100 * 10 / 100
        val rows = listOf(AxleRow(per100 * 1000.0 / 100.0 / 100.0, 100))
        return BrakeCertificateCalculator.calculate(BrakeCertificateInput(cat, 1000.0, rows, compositeShare = composite))
    }

    @Test fun noData() = assertEquals(StatusLevel.NONE, BrakeStatusResolver.resolve(null, false).level)

    @Test fun enough_ok() {
        val st = BrakeStatusResolver.resolve(calc(freight().norm + 1.0), true)
        assertEquals(StatusLevel.OK, st.level)
    }

    @Test fun shortButSpeedKept_composite75_isWarningWithDeficitPer100() {
        val r = calc(31.5, CompositeShare.P75)
        assertTrue(!r.enough)
        val st = BrakeStatusResolver.resolve(r, true)
        assertEquals(StatusLevel.WARNING, st.level)
        assertTrue(st.headline.startsWith("Не хватает 1,5 тс на 100 тс веса; скорость не снижена"), st.headline)
        assertTrue(st.headline.contains("п. 3.3 Инструкции ЦТ-ЦВ-ЦЛ-ВНИИЖТ/277"))
    }

    @Test fun shortButSpeedKept_composite100_boundary30() {
        assertEquals(StatusLevel.WARNING, BrakeStatusResolver.resolve(calc(30.0, CompositeShare.P100), true).level)
        // ниже 30 при К-100 - п. 3.3 не применим
        assertEquals(StatusLevel.DANGER, BrakeStatusResolver.resolve(calc(29.9, CompositeShare.P100), true).level)
    }

    @Test fun shortWithReduction_isDanger() {
        val r = calc(freight().norm - 1.5)
        val st = BrakeStatusResolver.resolve(r, true)
        assertEquals(StatusLevel.DANGER, st.level)
        assertTrue(st.headline.contains("скорость снижена"), st.headline)
    }

    @Test fun belowMinimum_isDanger() {
        val min = freight().minNorm!!
        val st = BrakeStatusResolver.resolve(calc(min - 1.0), true)
        assertEquals(StatusLevel.DANGER, st.level)
        assertTrue(st.headline.contains("ниже минимального"))
    }
}
