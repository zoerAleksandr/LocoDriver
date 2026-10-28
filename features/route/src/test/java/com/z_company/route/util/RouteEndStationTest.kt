package com.z_company.route.util

import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class RouteEndStationTest {

    private fun train(from: String, to: String, dep: Long? = null, arr: Long? = null) = Train(
        stations = mutableListOf(
            Station(stationName = from, timeDeparture = dep),
            Station(stationName = to, timeArrival = arr),
        )
    )

    private fun route(trains: List<Train>, passengers: List<Passenger> = emptyList()) =
        Route(trains = trains.toMutableList(), passengers = passengers.toMutableList())

    @Test
    fun noData() = assertNull(routeEndStation(route(emptyList())))

    @Test
    fun laterTimedWins() {
        val r = route(listOf(train("А", "Б", 1, 300), train("В", "Г", 2, 200)))
        assertEquals("Б", routeEndStation(r))
    }

    @Test
    fun untimedTrainIsLast() {
        val r = route(listOf(train("А", "Б", 1, 900), train("В", "Г")))
        assertEquals("Г", routeEndStation(r))
    }

    @Test
    fun timedPassengerAfterTrain() {
        val r = route(
            listOf(train("А", "Б", 1, 300)),
            listOf(Passenger(stationArrival = "Д", timeArrival = 500)),
        )
        assertEquals("Д", routeEndStation(r))
    }

    @Test
    fun untimedPassengerAfterAllTrains() {
        val r = route(
            listOf(train("А", "Б", 1, 300), train("В", "Г")),
            listOf(Passenger(stationArrival = "Д")),
        )
        assertEquals("Д", routeEndStation(r))
    }
}
