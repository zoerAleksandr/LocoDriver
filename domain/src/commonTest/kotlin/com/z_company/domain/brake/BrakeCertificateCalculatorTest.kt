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
        // Пример из учебного материала: Q = 3040 тс, 33 тс на 100 тс -> 1003,2 тс; по 808-2022 (прил. М, п. 8) округляется вверх до 1004
        val r = freight(3040.0, 128, 8.5)
        near(r.requiredPressure, 1004.0)
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
        // На допускаемом минимуме п. 35 должен давать скорости табл. III.2 (проверка переноса таблиц).
        // Известное расхождение в самих Нормах: рефрижераторный (REEFER), спуск круче 0,010: формула 80, табл. 2 - 90.
        for (c in BrakeNorms.categories) {
            val min = c.minNorm ?: continue
            for (g in GradeClass.values().filter { it != GradeClass.OVER_015 }) {
                val expected = c.minSpeeds[g.ordinal] ?: continue
                val r = BrakeCertificateCalculator.calculate(
                    BrakeCertificateInput(
                        category = c, compositionWeight = 1000.0,
                        rows = listOf(AxleRow(min * 10.0, 1)), grade = g,
                    ),
                )
                val s = r.speed
                // Расхождение в самих Правилах для «локомотивы в голове и хвосте» (табл. III.2: 50 при спуске круче 0,006, формула п. 35: 65 / 55):
                // в точке минимума принимается меньшая из двух скоростей, поэтому результат равен таблице.
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
        near(r.requiredPressure, 591.0) // 590,4 -> 591
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

    private fun manualAxles(step: SlopeStep, override: BrakeNorms.ManualBrakeNorm? = null) =
        BrakeCertificateCalculator.calculate(
            BrakeCertificateInput(
                category = cat("FREIGHT_LOADED"), compositionWeight = 3040.0,
                rows = listOf(AxleRow(8.5, 128)), grade = step.grade, slope = step, manualNorm = override,
            ),
        ).requiredManualAxles

    @Test
    fun manualAuto_byEverySlopeStep_table6WithLowerBound06() {
        // Q = 3040 тс: k * 30,4 с округлением вверх
        // ≤0,006: табл. 6 = 0,4, но не менее 0,6 -> 18,24 -> 19
        assertEquals(19, manualAxles(SlopeStep.S006))
        assertEquals(0.6, BrakeNorms.manualInfo(SlopeStep.S006, null).perHundred)
        assertEquals(19, manualAxles(SlopeStep.S008))     // 0,6
        assertEquals(25, manualAxles(SlopeStep.S010))     // 0,8 -> 24,32
        assertEquals(31, manualAxles(SlopeStep.S012))     // 1,0 -> 30,4
        assertEquals(37, manualAxles(SlopeStep.S014))     // 1,2 -> 36,48
        assertEquals(43, manualAxles(SlopeStep.S016))     // 1,4 -> 42,56
        assertEquals(49, manualAxles(SlopeStep.S018))     // 1,6 -> 48,64
        assertEquals(55, manualAxles(SlopeStep.S020))     // 1,8 -> 54,72
        for (s in SlopeStep.values()) assertEquals(maxOf(0.6, s.table6), BrakeNorms.manualInfo(s, null).perHundred, s.name)
    }

    @Test
    fun manualOverride_ignoresSlope() {
        // ручное 0,4 разрешено (одна дорога): 12,16 -> 13, от уклона не зависит
        assertEquals(13, manualAxles(SlopeStep.S020, BrakeNorms.ManualBrakeNorm.SLOPE_006))
        assertEquals(13, manualAxles(SlopeStep.S006, BrakeNorms.ManualBrakeNorm.SLOPE_006))
        val info = BrakeNorms.manualInfo(SlopeStep.S020, BrakeNorms.ManualBrakeNorm.SLOPE_006)
        assertTrue(!info.auto)
        assertTrue(BrakeNorms.manualInfo(SlopeStep.S010, null).text.contains("0,010"))
    }

    @Test
    fun slopeSteps_gradeForSpeeds() {
        assertEquals(GradeClass.UP_TO_006, SlopeStep.S006.grade)
        assertEquals(GradeClass.OVER_006_TO_010, SlopeStep.S010.grade)
        assertEquals(GradeClass.OVER_010_TO_015, SlopeStep.S014.grade)
        assertEquals(GradeClass.OVER_015, SlopeStep.S016.grade)
        // круче 0,015 скорость по таблицам не определяется
        val r = BrakeCertificateCalculator.calculate(
            BrakeCertificateInput(cat("FREIGHT_LOADED"), 3040.0, listOf(AxleRow(8.5, 128)), grade = GradeClass.OVER_015, slope = SlopeStep.S016),
        )
        assertTrue(r.speed is SpeedLimit.NotDetermined)
    }

    @Test
    fun fractionalInputs() {
        // 3040,5 тс, 8,25 тс на ось, 128 осей
        val r = BrakeCertificateCalculator.calculate(
            BrakeCertificateInput(cat("FREIGHT_LOADED"), 3040.5, listOf(AxleRow(8.25, 128))),
        )
        near(r.requiredPressure, 1004.0) // 1003,365 -> ближайшее большее целое (808-2022, прил. М, п. 8)
        near(r.actualPressure, 8.25 * 128)
        assertTrue(r.enough)
    }
}

class InstructionRefTest {
    @Test
    fun annotateAddsDocumentAfterFirstRef() {
        val g = InstructionRef.GENITIVE
        assertEquals("п. 35 прил. 2 $g: снижение", InstructionRef.annotate("п. 35: снижение"))
        assertEquals("(п. 48, 50 прил. 2 $g)", InstructionRef.annotate("(п. 48, 50)"))
        assertEquals("по табл. III.4 прил. 2 $g для уклона", InstructionRef.annotate("по табл. III.4 для уклона"))
        assertEquals("п. 133 $g", InstructionRef.annotate("п. 133"))
        assertEquals("без ссылок", InstructionRef.annotate("без ссылок"))
        val already = "п. 35 прил. 2 $g"
        assertEquals(already, InstructionRef.annotate(already))
        assertEquals("п. 35 прил. 2 $g", InstructionRef.p("35"))
        assertEquals("табл. III.1 прил. 2 $g", InstructionRef.table("III.1"))
        assertEquals("п. 133 $g", InstructionRef.main("133"))
    }
}

class SpeedBranchesTest {
    private fun r(id: String, weight: Double, total: Double, grade: GradeClass = GradeClass.UP_TO_006) =
        BrakeCertificateCalculator.calculate(
            BrakeCertificateInput(BrakeNorms.category(id)!!, weight, listOf(AxleRow(total / 10.0, 10)), grade = grade),
        ).speed

    @Test
    fun enough_tableSpeed_notConditional() {
        val s = r("FREIGHT_LOADED", 1000.0, 400.0) as SpeedLimit.Allowed
        assertEquals(80, s.kmh)
        assertEquals(false, s.conditional)
        assertTrue(s.note.contains("Правил ТО тормозного оборудования"))
    }

    @Test
    fun betweenMinAndNorm_reduction_withVisibleSteps() {
        // 29,5 тс на 100: не хватает 3,5 -> 4 т -> 80 - 2*4 = 72 -> 70
        val s = r("FREIGHT_LOADED", 1000.0, 295.0) as SpeedLimit.Allowed
        assertEquals(70, s.kmh)
        assertEquals(80, s.reducedFrom)
        assertEquals(true, s.conditional)
        val steps = s.steps!!
        assertTrue(steps.contains("80 - 2×4 = 72"), steps)
        assertTrue(steps.contains("до кратного 5"), steps)
        assertTrue(steps.contains("п. 35 прил. 2 Правил ТО тормозного оборудования"), steps)
        assertTrue(s.condition!!.contains("п. 33-34 прил. 2 Правил ТО тормозного оборудования"))
    }

    @Test
    fun belowMinimum_notAllowedByTables_citesP37() {
        val s = r("FREIGHT_LOADED", 1000.0, 270.0) as SpeedLimit.NotDetermined // 27 < 28
        assertEquals(SpeedLimit.Kind.BELOW_MIN, s.kind)
        assertTrue(s.reason.contains("п. 37 прил. 2 Правил ТО тормозного оборудования"), s.reason)
        assertTrue(s.reason.contains("начальник железной дороги"), s.reason)
        assertTrue(s.reason.contains("20 %"), s.reason)
    }

    @Test
    fun noMinimumInTable() {
        // порожний 350-400 осей (44): в табл. 2 минимума нет
        val s = r("EMPTY_350_400", 1000.0, 300.0) as SpeedLimit.NotDetermined
        assertEquals(SpeedLimit.Kind.NO_MIN_IN_TABLE, s.kind)
    }

    @Test
    fun noNormsForSteepGrade() {
        val a = r("PASS_130", 1000.0, 900.0, GradeClass.OVER_010_TO_015) as SpeedLimit.NotDetermined
        assertEquals(SpeedLimit.Kind.NO_NORM_STEEP, a.kind)
        val b = r("FREIGHT_LOADED", 1000.0, 400.0, GradeClass.OVER_015) as SpeedLimit.NotDetermined
        assertEquals(SpeedLimit.Kind.NO_NORM_STEEP, b.kind)
        assertTrue(b.reason.contains("п. 38 прил. 2 Правил ТО тормозного оборудования"))
    }

    @Test
    fun passengerRate_perGrade() {
        // пассажирский до 120: норма 60, 55 тс на 100 -> не хватает 5 -> 120 - 1*5 = 115 -> 115 (спуск до 0,006)
        val s = r("PASS_120", 1000.0, 550.0) as SpeedLimit.Allowed
        assertEquals(115, s.kmh)
    }
}

class AxleCheckTest {
    @Test
    fun compareCases() {
        assertTrue(AxleCheck.compare(128, 0) is AxleSumCheck.NoTable)
        assertEquals(AxleSumCheck.Equal(128), AxleCheck.compare(128, 128))
        assertEquals(AxleSumCheck.TableLess(120, 128, 8), AxleCheck.compare(128, 120))
        assertEquals(AxleSumCheck.TableMore(136, 128, 8), AxleCheck.compare(128, 136))
        assertEquals(AxleSumCheck.DeclaredEmpty(120), AxleCheck.compare(null, 120))
        assertEquals(AxleSumCheck.DeclaredEmpty(120), AxleCheck.compare(0, 120))
    }

    @Test
    fun messages() {
        assertTrue(AxleCheck.message(AxleCheck.compare(128, 120))!!.contains("не хватает 8"))
        assertTrue(AxleCheck.message(AxleCheck.compare(128, 136))!!.contains("больше на 8"))
        assertEquals(null, AxleCheck.message(AxleCheck.compare(128, 128)))
        assertEquals(null, AxleCheck.message(AxleCheck.compare(128, 0)))
    }

    @Test
    fun fractionalPressureDoesNotAffectAxleSum() {
        // оси целые, нажатие дробное: сверка идёт только по числу осей
        val rows = listOf(AxleRow(8.25, 100), AxleRow(3.5, 28))
        val sum = rows.sumOf { it.axles }
        assertEquals(AxleSumCheck.Equal(128), AxleCheck.compare(128, sum))
    }
}

class RequiredPressureRoundingTest {
    private fun req(w: Double) = BrakeCertificateCalculator.calculate(
        BrakeCertificateInput(BrakeNorms.category("FREIGHT_LOADED")!!, w, listOf(AxleRow(1.0, 1))),
    ).requiredPressure

    @Test fun fractionalRoundsUp() {
        assertEquals(1004.0, req(3040.0)) // 1003,2
        assertEquals(1003.0, req(3039.39)) // 1002,99 -> 1003
        assertEquals(330.0, req(1000.0)) // целое остаётся
    }

    @Test fun enoughComparesWithRoundedRequired() {
        // 1000,5 тс: 330,165 -> 331; фактическое 330,5 меньше 331 -> не хватает
        val r = BrakeCertificateCalculator.calculate(BrakeCertificateInput(BrakeNorms.category("FREIGHT_LOADED")!!, 1000.5, listOf(AxleRow(330.5, 1))))
        assertEquals(331.0, r.requiredPressure)
        assertTrue(!r.enough)
    }
}

class HeadTailMinimumPointTest {
    private fun speedAt(per100: Double, grade: GradeClass): SpeedLimit.Allowed {
        val c = BrakeNorms.category("HEAD_TAIL_12")!!
        return BrakeCertificateCalculator.calculate(
            BrakeCertificateInput(c, 1000.0, listOf(AxleRow(per100 * 10.0, 1)), grade = grade),
        ).speed as SpeedLimit.Allowed
    }

    @Test fun atMinimum_takesSmallerOfFormulaAndTable() {
        // 28 тс: формула 75 - 2*5 = 65 (до 0,006: таблица 65 - равны), 0,006-0,010: формула 65, табл. III.2 - 50 -> 50
        assertEquals(65, speedAt(28.0, GradeClass.UP_TO_006).kmh)
        val mid = speedAt(28.0, GradeClass.OVER_006_TO_010)
        assertEquals(50, mid.kmh)
        assertTrue(mid.steps!!.contains("меньшая из двух скоростей"), mid.steps)
        assertEquals(50, speedAt(28.0, GradeClass.OVER_010_TO_015).kmh) // формула 55, табл. 50
    }

    @Test fun aboveMinimum_formulaOnly() {
        // 29 тс: не хватает 4 т -> 75 - 8 = 67 -> 65 (до 0,010) - таблица не ограничивает
        assertEquals(65, speedAt(29.0, GradeClass.OVER_006_TO_010).kmh)
    }

    @Test fun locoContributionIsIncludedAndVisible() {
        val cat = BrakeNorms.category("FREIGHT_PASS")!!
        val rows = listOf(AxleRow(7.0, 100))
        val without = BrakeCertificateCalculator.calculate(BrakeCertificateInput(cat, 1000.0, rows))
        val with = BrakeCertificateCalculator.calculate(BrakeCertificateInput(cat, 1000.0, rows, loco = LocoBrakeInput(200.0, 8, 10.0)))
        assertEquals(null, without.loco)
        assertEquals(LocoShare(8, 80.0, 200.0), with.loco)
        assertEquals(without.totalAxles + 8, with.totalAxles)
        assertEquals(without.actualPressure + 80.0, with.actualPressure, 1e-9)
        assertEquals(1200.0, with.totalWeight, 1e-9)
        assertEquals(528.0, with.requiredPressure, 1e-9) // 44 * 1200 / 100
    }

    @Test fun locoRow_mergedIntoSamePressureRow_noDoubleCount() {
        val rows = listOf(AxleRow(7.0, 100), AxleRow(5.0, 50))
        val loco = LocoBrakeInput(184.0, 8, 7.0) // такое же нажатие, как у строки 7
        val r = BrakeTable.locoRow(rows, loco)
        assertEquals(7.0, r.mergedPressure)
        assertEquals(false, r.separateRow)
        assertEquals(158, r.totalAxles) // 100 + 50 + 8 - локомотив один раз
        assertEquals(700.0 + 250.0 + 56.0, r.totalPressure, 1e-9)
    }

    @Test fun locoRow_separateWhenNoMatchingPressure() {
        val rows = listOf(AxleRow(7.0, 100))
        val r = BrakeTable.locoRow(rows, LocoBrakeInput(184.0, 8, 21.0))
        assertEquals(null, r.mergedPressure)
        assertEquals(true, r.separateRow)
        assertEquals(108, r.totalAxles)
        assertEquals(700.0 + 168.0, r.totalPressure, 1e-9)
        val none = BrakeTable.locoRow(rows, null)
        assertEquals(false, none.separateRow); assertEquals(100, none.totalAxles)
    }

    private val printed = BrakeNorms.blankAxlePressures

    @Test fun locoMerge_user14_variants() {
        // «14», «14,0», «14.0»: парсинг даёт одно и то же число
        for (txt in listOf("14", "14,0", "14.0")) {
            val p = txt.replace(',', '.').toDouble()
            val r = BrakeTable.locoRow(listOf(AxleRow(14.0, 100)), LocoBrakeInput(184.0, 8, p), printed)
            assertEquals(14.0, r.mergedPressure); assertEquals(false, r.separateRow); assertEquals(108, r.totalAxles)
        }
        // значение из справочника с плавающей точкой (сумма/деление даёт 14.000000000000002)
        val fp = 0.1 * 140.0
        assertEquals(false, BrakeTable.locoRow(listOf(AxleRow(14.0, 100)), LocoBrakeInput(184.0, 8, fp), printed).separateRow)
    }

    @Test fun locoMerge_intoPrintedRowEvenWhenNotFilled_orAvgMode() {
        // таблица пуста: чип 14 есть среди напечатанных значений, но не заполнен -> локомотив пишется в него
        val r = BrakeTable.locoRow(emptyList(), LocoBrakeInput(184.0, 8, 14.0), printed)
        assertEquals(14.0, r.mergedPressure); assertEquals(false, r.separateRow)
        assertEquals(8, r.totalAxles); assertEquals(112.0, r.totalPressure, 1e-9)
        // режим «одно среднее нажатие»: единственная строка 14 с осями
        val avg = BrakeTable.locoRow(listOf(AxleRow(14.0, 220)), LocoBrakeInput(184.0, 8, 14.0), printed)
        assertEquals(false, avg.separateRow); assertEquals(228, avg.totalAxles)
    }

    @Test fun locoMerge_fractionalCloseValueAndSeparate() {
        // близкое значение в пределах 0,001 -> слияние; дальше -> отдельная строка
        assertEquals(false, BrakeTable.locoRow(listOf(AxleRow(12.5, 10)), LocoBrakeInput(100.0, 6, 12.5004), printed).separateRow)
        assertEquals(true, BrakeTable.locoRow(listOf(AxleRow(12.5, 10)), LocoBrakeInput(100.0, 6, 12.52), listOf(12.5)).separateRow)
        // нет такого нажатия ни в строках, ни в шкале -> отдельная строка; «Всего» без двойного счёта
        val sep = BrakeTable.locoRow(listOf(AxleRow(7.0, 100)), LocoBrakeInput(100.0, 6, 13.3), printed)
        assertEquals(true, sep.separateRow); assertEquals(106, sep.totalAxles); assertEquals(700.0 + 79.8, sep.totalPressure, 1e-9)
    }

    @Test fun axleCheck_withLocoOnBothSides() {
        // вагоны: в таблице 6, в «Всего осей» 6; локомотив 8 осей - обе стороны с локомотивом, расхождения нет
        assertEquals(AxleSumCheck.Equal(14), AxleCheck.compareWithLoco(6, 6, 8))
        // реальное расхождение (4 против 6) сохраняется и не включает оси локомотива в разницу
        val diff = AxleCheck.compareWithLoco(10, 6, 8) as AxleSumCheck.TableLess
        assertEquals(4, diff.diff)
        // без локомотива - как прежде
        assertEquals(AxleSumCheck.Equal(128), AxleCheck.compareWithLoco(128, 128, 0))
        // пустая таблица не даёт ложной разницы от локомотива
        assertTrue(AxleCheck.compareWithLoco(null, 0, 8) is AxleSumCheck.NoTable)
    }
}
