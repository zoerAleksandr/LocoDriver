package com.z_company.domain.util_for_entities_test

import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.getWorkingTimeOnAHoliday
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Тесты для getWorkingTimeOnAHoliday() с учётом часового пояса.
 *
 * getWorkingTimeOnAHoliday:
 * 1. Создаёт границы праздника (00:00–00:00 следующего дня) в часовом поясе пользователя
 *    через getTimeZone(offsetInMoscow) → "GMT+N"
 * 2. Вычитает offsetInMoscow при вызове timeInLongInPeriod() — т.к. timestamps маршрутов по Москве,
 *    а границы праздника в местном часовом поясе
 */
class HolidayTimezoneTest {

    private val msk = TimeZone.of("Europe/Moscow")
    private val oneHourMs = 3_600_000L
    private val offsetMoscow = 0L
    private val offsetEkaterinburg = 7_200_000L

    private fun mskMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long {
        return LocalDateTime(year, month, day, hour, minute)
            .toInstant(msk)
            .toEpochMilliseconds()
    }

    private fun routeOf(
        startWork: Long,
        endWork: Long,
        breakStart: Long? = null,
        breakEnd: Long? = null,
    ): Route {
        return Route(
            basicData = BasicData(
                timeStartWork = startWork,
                timeEndWork = endWork,
                timeStartBreak = breakStart,
                timeEndBreak = breakEnd,
            )
        )
    }

    /** Январь с 1-м числом как праздником (Новый год) */
    private fun januaryWithHoliday(): MonthOfYear {
        val days = (1..31).map { dayNum ->
            Day(
                dayOfMonth = dayNum,
                tag = if (dayNum == 1) TagForDay.HOLIDAY else TagForDay.WORKING_DAY
            )
        }
        return MonthOfYear(year = 2025, month = 0, days = days) // month=0 = январь
    }

    /** Январь с 1-8 января как праздники */
    private fun januaryWithHolidaysWeek(): MonthOfYear {
        val days = (1..31).map { dayNum ->
            Day(
                dayOfMonth = dayNum,
                tag = if (dayNum in 1..8) TagForDay.HOLIDAY else TagForDay.WORKING_DAY
            )
        }
        return MonthOfYear(year = 2025, month = 0, days = days)
    }

    /** Декабрь без праздников */
    private fun december(): MonthOfYear {
        val days = (1..31).map { Day(dayOfMonth = it, tag = TagForDay.WORKING_DAY) }
        return MonthOfYear(year = 2024, month = 11, days = days) // month=11 = декабрь
    }

    // ==================== Москва ====================

    @Test
    fun moscow_routeFullyInHoliday_returnsFullTime() = runTest {
        // 1 янв 08:00 → 1 янв 20:00 MSK = 12 часов, 1 января праздник
        val route = routeOf(
            startWork = mskMillis(2025, 1, 1, 8, 0),
            endWork = mskMillis(2025, 1, 1, 20, 0)
        )
        val result = listOf(route).getWorkingTimeOnAHoliday(januaryWithHoliday(), offsetMoscow).first()
        assertEquals(12 * oneHourMs, result)
    }

    @Test
    fun moscow_routePartiallyInHoliday_returnsOverlap() = runTest {
        // 31 дек 22:00 → 1 янв 06:00 MSK (8 часов)
        // Праздник 1 янв (00:00–00:00 2янв), month=январь
        // Пересечение: 00:00(1янв) → 06:00(1янв) = 6 часов
        val route = routeOf(
            startWork = mskMillis(2024, 12, 31, 22, 0),
            endWork = mskMillis(2025, 1, 1, 6, 0)
        )
        val result = listOf(route).getWorkingTimeOnAHoliday(januaryWithHoliday(), offsetMoscow).first()
        assertEquals(6 * oneHourMs, result)
    }

    @Test
    fun moscow_holidayTime_excludesBreakOnlyInsideHolidayOverlap() = runTest {
        val route = routeOf(
            startWork = mskMillis(2025, 1, 1, 22, 0),
            endWork = mskMillis(2025, 1, 2, 6, 0),
            breakStart = mskMillis(2025, 1, 1, 23, 0),
            breakEnd = mskMillis(2025, 1, 2, 1, 0),
        )
        val result = listOf(route).getWorkingTimeOnAHoliday(januaryWithHoliday(), offsetMoscow).first()
        assertEquals(oneHourMs, result)
    }

    @Test
    fun moscow_routeNotInHoliday_returnsZero() = runTest {
        // 2 янв 08:00 → 2 янв 20:00 MSK, праздник только 1 января
        val route = routeOf(
            startWork = mskMillis(2025, 1, 2, 8, 0),
            endWork = mskMillis(2025, 1, 2, 20, 0)
        )
        val result = listOf(route).getWorkingTimeOnAHoliday(januaryWithHoliday(), offsetMoscow).first()
        assertEquals(0L, result)
    }

    @Test
    fun moscow_noHolidays_returnsZero() = runTest {
        // Декабрь без праздников
        val route = routeOf(
            startWork = mskMillis(2024, 12, 25, 8, 0),
            endWork = mskMillis(2024, 12, 25, 20, 0)
        )
        val result = listOf(route).getWorkingTimeOnAHoliday(december(), offsetMoscow).first()
        assertEquals(0L, result)
    }

