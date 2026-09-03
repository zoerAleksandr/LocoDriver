@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.route.viewmodel

import com.z_company.domain.entities.DateSetTariffRate
import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Test
import kotlin.test.assertEquals

class ReserveSegmentCalculationTest {
    private val hour = 3_600_000L
    private val moscow = TimeZone.of("Europe/Moscow")

    private fun instant(day: Int, hour: Int): Long =
        LocalDateTime(2025, 1, day, hour, 0).toInstant(moscow).toEpochMilliseconds()

    private fun helper(number: String): SalaryCalculationHelper {
        val start = instant(10, 22)
        val end = instant(11, 3)
        val route = Route(
            basicData = BasicData(
                timeStartWork = start,
                timeEndWork = end,
                timeStartBreak = instant(10, 23),
                timeEndBreak = instant(11, 0),
            ),
            trains = mutableListOf(Train(
                number = number,
                stations = mutableListOf(
                    Station(timeDeparture = start),
                    Station(timeArrival = end),
                ),
            )),
            passengers = mutableListOf(Passenger(
                timeDeparture = instant(11, 0),
                timeArrival = instant(11, 1),
            )),
        )
        return SalaryCalculationHelper(
            userSettings = UserSettings(selectMonthOfYear = MonthOfYear(
                year = 2025,
                month = 0,
                tariffRate = 200.0,
                dateSetTariffRate = DateSetTariffRate(dateNewRate = 11, oldRate = 100.0),
                days = (1..31).map { Day(it, TagForDay.WORKING_DAY) },
            )),
            salarySetting = SalarySetting(),
            allRoutes = listOf(route),
        )
    }

    @Test
    fun reserveUsesActualIntervalExcludingBreakPassengerAndApplyingTariffs() = runTest {
        val calculation = helper(number = "4001")

        // Четыре оплачиваемых часа состоят из 3 ч резервом и 1 ч пассажиром.
        // Они оплачиваются отдельными строками и не должны повторяться в 004L.
        assertEquals(4 * hour, calculation.getTotalWorkTime().first())
        assertEquals(3 * hour, calculation.getSingleLocomotiveTimeFlow().first())
        assertEquals(500.0, calculation.getMoneyAtSingleLocomotiveFlow().first(), 0.001)
        assertEquals(1 * hour, calculation.getPassengerTimeFlow().first())
        assertEquals(0L, calculation.getWorkTimeAtTariffFlow().first())
        assertEquals(0.0, calculation.getMoneyAtWorkTimeAtTariff().first(), 0.001)
    }

    @Test
    fun numberOutsideReserveRangesDoesNotQualify() = runTest {
        val calculation = helper(number = "4000")

        assertEquals(0L, calculation.getSingleLocomotiveTimeFlow().first())
        assertEquals(0.0, calculation.getMoneyAtSingleLocomotiveFlow().first(), 0.001)
    }
}
