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
    fun `nearest station ahead of the last one in the train`() {
        assertEquals("Сиверская", ServicePhaseStationPicker.nearestAhead(phase, listOf("Гатчина", "Суйда"), at(30.199)))
    }

    @Test
    fun `station already passed is not suggested even if closer`() {
        // стоим ещё у Суйды, она уже в поезде — впереди в радиусе ничего нет
        assertNull(ServicePhaseStationPicker.nearestAhead(phase, listOf("Гатчина", "Суйда"), at(30.101)))
    }

    @Test
    fun `nothing near means empty`() {
        assertNull(ServicePhaseStationPicker.nearestAhead(phase, listOf("Гатчина"), at(30.3)))
    }

    @Test
    fun `train names are matched ignoring case and yo`() {
        assertEquals("Луга", ServicePhaseStationPicker.nearestAhead(phase, listOf("гатчина", "толмачево"), at(30.49)))
    }

    @Test
    fun `standing still - nearest station of the phase, even if already passed`() {
        val here = at(30.1005) // у Суйды, она уже в поезде, но стоим тут — дописываем её
        assertEquals("Суйда", ServicePhaseStationPicker.nameForNewStation(phase, listOf("Гатчина", "Сиверская"), here, previousFix = at(30.1))
        )
        // в движении, но уже на станции (≤ 500 м) — её, даже если по списку она позади
        assertEquals("Суйда", ServicePhaseStationPicker.nameForNewStation(phase, listOf("Гатчина", "Сиверская"), here, previousFix = at(30.0)))
        // в движении в 1,5 км от пройденной Суйды — впереди в радиусе ничего нет
        assertNull(ServicePhaseStationPicker.nameForNewStation(phase, listOf("Гатчина", "Суйда"), at(30.126), previousFix = at(30.0)))
    }

    @Test
    fun `without phase - nearest known station from all phases`() {
        val points = ServicePhaseStationPicker.knownPoints(listOf(phase))
        val names = ServicePhaseStationPicker.knownNames(listOf(phase))
        assertEquals("Толмачёво", ServicePhaseStationPicker.nearestKnown(points, names, at(30.399)))
        assertNull(ServicePhaseStationPicker.nearestKnown(points, names, at(30.3)))
    }

    @Test
    fun `same as previous station is not repeated`() {
        assertNull(ServicePhaseStationPicker.nameForNewStation(phase, listOf("Гатчина", "Сиверская", null), at(30.199), null))
    }

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
}
