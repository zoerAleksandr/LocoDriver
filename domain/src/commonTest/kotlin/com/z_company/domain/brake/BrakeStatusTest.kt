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

    @Test fun composite50_boundary32() {
        // п. 31 Правил: К-50 - не менее 32 тс на 100 тс веса, скорость не снижается (до 80)
        assertEquals(StatusLevel.WARNING, BrakeStatusResolver.resolve(calc(32.0, CompositeShare.P50), true).level)
        assertEquals(StatusLevel.DANGER, BrakeStatusResolver.resolve(calc(31.9, CompositeShare.P50), true).level)
        // К-75: 31, К-100: 30
        assertEquals(StatusLevel.WARNING, BrakeStatusResolver.resolve(calc(31.0, CompositeShare.P75), true).level)
        assertEquals(StatusLevel.DANGER, BrakeStatusResolver.resolve(calc(30.9, CompositeShare.P75), true).level)
    }

    @Test fun headAndTailJoinedTrain_minSpeeds() {
        // табл. III.2 Правил, «28 | 65 | 50 | (50)»
        val c = BrakeNorms.category("HEAD_TAIL_12")!!
        assertEquals(listOf<Int?>(65, 50, 50), c.minSpeeds)
        assertEquals(28, c.minNorm)
    }

    @Test fun blankAxlePressures_haveFormRows() {
        // форма ВУ-45 (808-2017 прил. К): в том числе 7,5 и 9,5
        assertTrue(7.5 in BrakeNorms.blankAxlePressures && 9.5 in BrakeNorms.blankAxlePressures)
        assertEquals(27, BrakeNorms.blankAxlePressures.size)
    }
}
