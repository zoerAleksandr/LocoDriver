package com.z_company.route.util

import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class TurnaroundRestTest {

    private val hour = 3_600_000L

    private fun leg(
        start: Long,
        end: Long,
        restAtPO: Boolean = false,
        arrivalStation: String? = null,
        departureStation: String? = null,
    ): Route {
        val stations = mutableListOf<Station>()
        if (departureStation != null || arrivalStation != null) {
            stations.add(Station(stationName = departureStation, timeDeparture = start))
            stations.add(Station(stationName = arrivalStation, timeArrival = end))
        }
        return Route(
            basicData = BasicData(
                timeStartWork = start,
                timeEndWork = end,
                restPointOfTurnover = restAtPO,
            ),
            trains = if (stations.isEmpty()) mutableListOf()
            else mutableListOf(Train(stations = stations)),
        )
    }

    @Test
    fun validPairReturnsConnector() {
        // туда 08:00–14:00 (флаг отдыха), обратно через 6 ч — явка 20:00.
        val outbound = leg(start = 8 * hour, end = 14 * hour, restAtPO = true, arrivalStation = "Бабаево")
        val ret = leg(start = 20 * hour, end = 26 * hour)
        val rest = turnaroundRestBetween(outbound, ret)
        assertEquals(6 * hour, rest?.durationMs)
        assertEquals("Бабаево", rest?.station)
    }

    @Test
    fun orderIndependentGivesSameResult() {
        val outbound = leg(start = 8 * hour, end = 14 * hour, restAtPO = true)
        val ret = leg(start = 20 * hour, end = 26 * hour)
        // Порядок аргументов не важен (список бывает и по возрастанию, и по убыванию).
        assertEquals(6 * hour, turnaroundRestBetween(ret, outbound)?.durationMs)
    }

    @Test
    fun noFlagReturnsNull() {
        val outbound = leg(start = 8 * hour, end = 14 * hour, restAtPO = false)
        val ret = leg(start = 20 * hour, end = 26 * hour)
        assertNull(turnaroundRestBetween(outbound, ret))
    }

    @Test
    fun longGapStillShows() {
        // Потолка нет — длительность отдыха определяет пользователь. Даже большой
        // разрыв с выставленным флагом показывает коннектор.
        val outbound = leg(start = 8 * hour, end = 14 * hour, restAtPO = true)
        val ret = leg(start = 14 * hour + 40 * hour, end = 14 * hour + 46 * hour) // +40 ч
        assertEquals(40 * hour, turnaroundRestBetween(outbound, ret)?.durationMs)
    }

    @Test
    fun shortGapStillShows() {
        // Короткий отдых тоже показываем — доверяем флагу пользователя.
        val outbound = leg(start = 8 * hour, end = 20 * hour, restAtPO = true)
        val ret = leg(start = 20 * hour + 15 * 60 * 1000, end = 26 * hour)
        assertEquals(15 * 60 * 1000L, turnaroundRestBetween(outbound, ret)?.durationMs)
    }

    @Test
    fun nonPositiveGapReturnsNull() {
        // Явка «обратно» не позже сдачи «туда» → это не отдых после оборота.
        val outbound = leg(start = 8 * hour, end = 20 * hour, restAtPO = true)
        val ret = leg(start = 20 * hour, end = 26 * hour) // разрыв 0
        assertNull(turnaroundRestBetween(outbound, ret))
    }

    @Test
    fun nightCrossingIsPlainMillisMath() {
        // Оборот через полночь: сдача 22:00, явка на следующий день 06:00 → 8 ч.
        val outbound = leg(start = 16 * hour, end = 22 * hour, restAtPO = true)
        val ret = leg(start = 30 * hour, end = 36 * hour) // 30ч = 06:00 следующего дня
        assertEquals(8 * hour, turnaroundRestBetween(outbound, ret)?.durationMs)
    }

    @Test
    fun nullTimesReturnNull() {
        val outbound = Route(basicData = BasicData(timeStartWork = null, restPointOfTurnover = true))
        val ret = leg(start = 20 * hour, end = 26 * hour)
        assertNull(turnaroundRestBetween(outbound, ret))
    }

    @Test
    fun stationFallsBackToReturnDeparture() {
        // У «туда» нет станций — берём станцию отправления «обратно».
        val outbound = leg(start = 8 * hour, end = 14 * hour, restAtPO = true)
        val ret = leg(start = 20 * hour, end = 26 * hour, departureStation = "Лужская")
        assertEquals("Лужская", turnaroundRestBetween(outbound, ret)?.station)
    }
}
