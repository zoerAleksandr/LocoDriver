@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.domain.util_for_entities_test

import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.getNewRoutesToDayRange
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.setting.CrossMonthTimezone
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.util.TimeCalculationContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * BUG-37: `List<Route>.getWorkTime` для переходного маршрута берёт часть месяца
 * без вычета перерыва (в отличие от `Route.getWorkTimeInMonth`).
 * BUG-36 (перерыв): `getNewRoutesToDayRange` при смене тарифа обрезал окно
 * смены, но не перерыв — переходящий через дату смены маршрут вычитал перерыв
 * в обеих частях.
 */
class TransitionRouteWorkTimeTest {

    private val hour = 3_600_000L
    private val msk = TimeZone.of("GMT+3")

    private val moscowContext = TimeCalculationContext.from(
        UserSettings(timeZone = 3 * hour, crossMonthTimezone = CrossMonthTimezone.MOSCOW)
    )

    private fun mskMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        LocalDateTime(year, month, day, hour, minute).toInstant(msk).toEpochMilliseconds()

    private fun routeOf(
        startWork: Long,
        endWork: Long,
        breakStart: Long? = null,
        breakEnd: Long? = null,
    ): Route = Route(
        basicData = BasicData(
            timeStartWork = startWork,
            timeEndWork = endWork,
            timeStartBreak = breakStart,
            timeEndBreak = breakEnd,
        )
    )

    private fun september2026(): MonthOfYear {
        val days = (1..30).map { Day(dayOfMonth = it, tag = TagForDay.WORKING_DAY) }
        return MonthOfYear(year = 2026, month = 8, days = days) // month=8 → сентябрь
    }

    @Test
    fun transitionRouteWorkTimeDeductsBreakInsideMonth() {
        // 31.08 20:00 → 01.09 08:00, перерыв 01.09 00:00–01:00 (1 ч целиком в сентябре).
        // Сентябрьская часть: 8 ч − 1 ч = 7 ч (раньше — 8 ч без вычета перерыва).
        val route = routeOf(
            startWork = mskMillis(2026, 8, 31, 20, 0),
            endWork = mskMillis(2026, 9, 1, 8, 0),
            breakStart = mskMillis(2026, 9, 1, 0, 0),
            breakEnd = mskMillis(2026, 9, 1, 1, 0),
        )

        assertEquals(
            7 * hour,
            listOf(route).getWorkTime(september2026(), moscowContext),
        )
    }

    @Test
    fun transitionRouteWorkTimeDeductsOnlyBreakPortionInsideMonth() {
        // Перерыв 31.08 23:00–01.09 01:00: в сентябре только 1 ч из 2 ч.
        val route = routeOf(
            startWork = mskMillis(2026, 8, 31, 20, 0),
            endWork = mskMillis(2026, 9, 1, 8, 0),
            breakStart = mskMillis(2026, 8, 31, 23, 0),
            breakEnd = mskMillis(2026, 9, 1, 1, 0),
        )

        assertEquals(
            7 * hour,
            listOf(route).getWorkTime(september2026(), moscowContext),
        )
    }

    @Test
    fun tariffSplitDeductsBreakOnlyOnceAcrossParts() {
        // 14.09 22:00 → 15.09 10:00 (12 ч), перерыв 15.09 02:00–03:00 (1 ч).
        // Смена тарифа 15-го: часть 1 = [22:00; 00:00) без перерыва (2 ч),
        // часть 2 = [00:00; 10:00) минус 1 ч перерыва (9 ч). Сумма = 11 ч,
        // а не 10 ч (перерыв вычитался в обеих частях).
        val route = routeOf(
            startWork = mskMillis(2026, 9, 14, 22, 0),
            endWork = mskMillis(2026, 9, 15, 10, 0),
            breakStart = mskMillis(2026, 9, 15, 2, 0),
            breakEnd = mskMillis(2026, 9, 15, 3, 0),
        )
        val routes = listOf(route)

        val first = routes.getNewRoutesToDayRange(
            days = 1..15,
            monthOfYear = september2026(),
            context = moscowContext,
            isLastDayOfMonth = false,
        )
        val second = routes.getNewRoutesToDayRange(
            days = 15..30,
            monthOfYear = september2026(),
            context = moscowContext,
            isLastDayOfMonth = true,
        )

        assertEquals(2 * hour, first.single().let { it.basicData.timeEndWork!! - it.basicData.timeStartWork!! })
        assertEquals(10 * hour, second.single().let { it.basicData.timeEndWork!! - it.basicData.timeStartWork!! })
        assertEquals(11 * hour, first.sumOf { it.getWorkTime() ?: 0L } + second.sumOf { it.getWorkTime() ?: 0L })
    }

    @Test
    fun tariffSplitSplitsBreakAcrossBoundary() {
        // Перерыв 14.09 23:00–15.09 01:00 (2 ч) делится границей смены тарифа:
        // 1 ч в каждой части, суммарно вычитается ровно 2 ч.
        val route = routeOf(
            startWork = mskMillis(2026, 9, 14, 22, 0),
            endWork = mskMillis(2026, 9, 15, 10, 0),
            breakStart = mskMillis(2026, 9, 14, 23, 0),
            breakEnd = mskMillis(2026, 9, 15, 1, 0),
        )
        val routes = listOf(route)

        val first = routes.getNewRoutesToDayRange(
            days = 1..15,
            monthOfYear = september2026(),
            context = moscowContext,
            isLastDayOfMonth = false,
        )
        val second = routes.getNewRoutesToDayRange(
            days = 15..30,
            monthOfYear = september2026(),
            context = moscowContext,
            isLastDayOfMonth = true,
        )

        assertEquals(1 * hour, first.single().getWorkTime() ?: 0L)
        assertEquals(9 * hour, second.single().getWorkTime() ?: 0L)
    }

    @Test
    fun tariffSplitSkipsEmptyPartOnBoundary() {
        // Явка ровно на границе смены тарифа: в первой части не должно быть
        // пустого куска, который вычитал бы перерыв второй раз.
        val route = routeOf(
            startWork = mskMillis(2026, 9, 15, 0, 0),
            endWork = mskMillis(2026, 9, 15, 8, 0),
            breakStart = mskMillis(2026, 9, 15, 1, 0),
            breakEnd = mskMillis(2026, 9, 15, 2, 0),
        )
        val routes = listOf(route)

        val first = routes.getNewRoutesToDayRange(
            days = 1..15,
            monthOfYear = september2026(),
            context = moscowContext,
            isLastDayOfMonth = false,
        )
        val second = routes.getNewRoutesToDayRange(
            days = 15..30,
            monthOfYear = september2026(),
            context = moscowContext,
            isLastDayOfMonth = true,
        )

        assertTrue(first.isEmpty(), "Пустой кусок на границе не должен попадать в список")
        assertEquals(7 * hour, second.single().getWorkTime() ?: 0L)
    }
}
