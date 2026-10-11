package com.z_company.domain.brake

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BrakeCertificateCalculatorTest {
    private fun cat(id: String) = BrakeNorms.category(id)!!
    private fun near(a: Double, b: Double) = assertTrue(abs(a - b) < 1e-6, "ожидалось $b, получено $a")

    private fun freight(
        weight: Double,
        axles: Int,
        pressure: Double,
        id: String = "FREIGHT_LOADED",
        grade: GradeClass = GradeClass.UP_TO_006,
        share: CompositeShare = CompositeShare.NONE,
        autoblock: Boolean = false,
    ) = BrakeCertificateCalculator.calculate(
        BrakeCertificateInput(
            category = cat(id), compositionWeight = weight, rows = listOf(AxleRow(pressure, axles)),
            grade = grade, compositeShare = share, autoblock3 = autoblock,
        ),
    )

    private fun speed(r: BrakeCertificateResult) = (r.speed as SpeedLimit.Allowed).kmh

    @Test
    fun requiredPressure_exampleFromInstruction() {
        // Пример из учебного материала: Q = 3040 тс, 33 тс на 100 тс -> 1003,2 тс
        val r = freight(3040.0, 128, 8.5)
        near(r.requiredPressure, 1003.2)
        near(r.actualPressure, 1088.0)
        assertTrue(r.enough)
        assertEquals(80, speed(r))
    }

    @Test
    fun requiredPressure_passengerNorms() {
        near(freight(1000.0, 1, 1.0, id = "PASS_120").requiredPressure, 600.0)
        near(freight(1000.0, 1, 1.0, id = "EMPTY_350").requiredPressure, 550.0)
        near(freight(1000.0, 1, 1.0, id = "FREIGHT_PASS").requiredPressure, 440.0)
    }

    @Test
    fun autoblockBracketSpeed() {
        assertEquals(90, speed(freight(3040.0, 128, 8.5, autoblock = true)))
        // на спуске круче 0,010 скобки не действуют, действует знаменатель
        assertEquals(70, speed(freight(3040.0, 128, 8.5, grade = GradeClass.OVER_010_TO_015, autoblock = true)))
    }

    @Test
    fun shortage_reducesSpeed_p44() {
        // 128 осей x 7,0 = 896 -> 29,47 тс на 100 тс; недостаёт 3,53 -> 4 т -> 80 - 8 = 72 -> 70
        val r = freight(3040.0, 128, 7.0)
        assertTrue(!r.enough)
        near(r.actualPer100, 896.0 * 100 / 3040.0)
        val s = r.speed as SpeedLimit.Allowed
        assertEquals(70, s.kmh)
        assertEquals(80, s.reducedFrom)
    }

    @Test
    fun belowMinimum_notDetermined() {
        val r = freight(3040.0, 100, 7.0) // 700 -> 23 тс/100 < 28
        assertTrue(r.speed is SpeedLimit.NotDetermined)
    }

    @Test
    fun special_p33_compositeOverloadedAxles() {
        // 31,5 тс на 100 тс: нормально ниже 33, при 75 % композита и нагрузке >21 тс - до 80
        val w = 1000.0
        val r75 = freight(w, 1, 315.0, share = CompositeShare.P75)
        assertEquals(80, speed(r75))
        // 30,5 тс: 75 % не хватает (нужно >= 31), падает на п. 4.4: 33-30,5=2,5 -> 3 т -> 80-6=74 -> 70
        assertEquals(70, speed(freight(w, 1, 305.0, share = CompositeShare.P75)))
        // 100 % композита допускает 30
        assertEquals(80, speed(freight(w, 1, 305.0, share = CompositeShare.P100)))
    }

    @Test
    fun table2_consistency_minNormGivesTable2Speeds() {
        // На допускаемом минимуме п. 4.4 должен давать скорости табл. 2 (проверка переноса таблиц).
        // Известное расхождение в самих Нормах: рефрижераторный (REEFER), спуск круче 0,010: формула 80, табл. 2 - 90.
        for (c in BrakeNorms.categories) {
            val min = c.minNorm ?: continue
            for (g in GradeClass.values()) {
                val expected = c.minSpeeds[g.ordinal] ?: continue
                val r = BrakeCertificateCalculator.calculate(
                    BrakeCertificateInput(
                        category = c, compositionWeight = 1000.0,
                        rows = listOf(AxleRow(min * 10.0, 1)), grade = g,
                    ),
                )
                val s = r.speed
                if (c.id == "REEFER" && g == GradeClass.OVER_010_TO_015) {
                    assertEquals(80, (s as SpeedLimit.Allowed).kmh)
                    continue
                }
                assertTrue(s is SpeedLimit.Allowed, "${c.id}/$g: $s")
                assertEquals(expected, s.kmh, "${c.id} / $g")
            }
        }
    }

    @Test
    fun passengerWithLoco() {
        // Состав 800 тс + локомотив ЭП1/ВЛ80-подобный 184 тс (8 осей x 14,0)
        val r = BrakeCertificateCalculator.calculate(
            BrakeCertificateInput(
                category = cat("PASS_120"), compositionWeight = 800.0,
                rows = listOf(AxleRow(10.0, 64)),
                loco = LocoBrakeInput(weight = 184.0, axles = 8, pressurePerAxle = 14.0),
            ),
        )
        near(r.totalWeight, 984.0)
        near(r.requiredPressure, 590.4)
        near(r.actualPressure, 640.0 + 112.0)
        assertTrue(r.enough)
        assertEquals(120, speed(r))
        assertEquals(72, r.totalAxles)
        assertEquals(null, r.requiredManualAxles) // у пассажирских ручные тормоза по вагонам, не нормируются
    }

    @Test
    fun locoNotCountedForFreightWarning() {
        val r = BrakeCertificateCalculator.calculate(
            BrakeCertificateInput(
                category = cat("FREIGHT_LOADED"), compositionWeight = 3040.0,
                rows = listOf(AxleRow(8.5, 128)),
                loco = LocoBrakeInput(184.0, 8, 14.0),
            ),
        )
        assertTrue(r.warnings.isNotEmpty())
    }

    @Test
    fun manualBrakes() {
        fun req(norm: BrakeNorms.ManualBrakeNorm, actual: Int? = null) =
            BrakeCertificateCalculator.calculate(
                BrakeCertificateInput(
                    category = cat("FREIGHT_LOADED"), compositionWeight = 3040.0,
                    rows = listOf(AxleRow(8.5, 128)), manualNorm = norm, actualManualAxles = actual,
                ),
            )
        assertEquals(19, req(BrakeNorms.ManualBrakeNorm.UNIFIED_06).requiredManualAxles) // 18,24 -> 19
        assertEquals(25, req(BrakeNorms.ManualBrakeNorm.SLOPE_010).requiredManualAxles) // 24,32 -> 25
        assertEquals(31, req(BrakeNorms.ManualBrakeNorm.SLOPE_012).requiredManualAxles) // 30,4 -> 31
        assertEquals(true, req(BrakeNorms.ManualBrakeNorm.UNIFIED_06, 19).manualAxlesOk)
        assertEquals(false, req(BrakeNorms.ManualBrakeNorm.UNIFIED_06, 18).manualAxlesOk)
        // ровно целое не округляется вверх: 1000 тс x 0,6 = 6
        val exact = BrakeCertificateCalculator.calculate(
            BrakeCertificateInput(cat("FREIGHT_LOADED"), 1000.0, listOf(AxleRow(8.5, 40)))
        )
        assertEquals(6, exact.requiredManualAxles)
    }

    @Test
    fun steepWithoutNorm() {
        val r = freight(1000.0, 1, 1000.0, id = "PASS_130", grade = GradeClass.OVER_010_TO_015)
        assertNotNull(r)
        assertTrue(r.speed is SpeedLimit.NotDetermined)
    }
}
