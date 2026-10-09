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
    fun `dense node with phase picks by order not by distance`() {
        // Стоим ближе к «Узел-2», но в поезде записано только «Начало» - первая по порядку в радиусе «Узел-1».
        assertEquals("Узел-1", pick(phase, listOf("Начало"), 30.0045))
        // «Узел-1» уже записана - допускается повтор («уже на станции»): не ранее неё.
        assertEquals("Узел-1", pick(phase, listOf("Начало", "Узел-1"), 30.0045))
        // Записана «Узел-2» - «Узел-1» позади, не подходит.
        assertEquals("Узел-2", pick(phase, listOf("Начало", "Узел-1", "Узел-2"), 30.0))
    }

    @Test
    fun `nothing written - first in the leg order within radius`() {
        assertEquals("Узел-1", pick(phase, emptyList(), 30.0045))
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
