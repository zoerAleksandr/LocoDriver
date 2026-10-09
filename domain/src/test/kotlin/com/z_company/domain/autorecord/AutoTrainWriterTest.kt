package com.z_company.domain.autorecord

import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.setting.ServicePhase
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AutoTrainWriterTest {

    private val min = 60_000L
    private val base = 1_700_000_000_000L - (1_700_000_000_000L % min)

    private fun ev(type: StopEventType, atMin: Int) =
        StopEvent(type, base + atMin * min + 17_000L, 59.0, 30.0)

    private fun train(vararg st: Station, phase: ServicePhase? = null) =
        Train(stations = st.toMutableList(), servicePhase = phase)

    @Test
    fun `empty train accepts departure only and creates a station`() {
        assertNull(AutoTrainWriter.apply(train(), ev(StopEventType.ARRIVAL, 0), "Гатчина"))
        val t = AutoTrainWriter.apply(train(), ev(StopEventType.DEPARTURE, 0), "Гатчина")
        assertNotNull(t)
        assertEquals(1, t.stations.size)
        assertEquals(base, t.stations[0].timeDeparture)
        assertEquals("Гатчина", t.stations[0].stationName)
    }

    @Test
    fun `arrival after departure creates next station with empty name outside stations`() {
        val t = AutoTrainWriter.apply(
            train(Station(stationName = "А", timeDeparture = base)),
            ev(StopEventType.ARRIVAL, 10), null,
        )
        assertNotNull(t)
        assertEquals(2, t.stations.size)
        assertEquals(base + 10 * min, t.stations[1].timeArrival)
        assertNull(t.stations[1].stationName)
    }

    @Test
    fun `departure after arrival fills the same station and keeps manual name`() {
        val t = AutoTrainWriter.apply(
            train(Station(stationName = "Моя", timeArrival = base)),
            ev(StopEventType.DEPARTURE, 5), "Другая",
        )
        assertNotNull(t)
        assertEquals("Моя", t.stations[0].stationName)
        assertEquals(base + 5 * min, t.stations[0].timeDeparture)
    }

    @Test
    fun `existing next station gets arrival and empty name is filled`() {
        val t = AutoTrainWriter.apply(
            train(Station(timeDeparture = base), Station()),
            ev(StopEventType.ARRIVAL, 3), "Б",
        )
        assertNotNull(t)
        assertEquals(2, t.stations.size)
        assertEquals("Б", t.stations[1].stationName)
        assertEquals(base + 3 * min, t.stations[1].timeArrival)
    }

    @Test
    fun `event not matching the next GO step is skipped`() {
        val arrived = train(Station(timeArrival = base))
        assertNull(AutoTrainWriter.apply(arrived, ev(StopEventType.ARRIVAL, 5), null))
        val departed = train(Station(timeDeparture = base))
        assertNull(AutoTrainWriter.apply(departed, ev(StopEventType.DEPARTURE, 5), null))
    }

    @Test
    fun `event earlier than last filled time is skipped`() {
        val t = train(Station(timeDeparture = base + 10 * min))
        assertNull(AutoTrainWriter.apply(t, ev(StopEventType.ARRIVAL, 5), null))
    }

    @Test
    fun `train without times takes departure on the first station`() {
        val t = AutoTrainWriter.apply(train(Station(stationName = "А")), ev(StopEventType.DEPARTURE, 1), null)
        assertNotNull(t)
        assertEquals(base + min, t.stations[0].timeDeparture)
    }

    @Test
    fun `with service phase the new station is inserted before the phase arrival`() {
        val phase = ServicePhase(departureStation = "А", arrivalStation = "Я", distance = 10, linearMileageRate = 1.0)
        val t = AutoTrainWriter.apply(
            train(Station(stationName = "А", timeDeparture = base), Station(stationName = "Я"), phase = phase),
            ev(StopEventType.ARRIVAL, 4), "Б",
        )
        assertNotNull(t)
        assertEquals(listOf("А", "Б", "Я"), t.stations.map { it.stationName })
        assertEquals(base + 4 * min, t.stations[1].timeArrival)
        assertNull(t.stations[2].timeArrival)
    }

    @Test
    fun `nearest name within 500 m, empty outside`() {
        val points = mapOf("a" to (59.5700 to 30.1200), "b" to (59.5750 to 30.1200))
        val names = mapOf("a" to "Гатчина", "b" to "Рядом")
        assertEquals("Гатчина", AutoStationLocator.nearestName(points, names, 59.5703, 30.1200))
        assertNull(AutoStationLocator.nearestName(points, names, 59.60, 30.20))
    }

    @Test
    fun `arrival at phase end station fills it instead of new intermediate`() {
        val phase = ServicePhase(departureStation = "Лужская", arrivalStation = "Веймарн", distance = 50)
        val t = AutoTrainWriter.apply(
            train(
                Station(stationName = "Лужская", timeDeparture = base),
                Station(stationName = "Котлы-2", timeArrival = base + 10 * min, timeDeparture = base + 11 * min),
                Station(stationName = "Веймарн"),
                phase = phase,
            ),
            ev(StopEventType.ARRIVAL, 30), "веймарн",
        )
        assertNotNull(t)
        assertEquals(3, t.stations.size)
        assertEquals(base + 30 * min, t.stations[2].timeArrival)
        assertEquals("Веймарн", t.stations[2].stationName)
    }

    @Test
    fun `phase end match ignores yo and hyphen`() {
        val phase = ServicePhase(departureStation = "А", arrivalStation = "Тёмный-Лес", distance = 5)
        val t = AutoTrainWriter.apply(
            train(
                Station(stationName = "А", timeDeparture = base),
                Station(stationName = "Тёмный-Лес"),
                phase = phase,
            ),
            ev(StopEventType.ARRIVAL, 5), "Темный лес",
        )
        assertNotNull(t)
        assertEquals(2, t.stations.size)
        assertEquals(base + 5 * min, t.stations[1].timeArrival)
    }

    @Test
    fun `phase and different station name still inserts intermediate`() {
        val phase = ServicePhase(departureStation = "А", arrivalStation = "Б", distance = 5)
        val t = AutoTrainWriter.apply(
            train(
                Station(stationName = "А", timeDeparture = base),
                Station(stationName = "Б"),
                phase = phase,
            ),
            ev(StopEventType.ARRIVAL, 5), "В",
        )
        assertNotNull(t)
        assertEquals(3, t.stations.size)
        assertEquals("В", t.stations[1].stationName)
        assertNull(t.stations[2].timeArrival)
    }
}
