package com.z_company.domain.util_for_entities_test

import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTimeOutsideWork
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.util.TimeCalculationContext
import kotlinx.datetime.TimeZone
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

    @Test
    fun listCalculationMergesOverlapsAndExcludesPartInsideWork() {
        val route = Route(
            basicData = BasicData(timeStartWork = 12 * hour, timeEndWork = 20 * hour),
            passengers = mutableListOf(
                Passenger(
                    timeDeparture = 9 * hour,
                    timeArrival = 15 * hour,
                    isWorkStartByArrival = true,
                ),
                Passenger(
                    timeDeparture = 10 * hour,
                    timeArrival = 13 * hour,
                    isWorkStartByArrival = true,
                ),
            ),
        )
        val month = MonthOfYear(
            year = 1970,
            month = 0,
            days = (1..31).map { Day(it, TagForDay.WORKING_DAY) },
        )
        val utc = TimeZone.UTC

        assertEquals(
            3 * hour,
            listOf(route).getPassengerTimeOutsideWork(
                month,
                TimeCalculationContext(localTZ = utc, crossMonthTZ = utc),
            ),
        )
    }

    @Test
    fun workStartGapOverOneMinuteCountsAsWorkedTime() {
        val minute = 60_000L
        val route = Route(
            basicData = BasicData(
                timeStartWork = 15 * hour + 5 * minute,
                timeEndWork = 20 * hour,
            ),
            passengers = mutableListOf(
                Passenger(
                    timeDeparture = 10 * hour,
                    timeArrival = 15 * hour,
                    isWorkStartByArrival = true,
                    workStartOffsetMinutes = 5,
                )
            )
        )

        assertEquals(5 * hour, route.getPassengerTimeOutsideWork())
        // Работа 4:55 + проезд 5:00 + ожидание 0:05.
        assertEquals(10 * hour, route.getWorkTime())
    }

    @Test
    fun workStartGapOfOneMinuteIsNotWorkedTime() {
        val minute = 60_000L
        val route = Route(
            basicData = BasicData(
                timeStartWork = 15 * hour + minute,
                timeEndWork = 20 * hour,
            ),
            passengers = mutableListOf(
                Passenger(
                    timeDeparture = 10 * hour,
                    timeArrival = 15 * hour,
                    isWorkStartByArrival = true,
                    workStartOffsetMinutes = 1,
                )
            )
        )

        // Работа 4:59 + проезд 5:00; минута зазора не считается.
        assertEquals(10 * hour - minute, route.getWorkTime())
    }
}
