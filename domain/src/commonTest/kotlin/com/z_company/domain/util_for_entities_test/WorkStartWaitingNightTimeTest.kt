@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.domain.util_for_entities_test

import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.getNightTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkStartWaitingNightTime
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.util.AccrualCondition
import com.z_company.domain.util.NightWindow
import com.z_company.domain.util.TimeCalculationContext
import com.z_company.domain.util.buildSalarySegments
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Часы ночного времени и деньги по ночной надбавке считаются по одним и тем же минутам,
 * включая зазор «прибытие → явка» (ожидание 018M), если он попадает в ночное окно 22:00–06:00.
 */
class WorkStartWaitingNightTimeTest {
    private val minute = 60_000L
    private val hour = 3_600_000L
    private val moscow = TimeZone.of("GMT+3")

    private fun at(day: Int, hour: Int, minute: Int = 0): Long =
        LocalDateTime(2025, 1, day, hour, minute).toInstant(moscow).toEpochMilliseconds()

    // Прибытие 21:50, смещение 20 мин → явка 22:10, смена 1 час. Ночь с 22:00: из зазора 10 мин ночных.
    private fun route(): Route {
        val arrival = at(10, 21, 50)
        val start = arrival + 20 * minute
        return Route(
            basicData = BasicData(timeStartWork = start, timeEndWork = start + hour),
            passengers = mutableListOf(
                Passenger(
                    timeDeparture = arrival - hour,
                    timeArrival = arrival,
                    isWorkStartByArrival = true,
                    workStartOffsetMinutes = 20,
                )
            ),
        )
    }

    private val settings = UserSettings(
        selectMonthOfYear = MonthOfYear(year = 2025, month = 0, tariffRate = 100.0, days = emptyList()),
        timeZone = 0L,
    )

    @Test
    fun waitingGapNightMinutesAreCounted() {
        assertEquals(10 * minute, route().getWorkStartWaitingNightTime(22, 0, 6, 0, 0L))
    }

    @Test
    fun gapOfOneMinuteHasNoNightWaiting() {
        val arrival = at(10, 21, 59)
        val start = arrival + minute
        val oneMinuteRoute = Route(
            basicData = BasicData(timeStartWork = start, timeEndWork = start + hour),
            passengers = mutableListOf(
                Passenger(timeDeparture = arrival - hour, timeArrival = arrival,
                    isWorkStartByArrival = true, workStartOffsetMinutes = 1)
            ),
        )
        assertEquals(0L, oneMinuteRoute.getWorkStartWaitingNightTime(22, 0, 6, 0, 0L))
    }

    @Test
    fun monthlyNightHoursMatchNightSegmentsUsedForMoney() = runTest {
        val route = route()
        val nightHours = listOf(route).getNightTime(settings)

        val context = TimeCalculationContext.from(settings)
        val segments = route.buildSalarySegments(
            monthOfYear = settings.selectMonthOfYear,
            context = context,
            initialTariffRatePerHour = 100.0,
            nightWindow = NightWindow(22, 0, 6, 0, 0L),
        )
        val nightSegmentsMillis = segments.filter { AccrualCondition.NIGHT in it.conditions }
            .sumOf { it.interval.durationMillis }

        // Смена 22:10–23:10 целиком в ночи (60 мин) + 10 мин зазора после 22:00.
        assertEquals(70 * minute, nightSegmentsMillis)
        assertEquals(nightSegmentsMillis, nightHours)
    }
}
