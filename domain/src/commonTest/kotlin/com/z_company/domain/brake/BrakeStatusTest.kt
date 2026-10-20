package com.z_company.domain.brake

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
        assertTrue(st.headline.contains("п. 31 прил. 2 Правил ТО тормозного оборудования"))
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
        // форма ВУ-45 (808-2022 прил. М): в том числе 7,5 и 9,5
        assertTrue(7.5 in BrakeNorms.blankAxlePressures && 9.5 in BrakeNorms.blankAxlePressures)
        assertEquals(27, BrakeNorms.blankAxlePressures.size)
    }

    @Test fun isCalculated_needsWeightAndPressure() {
        assertTrue(!BrakeStatusResolver.isCalculated(null))
        val cat = freight()
        val noPressure = BrakeCertificateCalculator.calculate(BrakeCertificateInput(cat, 1000.0, emptyList()))
        assertTrue(!BrakeStatusResolver.isCalculated(noPressure))
        val noWeight = BrakeCertificateCalculator.calculate(BrakeCertificateInput(cat, 0.0, listOf(AxleRow(5.0, 10))))
        assertTrue(!BrakeStatusResolver.isCalculated(noWeight))
        val ok = BrakeCertificateCalculator.calculate(BrakeCertificateInput(cat, 1000.0, listOf(AxleRow(5.0, 100))))
        assertTrue(BrakeStatusResolver.isCalculated(ok))
        // до расчёта плашка нейтральная, без прочерка
        assertEquals(StatusLevel.NONE, BrakeStatusResolver.resolve(noPressure, false).level)
    }

    @Test fun turnaroundEntry_808_2022_reverseColumns() {
        val e = TurnaroundEntry("t")
        assertEquals("", e.testKind) // вид по умолчанию пустой
        assertTrue(!e.compositionChanged) // графы 3-6 - прочерк, если состав не менялся
        assertTrue(e.copy(newWeight = "3100").compositionChanged)
        assertTrue(e.copy(newActual = "1010").compositionChanged)
    }

    @Test fun sourceRefs_point_to_808_2022() {
        assertEquals("Инструкция 808-2022 ПКБ ЦВ, прил. М", InstructionRef.FORM_DOC)
    }

    @Test fun byTrainNumber_unambiguousRanges() {
        assertEquals("FREIGHT_PASS", BrakeCategory.byTrainNumber("961")?.id)
        assertEquals("FREIGHT_PASS", BrakeCategory.byTrainNumber("970")?.id)
        assertEquals(null, BrakeCategory.byTrainNumber("960")) // людские
        assertEquals(null, BrakeCategory.byTrainNumber("971")) // почтово-багажные
        assertEquals("EMPTY_350", BrakeCategory.byTrainNumber("9301")?.id)
        assertEquals("EMPTY_350", BrakeCategory.byTrainNumber("9398")?.id)
        assertEquals(null, BrakeCategory.byTrainNumber("9399"))
        assertEquals(null, BrakeCategory.byTrainNumber("9300"))
        // 350-520 осей: по числу осей, иначе не определяется
        assertEquals(null, BrakeCategory.byTrainNumber("9401"))
        assertEquals("EMPTY_350_400", BrakeCategory.byTrainNumber("9401", axles = 350)?.id)
        assertEquals("EMPTY_350_400", BrakeCategory.byTrainNumber("9498", axles = 400)?.id)
        assertEquals("EMPTY_400_520", BrakeCategory.byTrainNumber("9450", axles = 401)?.id)
        assertEquals("EMPTY_400_520", BrakeCategory.byTrainNumber("9450", axles = 520)?.id)
        assertEquals(null, BrakeCategory.byTrainNumber("9450", axles = 349))
        assertEquals(null, BrakeCategory.byTrainNumber("9450", axles = 521))
        assertEquals(null, BrakeCategory.byTrainNumber("9499", axles = 400))
    }

    @Test fun byTrainNumber_notDetermined() {
        for (n in listOf("2604", "3001", "3405", "3501", "3601", "1001", "9201", "9501", "701", "6001", "7101", "8201")) {
            assertEquals(null, BrakeCategory.byTrainNumber(n, axles = 400), n)
        }
    }

    @Test fun byTrainNumber_formats() {
        assertEquals("FREIGHT_PASS", BrakeCategory.byTrainNumber("№ 965")?.id)
        assertEquals("FREIGHT_PASS", BrakeCategory.byTrainNumber("№965")?.id)
        assertEquals("FREIGHT_PASS", BrakeCategory.byTrainNumber("965А")?.id)
        assertEquals("FREIGHT_PASS", BrakeCategory.byTrainNumber(" 965 ")?.id)
        assertEquals("EMPTY_350", BrakeCategory.byTrainNumber("9310Б")?.id)
        assertEquals(null, BrakeCategory.byTrainNumber(""))
        assertEquals(null, BrakeCategory.byTrainNumber(null))
        assertEquals(null, BrakeCategory.byTrainNumber("abc"))
        assertEquals(null, BrakeCategory.byTrainNumber("А965"))
        assertEquals(null, BrakeCategory.byTrainNumber("0"))
        assertEquals(null, BrakeCategory.byTrainNumber("12345"))
        assertEquals(2604, BrakeCategory.parseTrainNumber("№ 2604А"))
    }

    @Test fun speedBar_speedShownOnlyWhenDetermined() {
        // OK
        assertEquals(80, BrakeStatusResolver.speedKmh(calc(freight().norm + 1.0)))
        // WARNING: нажатия не хватает, скорость не снижена - скорость есть
        assertEquals(80, BrakeStatusResolver.speedKmh(calc(31.5, CompositeShare.P75)))
        // снижена до N
        assertEquals(70, BrakeStatusResolver.speedKmh(calc(29.5)))
        // ниже минимума - скорости нет (плашка только с текстом)
        assertEquals(null, BrakeStatusResolver.speedKmh(calc(freight().minNorm!! - 1.0)))
        // до расчёта
        assertEquals(null, BrakeStatusResolver.speedKmh(null))
    }

    @Test fun locoCounted_byCategory() {
        for (c in BrakeNorms.categories) {
            val passengerLike = c.group == TrainGroup.PASSENGER || c.group == TrainGroup.FREIGHT_PASSENGER
            assertEquals(passengerLike, c.locoCountedByDefault, c.id)
        }
        assertTrue(BrakeCategory.byTrainNumber("965")!!.locoCountedByDefault)
        assertTrue(!BrakeCategory.byTrainNumber("9310")!!.locoCountedByDefault)
    }

    @Test fun suggestByTrainNumber_passenger22() {
        // 22 - пассажирский скорый (859р, диапазон 1-150): из груженого грузового выбирается пассажирская категория по умолчанию
        val r = BrakeCategory.suggestByTrainNumber("22", null, "FREIGHT_LOADED")
        assertEquals("PASS_120", r.categoryId)
        assertTrue(r.note != null)
        assertTrue(!BrakeNorms.category(r.categoryId!!)!!.let { !it.locoCountedByDefault })
        // уже выбранная пассажирская категория не меняется
        assertEquals(null, BrakeCategory.suggestByTrainNumber("22", null, "PASS_140").categoryId)
        assertEquals(null, BrakeCategory.suggestByTrainNumber("22", null, "PASS_140").note)
        // границы пассажирских диапазонов
        assertEquals("PASS_120", BrakeCategory.suggestByTrainNumber("298", null, "FREIGHT_LOADED").categoryId)
        assertEquals(null, BrakeCategory.suggestByTrainNumber("299", null, "FREIGHT_LOADED").categoryId)
        assertEquals("PASS_120", BrakeCategory.suggestByTrainNumber("301", null, "FREIGHT_LOADED").categoryId)
        assertEquals("PASS_120", BrakeCategory.suggestByTrainNumber("698", null, "FREIGHT_LOADED").categoryId)
        assertEquals(null, BrakeCategory.suggestByTrainNumber("699", null, "FREIGHT_LOADED").categoryId)
    }

    @Test fun suggestByTrainNumber_unambiguousOverridesRemembered_otherKeeps() {
        assertEquals("FREIGHT_PASS", BrakeCategory.suggestByTrainNumber("965", null, "PASS_120").categoryId)
        assertEquals("EMPTY_350", BrakeCategory.suggestByTrainNumber("9310", null, "FREIGHT_LOADED").categoryId)
        val amb = BrakeCategory.suggestByTrainNumber("2604", null, "FREIGHT_LOADED")
        assertEquals(null, amb.categoryId)
        assertTrue(amb.note!!.contains("не определяется"))
        assertEquals(null, BrakeCategory.suggestByTrainNumber("", null, "FREIGHT_LOADED").note)
        assertEquals(null, BrakeCategory.suggestByTrainNumber(null, null, "FREIGHT_LOADED").categoryId)
    }

    @Test fun catalog_newElectricLocos_caseAndLatinInsensitive() {
        for (s in listOf("2ЭС6", "2эс6", "2 эс 6", "2ЭС4К", "2эс4к", "3ЭС4К", "3эс5к", "2ЭС5К")) {
            val d = BrakeLocoCatalog.find(s)
            assertNotNull(d, s)
            assertEquals(null, d.pressurePerAxle) // достоверного источника нет - ввод вручную
        }
        assertEquals(192.0, BrakeLocoCatalog.find("2эс6")!!.weight)
        assertEquals(12, BrakeLocoCatalog.find("3ЭС5К")!!.axles)
        // латинские двойники (E, C, K) тоже находятся
        assertNotNull(BrakeLocoCatalog.find("3ЭC4K"))
        // объём ГР 3000 л (со слов владельца) для 3ЭС4К/3ЭС5К; 2000 л для 2ЭС (справочно)
        assertEquals(DensityLocoGroup.G7, BrakeDensity.groupBySeries("3эс4к"))
        assertEquals(DensityLocoGroup.G7, BrakeDensity.groupBySeries("3ЭС5К"))
        assertEquals(DensityLocoGroup.G5, BrakeDensity.groupBySeries("2ЭС6")) // справочно 2000 л
        assertEquals(DensityLocoGroup.G5, BrakeDensity.groupBySeries("2эс4к"))
        assertEquals(DensityLocoGroup.G5, BrakeDensity.groupBySeries("2ЭС5К"))
        assertEquals(DensityLocoGroup.G5, (BrakeDensity.groupByVolume("2000") as BrakeDensity.VolumeGroup.Found).group)
        // существующие серии не изменились
        assertEquals(14.0, BrakeLocoCatalog.find("ВЛ10У")!!.pressurePerAxle)
    }
}
