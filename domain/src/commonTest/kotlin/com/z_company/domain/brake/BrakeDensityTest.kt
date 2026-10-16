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
        // колонка 481-530 (табл. IV.1 Правил): 1000 л -> 11 с, 1800 л -> 20 с, 3000 л -> 33 с
        assertEquals(20.0, BrakeDensity.minTimeSeconds(DensityLocoGroup.G4, 481, null))
        assertEquals(11.0, BrakeDensity.minTimeSeconds(DensityLocoGroup.G1, 530, null))
        assertEquals(33.0, BrakeDensity.minTimeSeconds(DensityLocoGroup.G7, 500, null))
        assertNull(BrakeDensity.minTimeSeconds(DensityLocoGroup.G4, 531, null))
    }

    @Test
    fun table91_allRowsHaveTenColumns() {
        for (g in DensityLocoGroup.values()) assertEquals(10, g.minTimes.size, g.name)
    }

    @Test
    fun chargePressureReducesNorm() {
        // Правила: только 0,52-0,54 МПа (5,3-5,5 кгс/см2) -> минус 10 %; поправка «-20 % при 5,6-5,8» из Инструкции 277 удалена
        assertEquals(46.0, BrakeDensity.minTimeSeconds(DensityLocoGroup.G4, 230, 5.7))
        assertEquals(46.0, BrakeDensity.minTimeSeconds(DensityLocoGroup.G4, 230, 5.8))
        assertEquals(46.0 * 0.9, BrakeDensity.minTimeSeconds(DensityLocoGroup.G4, 230, 5.4))
        assertEquals(46.0 * 0.9, BrakeDensity.minTimeSeconds(DensityLocoGroup.G4, 230, 5.3))
        assertEquals(46.0 * 0.9, BrakeDensity.minTimeSeconds(DensityLocoGroup.G4, 230, 5.5))
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
        // порог IV - целое вверх: 0,9 x 46 = 41,4 -> 42
        assertEquals(BrakeDensity.Verdict.OK, BrakeDensity.assessFreightIV(42.0, 46.0).verdict)
        assertEquals(BrakeDensity.Verdict.FAIL, BrakeDensity.assessFreightIV(41.9, 46.0).verdict)
        assertEquals(BrakeDensity.Verdict.FAIL, BrakeDensity.assessFreightIV(41.4, 46.0).verdict)
        assertTrue(BrakeDensity.assessFreightIV(42.0, 46.0).text.contains("не менее 42 с"), BrakeDensity.assessFreightIV(42.0, 46.0).text)
        // II = 70: порог ровно 63 (без ошибки вещественной арифметики)
        assertEquals(BrakeDensity.Verdict.OK, BrakeDensity.assessFreightIV(63.0, 70.0).verdict)
        assertEquals(BrakeDensity.Verdict.FAIL, BrakeDensity.assessFreightIV(62.9, 70.0).verdict)
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
        // п. 48 а)-в) Правил: отклонение от зарядного не более 0,3 (до 300 осей), 0,5 (300-400), 0,7 (более 400)
        assertEquals(4.7, BrakeDensity.minTailPressure(5.0, 200))
        assertEquals(4.7, BrakeDensity.minTailPressure(5.0, 300))
        assertEquals(4.5, BrakeDensity.minTailPressure(5.0, 301))
        assertEquals(4.5, BrakeDensity.minTailPressure(5.0, 400))
        assertEquals(4.3, BrakeDensity.minTailPressure(5.0, 401))
        assertEquals(5.1, BrakeDensity.minTailPressure(5.4, 200))
        assertEquals(5.2, BrakeDensity.minTailPressure(5.7, 350))
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

class BrakeTurnaroundTest {
    private fun st(id: String, order: Int, arr: Long?, dep: Long?, final: Boolean = false, passing: Boolean = false, name: String? = id) =
        com.z_company.domain.entities.route.Station(
            stationId = id, stationName = name, timeArrival = arr, timeDeparture = dep,
            orderIndex = order, isFinalStation = final, isPassingStation = passing,
        )

    private val min = 60_000L

    @Test
    fun classify() {
        assertNull(TurnaroundPlanner.classify(5 * min))
        assertEquals(StopClass.OVER_5, TurnaroundPlanner.classify(5 * min + 1))
        assertEquals(StopClass.OVER_5, TurnaroundPlanner.classify(30 * min))
        assertEquals(StopClass.OVER_30, TurnaroundPlanner.classify(30 * min + 1))
    }

    @Test
    fun candidatesFromStations() {
        val now = 1_000 * min
        val stations = listOf(
            st("A", 0, null, 100 * min), // начальная: только отправление
            st("B", 1, 200 * min, 203 * min), // 3 мин - не стоянка
            st("C", 2, 300 * min, 310 * min), // 10 мин - >5
            st("D", 3, 400 * min, 450 * min), // 50 мин - >30
            st("E", 4, 500 * min, null, passing = true), // проходная
            st("F", 5, now - 40 * min, null), // идёт 40 мин
            st("G", 6, now - 50 * min, null, final = true), // конечная: не считается
        )
        val c = TurnaroundPlanner.candidates(stations, now)
        assertEquals(listOf("C", "D", "F"), c.map { it.stationId })
        assertEquals(StopClass.OVER_5, c[0].stopClass)
        assertEquals(StopClass.OVER_30, c[1].stopClass)
        assertTrue(c[2].ongoing)
        assertEquals(StopClass.OVER_30, c[2].stopClass)
    }

    @Test
    fun appliesNotToPassenger() {
        assertTrue(TurnaroundPlanner.applies(BrakeNorms.category("FREIGHT_LOADED")!!))
        assertTrue(TurnaroundPlanner.applies(BrakeNorms.category("FREIGHT_PASS")!!))
        assertTrue(!TurnaroundPlanner.applies(BrakeNorms.category("PASS_120")!!))
    }

    @Test
    fun dataRoundTripJson() {
        val d = BrakeCertificateData(
            id = "x", categoryId = "FREIGHT_LOADED", weight = "3040", totalAxles = "128", avgPressure = "8,5",
            rows = listOf(AxleRowData("8,5", "128")), flags = listOf("ЭПТ"), densityGroup = DensityLocoGroup.G4,
            measurements = listOf(DensityMeasurementData("II", 73.2, "8,5", "8,0")),
            turnaround = listOf(TurnaroundEntry("t1", "s1", "Луга", StopClass.OVER_30, 1L, 2L, 41.0, "17:40", "32027", true)),
        )
        val json = kotlinx.serialization.json.Json { encodeDefaults = true }
        val back = json.decodeFromString(BrakeCertificateData.serializer(), json.encodeToString(BrakeCertificateData.serializer(), d))
        assertEquals(d, back)
    }

    @Test
    fun autoGroupBySeries_onlyForKnownVolumes() {
        assertEquals(DensityLocoGroup.G1, BrakeDensity.groupBySeries("ЭП1"))
        assertEquals(DensityLocoGroup.G1, BrakeDensity.groupBySeries("ЧС7"))
        assertEquals(DensityLocoGroup.G1, BrakeDensity.groupBySeries("ТЭП70"))
        assertEquals(DensityLocoGroup.G1, BrakeDensity.groupBySeries("ТЭМ2"))
        assertEquals(DensityLocoGroup.G1, BrakeDensity.groupBySeries("ЧМЭ3"))
        assertEquals(DensityLocoGroup.G1, BrakeDensity.groupBySeries("ВЛ65"))
        assertEquals(DensityLocoGroup.G2, BrakeDensity.groupBySeries("ВЛ60К"))
        assertEquals(DensityLocoGroup.G3, BrakeDensity.groupBySeries("ВЛ10У"))
        assertEquals(DensityLocoGroup.G5, BrakeDensity.groupBySeries("2ТЭ116"))
        assertEquals(DensityLocoGroup.G5, BrakeDensity.groupBySeries("ВЛ85"))
        // ВЛ10: объём зависит от номера (№ 1-18 - 1960 л, с № 19 - 1500 л) - автоподбора нет
        assertNull(BrakeDensity.groupBySeries("ВЛ10"))
        // серия без подтверждённого объёма (ЧС2: диапазон 980-1080 л) - «Не выбрана»
        assertNull(BrakeDensity.groupBySeries("ЧС2"))
        assertNull(BrakeDensity.groupBySeries("2ЭС5К"))
    }

    @Test
    fun examplesFollowNearestSmallerRowRule() {
        val rows = DensityLocoGroup.values().map { it.liters }
        for (g in DensityLocoGroup.values()) for (e in g.examples) {
            val minVolume = Regex("""\d+""").find(e.volume)!!.value.toInt()
            val expected = rows.filter { it <= minVolume }.maxOrNull()
            assertEquals(expected, g.liters, "${e.series} ${e.volume}")
        }
        assertTrue(DensityLocoGroup.G1.examplesText.contains("ЭП1"))
        // в UI только названия серий: без объёмов, пометок и сомнительных (ВЛ10 с зависимостью объёма от номера)
        for (g in DensityLocoGroup.values()) assertTrue(!g.examplesText.contains("л)") && !g.examplesText.contains("ориент"), g.examplesText)
        assertEquals("ВЛ60", DensityLocoGroup.G2.examplesText)
        assertEquals("3ЭС4К, 3ЭС5К", DensityLocoGroup.G7.examplesText)
        assertEquals("ВЛ10 (№ 19+), ВЛ10У, ВЛ11".split(", ").toSet(), DensityLocoGroup.G3.examplesText.split(", ").toSet())
        assertTrue(DensityLocoGroup.G4.examplesText.contains("ВЛ10 (№ 1-18)"))
        assertEquals("1000 л", DensityLocoGroup.G1.title)
    }

    @Test
    fun noMinusTwentyPercentCorrection() {
        for (c in listOf(4.8, 5.2, 5.6, 5.7, 5.8)) assertEquals(1.0, BrakeDensity.chargeFactor(c), "$c")
        assertEquals(0.9, BrakeDensity.chargeFactor(5.4))
    }

    @Test
    fun threeES_group3000() {
        assertEquals(DensityLocoGroup.G7, BrakeDensity.groupBySeries("3ЭС4К"))
        assertEquals(DensityLocoGroup.G7, BrakeDensity.groupBySeries("3ЭС5К"))
        assertEquals(DensityLocoGroup.G7, BrakeDensity.groupForLoco("3эс5к", "0123"))
    }

    @Test
    fun vl10_groupByNumber_boundary18_19() {
        assertEquals(DensityLocoGroup.G4, BrakeDensity.groupForLoco("ВЛ10", "1"))
        assertEquals(DensityLocoGroup.G4, BrakeDensity.groupForLoco("ВЛ10", "18"))
        assertEquals(DensityLocoGroup.G3, BrakeDensity.groupForLoco("ВЛ10", "19"))
        assertEquals(DensityLocoGroup.G3, BrakeDensity.groupForLoco("ВЛ10", "1234"))
        assertEquals(DensityLocoGroup.G3, BrakeDensity.groupForLoco("ВЛ10", "1234А"))
        assertEquals(DensityLocoGroup.G3, BrakeDensity.groupForLoco("ВЛ10", "ВЛ10-1234"))
        assertEquals(DensityLocoGroup.G4, BrakeDensity.groupForLoco("ВЛ10", "0012Б"))
        // без догадок
        assertNull(BrakeDensity.groupForLoco("ВЛ10", ""))
        assertNull(BrakeDensity.groupForLoco("ВЛ10", null))
        assertNull(BrakeDensity.groupForLoco("ВЛ10", "0"))
        assertNull(BrakeDensity.groupForLoco("ВЛ10", "abc"))
        assertNull(BrakeDensity.groupForLoco("ВЛ10", "99999"))
        // ВЛ10У - 1500 л независимо от номера; остальные серии - как раньше
        assertEquals(DensityLocoGroup.G3, BrakeDensity.groupForLoco("ВЛ10У", ""))
        assertEquals(DensityLocoGroup.G4, BrakeDensity.groupForLoco("ВЛ80С", ""))
        assertNull(BrakeDensity.groupForLoco("2ЭС5К", "100"))
    }

    @Test
    fun parseLocoNumber_formats() {
        assertEquals(1234, BrakeDensity.parseLocoNumber("1234"))
        assertEquals(1234, BrakeDensity.parseLocoNumber(" 1234А "))
        assertEquals(1234, BrakeDensity.parseLocoNumber("1234/1235"))
        assertEquals(1234, BrakeDensity.parseLocoNumber("ВЛ10-1234"))
        assertEquals(1234, BrakeDensity.parseLocoNumber("ВЛ10 № 1234Б"))
        assertEquals(7, BrakeDensity.parseLocoNumber("№007"))
        assertNull(BrakeDensity.parseLocoNumber(""))
        assertNull(BrakeDensity.parseLocoNumber("А1234"))
    }

    @Test
    fun customVolume_groupByNearestSmaller() {
        fun g(t: String) = (BrakeDensity.groupByVolume(t) as? BrakeDensity.VolumeGroup.Found)?.group
        assertEquals(DensityLocoGroup.G3, g("1700")) // между 1500 и 1800 -> 1500
        assertEquals(DensityLocoGroup.G4, g("1800"))
        assertEquals(DensityLocoGroup.G3, g("1799,9"))
        assertEquals(DensityLocoGroup.G1, g("1000"))
        assertEquals(DensityLocoGroup.G1, g("1199.99"))
        assertEquals(DensityLocoGroup.G2, g("1200"))
        assertEquals(DensityLocoGroup.G7, g("3000"))
        assertEquals(DensityLocoGroup.G7, g("4500")) // больше таблицы - ближайший наименьший 3000
        assertTrue(BrakeDensity.groupByVolume("999") is BrakeDensity.VolumeGroup.BelowTable)
        assertTrue(BrakeDensity.groupByVolume("999,99") is BrakeDensity.VolumeGroup.BelowTable)
        assertTrue(BrakeDensity.groupByVolume("") is BrakeDensity.VolumeGroup.Invalid)
        assertTrue(BrakeDensity.groupByVolume("abc") is BrakeDensity.VolumeGroup.Invalid)
        assertTrue(BrakeDensity.groupByVolume("0") is BrakeDensity.VolumeGroup.Invalid)
        assertTrue(BrakeDensity.groupByVolume("-5") is BrakeDensity.VolumeGroup.Invalid)
        assertEquals("1700 л - округляем до 1500 л", BrakeDensity.volumeCaption("1700"))
        assertEquals("1450 л - округляем до 1200 л", BrakeDensity.volumeCaption("1450"))
        assertEquals("1500 л - группа 1500 л", BrakeDensity.volumeCaption("1500"))
        assertEquals("4500 л - округляем до 3000 л", BrakeDensity.volumeCaption("4500"))
        assertEquals("1700,5 л - округляем до 1500 л", BrakeDensity.volumeCaption("1700,5"))
        assertTrue(BrakeDensity.volumeCaption("800").contains("оценка недоступна"))
    }

    @Test
    fun customVolume_serialization_additiveField() {
        val json = kotlinx.serialization.json.Json { encodeDefaults = true; ignoreUnknownKeys = true }
        val old = """{"id":"x","categoryId":"FREIGHT_LOADED","grade":"UP_TO_006"}"""
        val parsedOld = json.decodeFromString(BrakeCertificateData.serializer(), old)
        assertEquals("", parsedOld.customVolume)
        val withVol = BrakeCertificateData(id = "x", categoryId = "FREIGHT_LOADED", grade = GradeClass.UP_TO_006, customVolume = "1700")
        val back = json.decodeFromString(BrakeCertificateData.serializer(), json.encodeToString(BrakeCertificateData.serializer(), withVol))
        assertEquals("1700", back.customVolume)
    }

    @Test
    fun fillFromRoute_seriesChanged_regroupsAutomatically() {
        val first = BrakeFillResolver.resolveLoco("ЭП1", "123", "", "", manualGroup = null)
        assertEquals("ЭП1", first.series)
        assertEquals(DensityLocoGroup.G1, first.effectiveGroup)
        // в маршруте серия сменилась (ВЛ80С): актуальные данные берутся сразу, группа определяется заново (1800 л)
        val second = BrakeFillResolver.resolveLoco("ВЛ80С", "2345", first.series, "123", first.manualGroup)
        assertEquals("ВЛ80С", second.series)
        assertEquals("2345", second.number)
        assertEquals(DensityLocoGroup.G4, second.effectiveGroup)
        // ВЛ10: группа по номеру из маршрута
        assertEquals(DensityLocoGroup.G4, BrakeFillResolver.resolveLoco("ВЛ10", "17", "", "", null).effectiveGroup)
        assertEquals(DensityLocoGroup.G3, BrakeFillResolver.resolveLoco("ВЛ10", "20", "", "", null).effectiveGroup)
    }

    @Test
    fun fillFromRoute_resetsManualGroupAndCustomVolume_onlyWhenRouteSeriesDeterminesGroup() {
        // серия из маршрута определяет группу -> ручная группа и свой объём сбрасываются
        val a = BrakeFillResolver.resolveLoco("ВЛ80С", "1", "ЭП1", "9", manualGroup = DensityLocoGroup.G6, customVolume = "")
        assertEquals("ВЛ80С", a.series)
        assertEquals(null, a.manualGroup)
        assertEquals(DensityLocoGroup.G4, a.effectiveGroup)
        assertTrue(a.groupReset)
        val b = BrakeFillResolver.resolveLoco("ВЛ80С", "1", "ЭП1", "9", manualGroup = null, customVolume = "1450")
        assertEquals("", b.customVolume)
        assertEquals(DensityLocoGroup.G4, b.effectiveGroup)
        // серия не определяет группу (2ЭС5К) -> группа и свой объём не трогаются, серия и номер заменяются
        val c = BrakeFillResolver.resolveLoco("2ЭС5К", "77", "ЭП1", "9", manualGroup = DensityLocoGroup.G6, customVolume = "")
        assertEquals("2ЭС5К", c.series)
        assertEquals("77", c.number)
        assertEquals(DensityLocoGroup.G6, c.manualGroup)
        assertTrue(!c.groupReset)
        val d = BrakeFillResolver.resolveLoco("2ЭС5К", "77", "ЭП1", "9", manualGroup = null, customVolume = "1700")
        assertEquals("1700", d.customVolume)
        assertEquals(DensityLocoGroup.G3, d.effectiveGroup)
        // ВЛ10 без разбираемого номера: группа не определяется -> ручная остаётся
        val e = BrakeFillResolver.resolveLoco("ВЛ10", "", "", "", manualGroup = DensityLocoGroup.G6)
        assertEquals(DensityLocoGroup.G6, e.manualGroup)
        // в маршруте нет локомотива: всё остаётся, как введено
        val none = BrakeFillResolver.resolveLoco(null, null, "ЭП1", "9", DensityLocoGroup.G6, "")
        assertEquals("ЭП1", none.series)
        assertEquals("9", none.number)
        assertEquals(DensityLocoGroup.G6, none.manualGroup)
    }

    @Test
    fun fillFromRoute_routeValuesReplaceInput_emptyRouteValuesDoNot() {
        assertEquals("3100", BrakeFillResolver.pickRouteValue("2900", "3100")) // введённое заменяется
        assertEquals("2900", BrakeFillResolver.pickRouteValue("2900", null)) // пустое поле маршрута не затирает
        assertEquals("2900", BrakeFillResolver.pickRouteValue("2900", ""))
        // при открытии экрана - только пустые поля
        assertEquals("2900", BrakeFillResolver.pickOnOpen("2900", "3100"))
        assertEquals("3100", BrakeFillResolver.pickOnOpen("", "3100"))
    }

    @Test
    fun normIV_isNinetyPercentOfII_ceiledToInteger() {
        assertEquals(63.0, BrakeDensity.normIvSeconds(69.0)) // 62,1 -> 63
        assertEquals(63.0, BrakeDensity.normIvSeconds(70.0)) // 0,9*70 = 63,00000000000001 не даёт 64
        assertEquals(56.0, BrakeDensity.normIvSeconds(62.1)) // 55,89 -> 56
        assertEquals(9.0, BrakeDensity.normIvSeconds(10.0)) // ровно 9
        assertEquals(10.0, BrakeDensity.normIvSeconds(11.0)) // 9,9 -> 10
        assertEquals(27.0, BrakeDensity.normIvSeconds(29.0)) // 26,1 -> 27
        assertEquals(38.0, BrakeDensity.normIvSeconds(41.4)) // 37,26 -> 38
        assertEquals(1.0, BrakeDensity.normIvSeconds(1.0)) // 0,9 -> 1
        assertEquals(90.0, BrakeDensity.normIvSeconds(100.0)) // 90,00000000000001 -> 90
        assertEquals(45.0, BrakeDensity.normIvSeconds(50.0)) // 45,0
        assertEquals(81.0, BrakeDensity.normIvSeconds(90.0)) // 81,0
        assertEquals("63", BrakeDensity.formatNormSeconds(BrakeDensity.normIvSeconds(69.0)))
        // норма II - как раньше: «не менее» вверх до 0,1 с, без лишнего «,0»
        assertEquals("69", BrakeDensity.formatNormSeconds(69.0))
        assertEquals("62,1", BrakeDensity.formatNormSeconds(62.1))
    }

    @Test
    fun normLines_iiAndIv_orHints() {
        // группа 1200 л, 230 осей (201-250): 29 с -> IV не менее 26,1
        val lines = BrakeDensity.normLines(DensityLocoGroup.G2, 230, null)
        assertEquals("Норма плотности во II-м: не менее 29 сек", lines[0])
        assertEquals("Норма плотности в IV-м: не менее 27 сек", lines[1])
        // 1200 л, до 100 осей: 69 -> 62,1
        assertEquals("Норма плотности в IV-м: не менее 63 сек", BrakeDensity.normLines(DensityLocoGroup.G2, 100, null)[1])
        // поправка -10 % при 5,4: II = 69*0.9 = 62,1; IV = 55,89 -> 56
        val corr = BrakeDensity.normLines(DensityLocoGroup.G2, 100, 5.4)
        assertEquals("Норма плотности во II-м: не менее 62,1 сек", corr[0])
        assertEquals("Норма плотности в IV-м: не менее 56 сек", corr[1])
        assertEquals(listOf("Норма плотности во II-м: укажите объём ГР"), BrakeDensity.normLines(null, 100, null))
        assertEquals(listOf("Норма плотности во II-м: укажите число осей"), BrakeDensity.normLines(DensityLocoGroup.G2, 0, null))
        assertEquals(1, BrakeDensity.normLines(DensityLocoGroup.G2, 600, null).size)
    }

    @Test
    fun oldSavedCertificate_withGrFields_stillReads() {
        // давление ГР убрано из UI и расчётов, но старые справки с этими полями читаются (поля необязательные)
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val old = """{"id":"x","categoryId":"FREIGHT_LOADED","grade":"UP_TO_006","measurements":[{"kind":"II","seconds":73.2,"grStart":"8,5","grEnd":"8,0"}]}"""
        val d = json.decodeFromString(BrakeCertificateData.serializer(), old)
        assertEquals(73.2, d.measurements[0].seconds)
        val noGr = json.decodeFromString(BrakeCertificateData.serializer(), """{"id":"x","categoryId":"FREIGHT_LOADED","grade":"UP_TO_006","measurements":[{"kind":"IV","seconds":50.0}]}""")
        assertEquals(50.0, noGr.measurements[0].seconds)
    }

    @Test
    fun assessments_failTexts_andVerdicts() {
        // замер ниже нормы: FAIL (в UI «недостаточно»), норма - OK
        assertEquals(BrakeDensity.Verdict.FAIL, BrakeDensity.assessFreightII(45.0, 46.0).verdict)
        assertEquals(BrakeDensity.Verdict.OK, BrakeDensity.assessFreightII(46.0, 46.0).verdict)
        assertEquals(BrakeDensity.Verdict.FAIL, BrakeDensity.assessFreightIV(41.0, 46.0).verdict)
        assertEquals(BrakeDensity.Verdict.FAIL, BrakeDensity.assessRoute(30.0, 46.0).verdict)
        assertEquals(BrakeDensity.Verdict.NO_NORM, BrakeDensity.assessFreightII(45.0, null).verdict)
    }

    @Test
    fun volumeFieldText_valueThenAuto() {
        assertEquals("1800 л · авто", BrakeDensity.volumeFieldText("", DensityLocoGroup.G4, manuallySelected = false))
        assertEquals("1800 л", BrakeDensity.volumeFieldText("", DensityLocoGroup.G4, manuallySelected = true))
        assertEquals("Не выбрана", BrakeDensity.volumeFieldText("", null, false))
        assertEquals("1450 л → 1200 л", BrakeDensity.volumeFieldText("1450", DensityLocoGroup.G2, true))
        assertEquals("1500 л", BrakeDensity.volumeFieldText("1500", DensityLocoGroup.G3, true))
        assertEquals("800 л · нет нормы", BrakeDensity.volumeFieldText("800", null, true))
        // ВЛ10 по номеру: значение группы и «авто»
        assertEquals("1800 л · авто", BrakeDensity.volumeFieldText("", BrakeDensity.groupForLoco("ВЛ10", "5"), false))
        assertEquals("1500 л · авто", BrakeDensity.volumeFieldText("", BrakeDensity.groupForLoco("ВЛ10", "25"), false))
    }

    @Test
    fun manualFieldText_valueThenAuto() {
        assertEquals("0,6 · авто", BrakeNorms.manualFieldText(BrakeNorms.manualInfo(SlopeStep.S006, null)))
        assertEquals("0,8 · авто", BrakeNorms.manualFieldText(BrakeNorms.manualInfo(SlopeStep.S010, null)))
        assertEquals("0,8", BrakeNorms.manualFieldText(BrakeNorms.manualInfo(SlopeStep.S006, BrakeNorms.ManualBrakeNorm.SLOPE_010)))
        assertEquals("1,2", BrakeNorms.manualFieldText(BrakeNorms.manualInfo(SlopeStep.S006, BrakeNorms.ManualBrakeNorm.SLOPE_014)))
    }
}
