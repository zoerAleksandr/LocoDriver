package com.z_company.domain.util_for_entities_test

import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.UtilsForEntities.getTravelTime
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetTravelTimeTest {

    @Test
    fun twoStationsReturnsTimeDifference() {
        // getTravelTime() округляет до минуты (floorToMinute) — реальные явки всегда
        // в минутах. 1:30 → floor 1:00, 5:45 → floor 5:00, разница = 4 минуты.
        val train = Train(
            stations = mutableListOf(
                Station(timeDeparture = 90_000L),   // 00:01:30
                Station(timeArrival = 345_000L)     // 00:05:45
            )
        )
        assertEquals(4 * 60_000L, train.getTravelTime())
    }

    @Test
    fun oneStationReturnsNull() {
        val train = Train(
            stations = mutableListOf(
                Station(timeDeparture = 1000L, timeArrival = 5000L)
            )
        )
        assertNull(train.getTravelTime())
    }

    @Test
    fun emptyStationsReturnsNull() {
        val train = Train(stations = mutableListOf())
        assertNull(train.getTravelTime())
    }

    @Test
    fun noDepartureReturnsNull() {
        val train = Train(
            stations = mutableListOf(
                Station(timeDeparture = null),
                Station(timeArrival = 5000L)
            )
        )
        assertNull(train.getTravelTime())
    }

    @Test
    fun noArrivalReturnsNull() {
        val train = Train(
            stations = mutableListOf(
                Station(timeDeparture = 1000L),
                Station(timeArrival = null)
            )
        )
        assertNull(train.getTravelTime())
    }

    @Test
    fun arrivalBeforeDepartureReturnsNull() {
        val train = Train(
            stations = mutableListOf(
                Station(timeDeparture = 5000L),
                Station(timeArrival = 1000L)
            )
        )
        assertNull(train.getTravelTime())
    }

    @Test
    fun multipleStationsUsesFirstDepartureAndLastArrival() {
        // Первое отправление 1:30 → floor 1:00, последнее прибытие 7:45 → floor 7:00,
        // разница = 6 минут. Средняя станция не влияет на результат.
        val train = Train(
            stations = mutableListOf(
                Station(timeDeparture = 90_000L, timeArrival = null),      // 00:01:30
                Station(timeDeparture = 120_000L, timeArrival = 180_000L), // 00:02:00–00:03:00
                Station(timeDeparture = null, timeArrival = 465_000L)      // 00:07:45
            )
        )
        assertEquals(6 * 60_000L, train.getTravelTime())
    }
}
