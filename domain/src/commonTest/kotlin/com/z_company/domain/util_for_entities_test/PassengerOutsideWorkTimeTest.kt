package com.z_company.domain.util_for_entities_test

import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTimeOutsideWork
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import kotlin.test.Test
import kotlin.test.assertEquals

class PassengerOutsideWorkTimeTest {

    private val hour = 3_600_000L

    @Test
    fun passengerInsideWorkIntervalIsNotAddedTwice() {
        val route = Route(
            basicData = BasicData(timeStartWork = 8 * hour, timeEndWork = 20 * hour),
            passengers = mutableListOf(
                Passenger(
                    timeDeparture = 10 * hour,
                    timeArrival = 15 * hour,
                    isWorkStartByArrival = true
                )
            )
        )

        assertEquals(0L, route.getPassengerTimeOutsideWork())
        assertEquals(12 * hour, route.getWorkTime())
    }

    @Test
    fun passengerBeforeArrivalBasedWorkStartIsAdded() {
        val route = Route(
            basicData = BasicData(timeStartWork = 15 * hour, timeEndWork = 20 * hour),
            passengers = mutableListOf(
                Passenger(
                    timeDeparture = 10 * hour,
                    timeArrival = 15 * hour,
                    isWorkStartByArrival = true
                )
            )
        )

        assertEquals(5 * hour, route.getPassengerTimeOutsideWork())
        assertEquals(10 * hour, route.getWorkTime())
    }

    @Test
    fun onlyPassengerPartOutsideWorkIntervalIsAdded() {
        val route = Route(
            basicData = BasicData(timeStartWork = 12 * hour, timeEndWork = 20 * hour),
            passengers = mutableListOf(
                Passenger(
                    timeDeparture = 10 * hour,
                    timeArrival = 15 * hour,
                    isWorkStartByArrival = true
                )
            )
        )

        assertEquals(2 * hour, route.getPassengerTimeOutsideWork())
        assertEquals(10 * hour, route.getWorkTime())
    }
}
