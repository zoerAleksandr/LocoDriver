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
        val train = Train(
            stations = mutableListOf(
                Station(timeDeparture = 1000L),
                Station(timeArrival = 5000L)
            )
        )
        assertEquals(4000L, train.getTravelTime())
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
        val train = Train(
            stations = mutableListOf(
                Station(timeDeparture = 1000L, timeArrival = null),
                Station(timeDeparture = 2000L, timeArrival = 3000L),
                Station(timeDeparture = null, timeArrival = 7000L)
            )
        )
        assertEquals(6000L, train.getTravelTime())
    }
}