    @Test
    fun moscow_multipleHolidays_sumsAll() = runTest {
        // Маршрут 1 янв 08:00 → 2 янв 08:00 MSK = 24 часа
        // 1-8 января праздники → 1 янв (08:00–00:00 = 16ч) + 2 янв (00:00–08:00 = 8ч) = 24ч
        val route = routeOf(
            startWork = mskMillis(2025, 1, 1, 8, 0),
            endWork = mskMillis(2025, 1, 2, 8, 0)
        )
        val result = listOf(route).getWorkingTimeOnAHoliday(januaryWithHolidaysWeek(), offsetMoscow).first()
        assertEquals(24 * oneHourMs, result)
    }

    // ==================== Екатеринбург ====================

    @Test
    fun ekaterinburg_routeInHolidayByLocalTime() = runTest {
        // 31 дек 22:00 MSK → 1 янв 04:00 MSK (6 часов)
        // В EKB: 1 янв 00:00 → 1 янв 06:00
        // Праздник 1 янв в EKB: 00:00 EKB → 00:00 2 янв EKB
        // Метод: startHoliday и endHoliday в "GMT+5", вычитает offset при проверке пересечения
        // startHoliday = 1 янв 00:00 GMT+5, endHoliday = 2 янв 00:00 GMT+5
        // startHoliday - offset = 1 янв 00:00 GMT+5 - 7_200_000 = 31 дек 22:00 GMT+5 → в epoch = 31 дек 22:00 MSK
        // Маршрут: 31 дек 22:00 MSK → 1 янв 04:00 MSK
        // Пересечение: 31 дек 22:00 → 1 янв 04:00 = 6 часов
        val route = routeOf(
            startWork = mskMillis(2024, 12, 31, 22, 0),
            endWork = mskMillis(2025, 1, 1, 4, 0)
        )
        val result = listOf(route).getWorkingTimeOnAHoliday(januaryWithHoliday(), offsetEkaterinburg).first()
        assertEquals(6 * oneHourMs, result)
    }

    @Test
    fun ekaterinburg_routeBeforeHolidayByLocalTime_returnsZero() = runTest {
        // 31 дек 08:00 MSK → 31 дек 19:00 MSK (11 часов)
        // В EKB: 31 дек 10:00 → 31 дек 21:00 — не праздник (праздник 1 янв)
        // startHoliday - offset для 1 янв = 31 дек 22:00 MSK
        // Маршрут заканчивается 19:00 MSK < 22:00 MSK → нет пересечения
        val route = routeOf(
            startWork = mskMillis(2024, 12, 31, 8, 0),
            endWork = mskMillis(2024, 12, 31, 19, 0)
        )
        val result = listOf(route).getWorkingTimeOnAHoliday(januaryWithHoliday(), offsetEkaterinburg).first()
        assertEquals(0L, result)
    }

    @Test
    fun ekaterinburg_routePartiallyOverlapsHoliday() = runTest {
        // 31 дек 20:00 MSK → 1 янв 02:00 MSK (6 часов)
        // В EKB: 31 дек 22:00 → 1 янв 04:00
        // Праздник 1 янв в EKB: 00:00 EKB → 00:00 2 янв EKB
        // Ожидаемое пересечение: 00:00–04:00 EKB = 4 часа
        // Раньше getWorkingTimeOnAHoliday() дополнительно сдвигала границы праздника
        // на -offsetInMoscow, расширяя период на 2ч назад и давая 6ч (весь маршрут).
        // Починено переходом на TimeCalculationContext (localTZ без двойного сдвига) —
        // тест обновлён под правильное значение.
        val route = routeOf(
            startWork = mskMillis(2024, 12, 31, 20, 0),
            endWork = mskMillis(2025, 1, 1, 2, 0)
        )
        val result = listOf(route).getWorkingTimeOnAHoliday(januaryWithHoliday(), offsetEkaterinburg).first()
        assertEquals(4 * oneHourMs, result)
    }

    // ==================== Несколько маршрутов ====================

    @Test
    fun multipleRoutes_holidayTimeSum() = runTest {
        // Маршрут 1: 1 янв 08:00 → 1 янв 20:00 (12 часов в праздник)
        // Маршрут 2: 1 янв 22:00 → 2 янв 06:00 (в праздник до 00:00 = 2 часа)
        val route1 = routeOf(
            startWork = mskMillis(2025, 1, 1, 8, 0),
            endWork = mskMillis(2025, 1, 1, 20, 0)
        )
        val route2 = routeOf(
            startWork = mskMillis(2025, 1, 1, 22, 0),
            endWork = mskMillis(2025, 1, 2, 6, 0)
        )
        val result = listOf(route1, route2).getWorkingTimeOnAHoliday(januaryWithHoliday(), offsetMoscow).first()
        assertEquals(14 * oneHourMs, result) // 12 + 2
    }

    @Test
    fun emptyRouteList_returnsZero() = runTest {
        val result = emptyList<Route>().getWorkingTimeOnAHoliday(januaryWithHoliday(), offsetMoscow).first()
        assertEquals(0L, result)
    }
}
