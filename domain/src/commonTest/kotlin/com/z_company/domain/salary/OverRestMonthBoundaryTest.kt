@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.domain.salary

import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.OverRestRoutes.adjacentRoutesOfMonth
import com.z_company.domain.entities.route.OverRestRoutes.overRestPayment
import com.z_company.domain.entities.route.OverRestRoutes.previousRouteFor
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.util.TimeCalculationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Переотдых в пункте оборота на стыке месяцев. Переотдых считается «по
 * календарю»: интервал от конца нормы отдыха до следующей явки режется по
 * границе месяца, а соседей из смежных месяцев месячный расчёт получает через
 * `adjacentRoutes` (в отработанное время они не входят).
 */
class OverRestMonthBoundaryTest {
    private val hour = 3_600_000L
    private val zone = TimeZone.of("GMT+3")

    private fun at(month: Int, day: Int, hour: Int, year: Int = 2025): Long =
        LocalDateTime(year, month, day, hour, 0).toInstant(zone).toEpochMilliseconds()

    private fun settings(month0: Int, tariff: Double = 100.0) = UserSettings(
        selectMonthOfYear = MonthOfYear(year = 2025, month = month0, tariffRate = tariff, days = emptyList()),
        timeZone = 0L,
        minTimeRestPointOfTurnover = 3 * hour,
    )

    private val salary = SalarySetting(zonalSurcharge = 0.0, harmfulnessPercent = 0.0)

    // Отдых в ПО 30 апреля: работа 15:00–19:00 (4 ч ≥ 3 ч нормы) → оплачиваемый
    // переотдых с 23:00 30-го. Явка 1 мая 03:00 → переотдых 4 ч, из них
    // 1 ч в апреле (23:00–00:00) и 3 ч в мае (00:00–03:00).
    private val lastOfApril = Route(
        basicData = BasicData(
            id = "apr-last",
            timeStartWork = at(4, 30, 15),
            timeEndWork = at(4, 30, 19),
            restPointOfTurnover = true,
        ),
    )
    private val firstOfMay = Route(
        basicData = BasicData(
            id = "may-first",
            timeStartWork = at(5, 1, 3),
            timeEndWork = at(5, 1, 9),
        ),
    )

    @Test
    fun mayPaysItsPartOfOverRestWhenPreviousMonthRouteIsAdjacent() = runTest {
        val helper = SalaryCalculationHelper(
            userSettings = settings(month0 = 4),
            salarySetting = salary,
            allRoutes = listOf(firstOfMay),
            adjacentRoutes = listOf(lastOfApril),
        )

        assertEquals(3 * hour, helper.getOverRestTimeFlow().first())
        assertEquals(300.0 * (2.0 / 3.0), helper.getMoneyOverRestFlow().first(), 0.001)
        // Сосед не попал в отработанное время месяца.
        assertEquals(6 * hour, helper.getTotalWorkTime().first())
    }

    @Test
    fun aprilPaysItsPartOfOverRestWhenNextMonthRouteIsAdjacent() = runTest {
        val helper = SalaryCalculationHelper(
            userSettings = settings(month0 = 3),
            salarySetting = salary,
            allRoutes = listOf(lastOfApril),
            adjacentRoutes = listOf(firstOfMay),
        )

        assertEquals(hour, helper.getOverRestTimeFlow().first())
        assertEquals(100.0 * (2.0 / 3.0), helper.getMoneyOverRestFlow().first(), 0.001)
        assertEquals(4 * hour, helper.getTotalWorkTime().first())
    }

    @Test
    fun monthPartsSumToFullOverRestOfTheTrip() = runTest {
        val april = SalaryCalculationHelper(
            userSettings = settings(month0 = 3),
            salarySetting = salary,
            allRoutes = listOf(lastOfApril),
            adjacentRoutes = listOf(firstOfMay),
        )
        val may = SalaryCalculationHelper(
            userSettings = settings(month0 = 4),
            salarySetting = salary,
            allRoutes = listOf(firstOfMay),
            adjacentRoutes = listOf(lastOfApril),
        )
        val trip = firstOfMay.overRestPayment(
            previous = lastOfApril,
            minTimeRest = 3 * hour,
            tariffRate = 100.0,
        )

        assertEquals(4 * hour, trip.timeMillis)
        assertEquals(
            trip.timeMillis,
            april.getOverRestTimeFlow().first() + may.getOverRestTimeFlow().first(),
        )
        assertEquals(
            trip.money,
            april.getMoneyOverRestFlow().first() + may.getMoneyOverRestFlow().first(),
            0.001,
        )
    }

