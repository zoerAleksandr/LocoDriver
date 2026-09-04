@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.domain.util_for_entities_test

import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.filterByMonth
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
 * Отбор маршрутов месяца ([filterByMonth]) — общее правило для приложения
 * (RouteUseCase.routeListByMonthFlow) и виджета.
 *
 * Регрессия: виджет считал принадлежность месяцу по жёстко зашитому GMT+3 и
 * выбрасывал маршрут, который по местному времени относится уже к новому месяцу.
 * Из-за этого «отработано» в виджете было меньше, чем на главном экране.
 */
class MonthRouteFilterTest {

    private val hour = 3_600_000L
    private val msk = TimeZone.of("GMT+3")

    /** Иркутск = МСК+5, границы месяца по местному времени. */
    private val localContext = TimeCalculationContext.from(
        UserSettings(timeZone = 5 * hour, crossMonthTimezone = CrossMonthTimezone.LOCAL)
    )

    /** Тот же пользователь, но границы месяца по московскому времени. */
    private val moscowContext = TimeCalculationContext.from(
        UserSettings(timeZone = 5 * hour, crossMonthTimezone = CrossMonthTimezone.MOSCOW)
    )

    private fun mskMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        LocalDateTime(year, month, day, hour, minute).toInstant(msk).toEpochMilliseconds()

    private fun routeOf(startWork: Long?, endWork: Long?): Route =
        Route(basicData = BasicData(timeStartWork = startWork, timeEndWork = endWork))

    private fun september2026(): MonthOfYear {
        val days = (1..30).map { Day(dayOfMonth = it, tag = TagForDay.WORKING_DAY) }
        return MonthOfYear(year = 2026, month = 8, days = days) // month=8 → сентябрь
    }

    @Test
    fun localTimezoneKeepsRouteThatEntersMonthOnlyByLocalTime() {
        // 31.08 15:00 → 31.08 23:16 МСК. По местному (МСК+5) сдача — 01.09 04:16,
        // значит 4:16 относятся к сентябрю.
        val route = routeOf(
            startWork = mskMillis(2026, 8, 31, 15, 0),
            endWork = mskMillis(2026, 8, 31, 23, 16),
        )

        val filtered = listOf(route).filterByMonth(september2026(), localContext)

        assertTrue(filtered.isNotEmpty(), "Маршрут должен попасть в сентябрь по местному времени")
        assertEquals(
            4 * hour + 16 * 60_000L,
            filtered.getWorkTime(september2026(), localContext),
        )
    }

    @Test
    fun moscowTimezoneDropsRouteThatEndsInPreviousMonth() {
        // Тот же маршрут при crossMonthTimezone = MOSCOW целиком относится к августу.
        val route = routeOf(
            startWork = mskMillis(2026, 8, 31, 15, 0),
            endWork = mskMillis(2026, 8, 31, 23, 16),
        )

        assertTrue(listOf(route).filterByMonth(september2026(), moscowContext).isEmpty())
    }

    @Test
    fun transitionalRouteFromPreviousMonthIsKeptInBothTimezones() {
        // 31.08 20:00 МСК → 01.09 08:00 МСК — переходный в любой зоне.
        val route = routeOf(
            startWork = mskMillis(2026, 8, 31, 20, 0),
            endWork = mskMillis(2026, 9, 1, 8, 0),
        )

        assertTrue(listOf(route).filterByMonth(september2026(), moscowContext).isNotEmpty())
        assertTrue(listOf(route).filterByMonth(september2026(), localContext).isNotEmpty())
    }

    @Test
    fun routeFromNextMonthIsDropped() {
        val route = routeOf(
            startWork = mskMillis(2026, 10, 2, 8, 0),
            endWork = mskMillis(2026, 10, 2, 18, 0),
        )

        assertTrue(listOf(route).filterByMonth(september2026(), localContext).isEmpty())
    }

    @Test
    fun routeWithoutStartTimeIsKept() {
        // Как и в RouteUseCase: маршрут без времени явки остаётся в списке.
        val route = routeOf(startWork = null, endWork = null)

        assertTrue(listOf(route).filterByMonth(september2026(), localContext).isNotEmpty())
    }
}
