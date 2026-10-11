package com.z_company.domain.util_test

import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.entities.setting.ServicePhasePoint
import com.z_company.domain.entities.setting.ServicePhaseStation
import com.z_company.domain.util.GeoFix
import com.z_company.domain.util.ServicePhaseOrder
import com.z_company.domain.util.ServicePhaseStationPicker
import com.z_company.domain.util.StationSuggestionOrder
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ServicePhaseStationPickerTest {

    // Линия по 60° с.ш.: 0.1° долготы ≈ 5.6 км
    private val phase = ServicePhase(
        departureStation = "Гатчина",
        arrivalStation = "Луга",
        distance = 30,
        departurePoint = ServicePhasePoint(60.0, 30.0),
        arrivalPoint = ServicePhasePoint(60.0, 30.5),
        intermediateStations = listOf(
            ServicePhaseStation(name = "Суйда", latitude = 60.0, longitude = 30.1),
            ServicePhaseStation(name = "Сиверская", latitude = 60.0, longitude = 30.2),
            ServicePhaseStation(name = "Кемка"), // без координат
            ServicePhaseStation(name = "Толмачёво", latitude = 60.0, longitude = 30.4),
        ),
    )

    private fun at(lon: Double) = GeoFix(60.0, lon)

    @Test
    fun `suggestions - nearest first, then by usage`() {
        val names = listOf("Мга", "Луга", "Сиверская", "Суйда")
        val usage = StationSuggestionOrder.usage(sequenceOf("Мга", "Суйда", "Суйда", "суйда", "Мга", null, ""))
        val points = ServicePhaseStationPicker.knownPoints(listOf(phase))
        assertEquals(listOf("Сиверская", "Суйда", "Мга", "Луга"), StationSuggestionOrder.sort(names, usage, points, at(30.201)))
        assertEquals(listOf("Суйда", "Мга", "Луга", "Сиверская"), StationSuggestionOrder.sort(names, usage))
    }

    @Test
    fun `phases - last used from here, other from here, then by usage`() {
        val a = ServicePhase(id = "a", departureStation = "Луга", arrivalStation = "Гатчина", distance = 1)
        val b = ServicePhase(id = "b", departureStation = "Луга", arrivalStation = "Батецкая", distance = 1)
        val c = ServicePhase(id = "c", departureStation = "Гатчина", arrivalStation = "Луга", distance = 1)
        val d = ServicePhase(id = "d", departureStation = "Мга", arrivalStation = "Луга", distance = 1)
        val sorted = ServicePhaseOrder.sort(
            phases = listOf(a, b, c, d),
            currentStation = "луга",
            usage = mapOf("a" to 10, "b" to 1, "d" to 5),
            lastUsedAt = mapOf("a" to 100L, "b" to 200L),
        )
        assertEquals(listOf("b", "a", "d", "c"), sorted.map { it.id })
    }

    @Test
    fun `current station - typed first station wins when phases depart from it`() {
        val a = ServicePhase(id = "a", departureStation = "Будогощь", arrivalStation = "Кириши", distance = 1)
        assertEquals("будогощь", ServicePhaseOrder.currentStation(listOf(a), "будогощь", null))
    }

    @Test
    fun `current station - gps nearest departure, not nearest arbitrary station`() {
        val a = ServicePhase(
            id = "a", departureStation = "Будогощь", arrivalStation = "Кириши", distance = 1,
            departurePoint = com.z_company.domain.entities.setting.ServicePhasePoint(59.4, 32.2),
        )
        val b = ServicePhase(
            id = "b", departureStation = "Мга", arrivalStation = "Будогощь", distance = 1,
            departurePoint = com.z_company.domain.entities.setting.ServicePhasePoint(59.7, 31.0),
        )
        assertEquals("Будогощь", ServicePhaseOrder.currentStation(listOf(b, a), null, GeoFix(59.401, 32.2)))
        assertEquals(null, ServicePhaseOrder.currentStation(listOf(b, a), null, GeoFix(50.0, 20.0)))
        // введённая станция без плеч от неё - уступает GPS
        assertEquals("Будогощь", ServicePhaseOrder.currentStation(listOf(b, a), "Тосно", GeoFix(59.401, 32.2)))
        assertEquals("Тосно", ServicePhaseOrder.currentStation(listOf(b, a), "Тосно", null))
    }

    @Test
    fun `phases from here go first when current station found by gps`() {
        val a = ServicePhase(id = "a", departureStation = "Мга", arrivalStation = "Луга", distance = 1)
        val b = ServicePhase(id = "b", departureStation = "Будогощь", arrivalStation = "Кириши", distance = 1)
        val c = ServicePhase(id = "c", departureStation = "Луга", arrivalStation = "Мга", distance = 1)
        val sorted = ServicePhaseOrder.sort(listOf(a, c, b), "Будогощь", mapOf("a" to 50, "c" to 9), emptyMap())
        assertEquals(listOf("b", "a", "c"), sorted.map { it.id })
    }
}