    @Test
    fun withoutAdjacentRoutesBehaviourIsUnchanged() = runTest {
        val april = SalaryCalculationHelper(
            userSettings = settings(month0 = 3),
            salarySetting = salary,
            allRoutes = listOf(lastOfApril),
        )
        val may = SalaryCalculationHelper(
            userSettings = settings(month0 = 4),
            salarySetting = salary,
            allRoutes = listOf(firstOfMay),
        )

        assertEquals(0L, april.getOverRestTimeFlow().first())
        assertEquals(0L, may.getOverRestTimeFlow().first())
        assertEquals(0.0, april.getMoneyOverRestFlow().first(), 0.0)
        assertEquals(0.0, may.getMoneyOverRestFlow().first(), 0.0)
    }

    @Test
    fun adjacentRouteAlreadyInMonthListIsNotCountedTwice() = runTest {
        // Переходный маршрут (явка 30-го, сдача 1-го) есть и в списке месяца,
        // и среди «соседей» — переотдых от него не должен удваиваться.
        val crossing = Route(
            basicData = BasicData(
                id = "crossing",
                timeStartWork = at(4, 30, 20),
                timeEndWork = at(5, 1, 1),
                restPointOfTurnover = true,
            ),
        )
        val next = Route(
            basicData = BasicData(id = "next", timeStartWork = at(5, 1, 10), timeEndWork = at(5, 1, 14)),
        )
        val helper = SalaryCalculationHelper(
            userSettings = settings(month0 = 4),
            salarySetting = salary,
            allRoutes = listOf(crossing, next),
            adjacentRoutes = listOf(crossing),
        )

        // Работа 5 ч ≥ 3 ч → переотдых с 06:00 до явки 10:00 = 4 ч, один раз.
        assertEquals(4 * hour, helper.getOverRestTimeFlow().first())
    }

    @Test
    fun adjacentRoutesOfMonthPicksLastBeforeAndFirstAfterMonth() {
        val context = TimeCalculationContext.from(settings(month0 = 4))
        val earlierInApril = Route(
            basicData = BasicData(id = "apr-early", timeStartWork = at(4, 10, 8), timeEndWork = at(4, 10, 12)),
        )
        val laterInJune = Route(
            basicData = BasicData(id = "jun-late", timeStartWork = at(6, 20, 8), timeEndWork = at(6, 20, 12)),
        )
        val firstOfJune = Route(
            basicData = BasicData(id = "jun-first", timeStartWork = at(6, 1, 0), timeEndWork = at(6, 1, 6)),
        )
        val all = listOf(laterInJune, firstOfMay, earlierInApril, firstOfJune, lastOfApril)

        val adjacent = all.adjacentRoutesOfMonth(MonthOfYear(year = 2025, month = 4, days = emptyList()), context)

        assertEquals(listOf("apr-last", "jun-first"), adjacent.map { it.basicData.id })
    }

    @Test
    fun previousRouteForIsNearestEarlierStartRegardlessOfMonth() {
        val earlierInApril = Route(
            basicData = BasicData(id = "apr-early", timeStartWork = at(4, 10, 8), timeEndWork = at(4, 10, 12)),
        )
        val later = Route(
            basicData = BasicData(id = "may-later", timeStartWork = at(5, 5, 8), timeEndWork = at(5, 5, 12)),
        )
        val noStart = Route(basicData = BasicData(id = "no-start"))
        val candidates = listOf(later, noStart, earlierInApril, firstOfMay, lastOfApril)

        assertSame(lastOfApril, candidates.previousRouteFor(firstOfMay))
        assertNull(listOf(firstOfMay, later).previousRouteFor(firstOfMay))
        assertNull(candidates.previousRouteFor(noStart))
    }
}
