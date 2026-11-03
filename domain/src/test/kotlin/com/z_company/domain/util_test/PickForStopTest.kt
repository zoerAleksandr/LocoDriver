package com.z_company.domain.util_test

import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.entities.setting.ServicePhasePoint
import com.z_company.domain.entities.setting.ServicePhaseStation
import com.z_company.domain.util.ServicePhaseStationPicker
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PickForStopTest {
    // Плотный узел: «Узел-1» и «Узел-2» в ~280 м друг от друга (0.005° долготы на 60° с.ш.).
    private val phase = ServicePhase(
        departureStation = "Начало",
        arrivalStation = "Конец",
        distance = 50,
        departurePoint = ServicePhasePoint(60.0, 29.0),
        arrivalPoint = ServicePhasePoint(60.0, 30.5),
        intermediateStations = listOf(
            ServicePhaseStation(name = "Узел-1", latitude = 60.0, longitude = 30.000),
            ServicePhaseStation(name = "Узел-2", latitude = 60.0, longitude = 30.005),
        ),
    )
    private val dirPoints = mapOf("справочник" to (60.0 to 30.002))
    private val dirNames = mapOf("справочник" to "Справочная")

    private fun pick(phase: ServicePhase?, train: List<String?>, lon: Double) =
        ServicePhaseStationPicker.pickForStop(phase, train, 60.0, lon, dirPoints, dirNames)

    @Test
    fun `dense node - nearest by path among candidates, not earlier than last written`() {
        // Стоим у «Узел-2» (35 м): ближайшая - она; допустима, т.к. записана только «Начало».
        assertEquals("Узел-2", pick(phase, listOf("Начало"), 30.0045))
        // «Узел-2» уже записана - повтор допускается («уже на станции»).
        assertEquals("Узел-2", pick(phase, listOf("Начало", "Узел-1", "Узел-2"), 30.0045))
        // Записана «Узел-2»: «Узел-1» и «Справочная» позади, у 30.0 подходит только «Узел-2» (стоим в 0 м от «Узел-1», она позади - «Узел-2» в 350 м).
        assertEquals("Узел-2", pick(phase, listOf("Начало", "Узел-1", "Узел-2"), 30.0))
    }

    @Test
    fun `nothing written - nearest in the radius`() {
        assertEquals("Узел-1", pick(phase, emptyList(), 30.0))
    }

    @Test
    fun `directory station on the leg between two leg stations is chosen and ordered`() {
        // «Справочная» (30.002) лежит между «Узел-1» (30.000) и «Узел-2» (30.005) на прямой плеча.
        assertEquals("Справочная", pick(phase, listOf("Начало"), 30.002))
        // «Узел-1» записана: «Справочная» после неё - подходит.
        assertEquals("Справочная", pick(phase, listOf("Начало", "Узел-1"), 30.002))
        // «Узел-2» записана: «Справочная» позади - не подходит, берётся «Узел-2» (в 1 км).
        assertEquals("Узел-2", pick(phase, listOf("Начало", "Узел-1", "Узел-2"), 30.002))
        // «Справочная» записана: у «Узел-1» она «ровно здесь» не подходит, ближайшая впереди - «Справочная» (повтор).
        assertEquals("Справочная", pick(phase, listOf("Начало", "Справочная"), 30.0))
    }

    @Test
    fun `directory station aside from the line is not a candidate`() {
        // 0.001 широты = 111 м в стороне от линии (допуск 150 м у прямых, 100 м у реальной линии).
        val aside = mapOf("справочник" to (60.0 to 30.002), "сбоку" to (60.0015 to 30.002))
        val asideNames = mapOf("справочник" to "Справочная", "сбоку" to "Сбоку")
        // «Сбоку» (167 м от прямой плеча > 150) не кандидат, хотя стоим прямо на ней; берётся «Справочная» (167 м).
        assertEquals("Справочная", ServicePhaseStationPicker.pickForStop(phase, emptyList(), 60.0015, 30.002, aside, asideNames))
        val line = listOf(60.0 to 29.0, 60.0 to 30.5)
        val near = mapOf("рядом" to (60.0008 to 30.002)) // 89 м от линии
        val far = mapOf("далеко" to (60.0012 to 30.002)) // 133 м от линии
        assertEquals("Рядом", ServicePhaseStationPicker.pickForStop(phase, emptyList(), 60.0, 30.002, near, mapOf("рядом" to "Рядом"), railLine = line))
        assertEquals("Узел-1", ServicePhaseStationPicker.pickForStop(phase, emptyList(), 60.0, 30.002, far, mapOf("далеко" to "Далеко"), railLine = line))
        // Без линии допуск 150 м: 133 м - кандидат.
        assertEquals("Далеко", ServicePhaseStationPicker.pickForStop(phase, emptyList(), 60.0, 30.002, far, mapOf("далеко" to "Далеко")))
    }

    @Test
    fun `real line bends - directory station on the line but off the straight segment`() {
        // Линия уходит на север: станция справочника на изгибе (60.01, 30.002) на линии, но далеко от прямой плеча.
        val line = listOf(60.0 to 29.0, 60.0 to 30.0, 60.01 to 30.0, 60.01 to 30.5)
        val p = mapOf("изгиб" to (60.01 to 30.002))
        val n = mapOf("изгиб" to "Изгиб")
        assertEquals("Изгиб", ServicePhaseStationPicker.pickForStop(phase, listOf("Начало", "Узел-1"), 60.01, 30.002, p, n, railLine = line))
    }

    @Test
    fun `no phase - nearest directory station within a kilometre`() {
        assertEquals("Справочная", pick(null, emptyList(), 30.003))
        assertNull(pick(null, emptyList(), 30.5))
    }

    @Test
    fun `phase has no stations in radius - falls back to directory`() {
        val bare = phase.copy(intermediateStations = emptyList())
        assertEquals("Справочная", pick(bare, listOf("Начало"), 30.003))
    }

    @Test
    fun `no candidates - empty`() {
        assertNull(pick(phase, listOf("Начало"), 30.2))
    }
}
