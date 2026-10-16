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
        for (g in DensityLocoGroup.values()) assertTrue(!g.examplesText.contains("л)") && !g.examplesText.contains("ВЛ10 ") && !g.examplesText.contains("ориент"), g.examplesText)
        assertEquals("ВЛ60", DensityLocoGroup.G2.examplesText)
        assertEquals("1000 л", DensityLocoGroup.G1.title)
    }

    @Test
    fun noMinusTwentyPercentCorrection() {
        for (c in listOf(4.8, 5.2, 5.6, 5.7, 5.8)) assertEquals(1.0, BrakeDensity.chargeFactor(c), "$c")
        assertEquals(0.9, BrakeDensity.chargeFactor(5.4))
    }
}
