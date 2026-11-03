package com.z_company.domain.entities_test

import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.entities.setting.ServicePhasePoint
import com.z_company.domain.entities.setting.ServicePhaseStation
import com.z_company.domain.entities.setting.ServicePhaseStationKind
import com.z_company.domain.entities.setting.ServicePhaseStationSource
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ServicePhaseStationsTest {

    private val gatchina = ServicePhasePoint(59.57, 30.12, osmId = 1, esr = "030201")
    private val luga = ServicePhasePoint(58.73, 29.85, osmId = 2, esr = "030400")

    private val phase = ServicePhase(
        departureStation = "Гатчина",
        arrivalStation = "Луга",
        distance = 96,
        linearMileageRate = 1.5,
        intermediateStations = listOf(
            ServicePhaseStation(name = "Сиверская", kmFromStart = 25.1, latitude = 59.35, longitude = 30.07),
            ServicePhaseStation(name = "Кемка", kind = ServicePhaseStationKind.HALT),
            ServicePhaseStation(name = "Толмачёво", kmFromStart = 68.3),
        ),
        departurePoint = gatchina,
        arrivalPoint = luga,
    )

    @Test
    fun `reversed flips order, endpoints and km`() {
        val back = phase.reversed()

        assertEquals("Луга", back.departureStation)
        assertEquals("Гатчина", back.arrivalStation)
        assertEquals(luga, back.departurePoint)
        assertEquals(gatchina, back.arrivalPoint)
        assertEquals(listOf("Толмачёво", "Кемка", "Сиверская"), back.intermediateStations.map { it.name })
        assertEquals(27.7, back.intermediateStations[0].kmFromStart!!, 1e-9)
        assertNull(back.intermediateStations[1].kmFromStart)
        assertEquals(70.9, back.intermediateStations[2].kmFromStart!!, 1e-9)
        assertEquals(1.5, back.linearMileageRate)
        assertNotEquals(phase.id, back.id)
        assertTrue(back.intermediateStations.none { st -> phase.intermediateStations.any { it.id == st.id } })
    }

    @Test
    fun `train copy has no stations`() {
        val copy = phase.forTrain()

        assertEquals(phase.id, copy.id)
        assertEquals(96, copy.distance)
        assertTrue(copy.intermediateStations.isEmpty())
        assertNull(copy.departurePoint)
        assertNull(copy.arrivalPoint)
    }

    @Test
    fun `json uses lowercase kind and source, unknown values fall back`() {
        val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
        val station = ServicePhaseStation(name = "Кемка", kind = ServicePhaseStationKind.HALT, source = ServicePhaseStationSource.OSM)
        val text = json.encodeToString(ServicePhaseStation.serializer(), station)
        assertTrue("\"kind\":\"halt\"" in text)
        assertTrue("\"source\":\"osm\"" in text)

        val decoded = json.decodeFromString(
            ServicePhaseStation.serializer(),
            """{"name":"Х","kind":"junction","source":"gps","futureField":1}""",
        )
        assertEquals(ServicePhaseStationKind.STATION, decoded.kind)
        assertEquals(ServicePhaseStationSource.MANUAL, decoded.source)
    }

    @Test
    fun `old phase json without stations decodes`() {
        val json = Json { ignoreUnknownKeys = true }
        val decoded = json.decodeFromString(
            ServicePhase.serializer(),
            """{"id":"x","departureStation":"А","arrivalStation":"Б","distance":10}""",
        )
        assertTrue(decoded.intermediateStations.isEmpty())
        assertNull(decoded.departurePoint)
    }
}
