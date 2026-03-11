package com.z_company.domain.util_for_entities_test

import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.UtilForMonthOfYear.getTimeInCurrentMonth
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.isTransition
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Тесты для isTransition, getWorkTime и getTimeInCurrentMonth с учётом часовых поясов.
 *
 * Все timestamps — по Москве (UTC+3).
 * offsetInMoscow = 0 для Москвы, 7_200_000 для Екатеринбурга (UTC+5).
 *
 * Для корректной работы тестов система должна быть в MSK (UTC+3),
 * так как код использует TimeZone.currentSystemDefault().
 */
class TimezoneWorkTimeTest {

    private val msk = TimeZone.of("Europe/Moscow")
    private val oneHourMs = 3_600_000L
    private val offsetMoscow = 0L
    private val offsetEkaterinburg = 7_200_000L // +2ч относительно Москвы

    /** Создаёт epoch millis из даты/времени по Москве */
    private fun mskMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long {
        return LocalDateTime(year, month, day, hour, minute)
            .toInstant(msk)
            .toEpochMilliseconds()
    }

    private fun routeOf(startWork: Long, endWork: Long): Route {
        return Route(
            basicData = BasicData(
                timeStartWork = startWork,
                timeEndWork = endWork
            )
        )
    }

    // ==================== isTransition ====================

    @Test
    fun isTransition_Moscow_crossMonth_returnsTrue() {
        // 31 янв 22:00 MSK → 1 фев 06:00 MSK — переход через месяц
        val route = routeOf(
            startWork = mskMillis(2025, 1, 31, 22, 0),
            endWork = mskMillis(2025, 2, 1, 6, 0)
        )
        assertTrue(route.isTransition(offsetMoscow))
    }

    @Test
    fun isTransition_Moscow_sameMonth_returnsFalse() {
        // 15 янв 08:00 → 15 янв 20:00 MSK — в одном месяце
        val route = routeOf(
            startWork = mskMillis(2025, 1, 15, 8, 0),
            endWork = mskMillis(2025, 1, 15, 20, 0)
        )
        assertFalse(route.isTransition(offsetMoscow))
    }

    @Test
    fun isTransition_Ekaterinburg_shiftedToSameMonth_returnsFalse() {
        // 31 янв 22:00 MSK → 1 фев 04:00 MSK
        // В местном (EKB = MSK+2): 1 фев 00:00 → 1 фев 06:00 — оба в феврале!
        val route = routeOf(
            startWork = mskMillis(2025, 1, 31, 22, 0),
            endWork = mskMillis(2025, 2, 1, 4, 0)
        )
        assertFalse(route.isTransition(offsetEkaterinburg))
    }

    @Test
    fun isTransition_Ekaterinburg_crossMonth_returnsTrue() {
        // 31 янв 20:00 MSK → 1 фев 01:00 MSK
        // В местном (EKB): 31 янв 22:00 → 1 фев 03:00 — переход
        val route = routeOf(
            startWork = mskMillis(2025, 1, 31, 20, 0),
            endWork = mskMillis(2025, 2, 1, 1, 0)
        )
        assertTrue(route.isTransition(offsetEkaterinburg))
    }

    @Test
    fun isTransition_nullStartWork_returnsFalse() {
        val route = Route(basicData = BasicData(timeStartWork = null, timeEndWork = mskMillis(2025, 1, 15, 20, 0)))
        assertFalse(route.isTransition(offsetMoscow))
    }

    @Test
    fun isTransition_nullEndWork_returnsFalse() {
        val route = Route(basicData = BasicData(timeStartWork = mskMillis(2025, 1, 15, 8, 0), timeEndWork = null))
        assertFalse(route.isTransition(offsetMoscow))
    }

    @Test
    fun isTransition_yearTransition_returnsTrue() {
        // 31 дек 22:00 MSK → 1 янв 06:00 MSK — переход через год
        val route = routeOf(
            startWork = mskMillis(2025, 12, 31, 22, 0),
            endWork = mskMillis(2026, 1, 1, 6, 0)
        )
        assertTrue(route.isTransition(offsetMoscow))
    }

    // ==================== getWorkTime ====================

    private fun januaryMonthOfYear(): MonthOfYear {
        val days = (1..31).map { Day(dayOfMonth = it, tag = TagForDay.WORKING_DAY) }
        return MonthOfYear(year = 2025, month = 0, days = days) // month=0 = январь
    }

    private fun februaryMonthOfYear(): MonthOfYear {
        val days = (1..28).map { Day(dayOfMonth = it, tag = TagForDay.WORKING_DAY) }
        return MonthOfYear(year = 2025, month = 1, days = days) // month=1 = февраль
    }

    @Test
    fun getWorkTime_Moscow_noTransition_returnsFullTime() {
        // Маршрут 15 янв 08:00 → 15 янв 20:00 MSK = 12 часов, месяц = январь
        val route = routeOf(
            startWork = mskMillis(2025, 1, 15, 8, 0),
            endWork = mskMillis(2025, 1, 15, 20, 0)
        )
        val result = listOf(route).getWorkTime(januaryMonthOfYear(), offsetMoscow)
        assertEquals(12 * oneHourMs, result)
    }

    @Test
    fun getWorkTime_Moscow_transitionRoute_returnsPartialTime() {
        // 31 янв 22:00 → 1 фев 06:00 MSK (8 часов), month = январь
        // Январская часть: 22:00 → 00:00 = 2 часа
        val route = routeOf(
            startWork = mskMillis(2025, 1, 31, 22, 0),
            endWork = mskMillis(2025, 2, 1, 6, 0)
        )
        val result = listOf(route).getWorkTime(januaryMonthOfYear(), offsetMoscow)
        assertEquals(2 * oneHourMs, result)
    }

    @Test
    fun getWorkTime_Moscow_twoRoutes_sumsCorrectly() {
        // Обычный маршрут 12ч + переходный 8ч (в текущем месяце 2ч)
        val routeNormal = routeOf(
            startWork = mskMillis(2025, 1, 15, 8, 0),
            endWork = mskMillis(2025, 1, 15, 20, 0)
        )
        val routeTransition = routeOf(
            startWork = mskMillis(2025, 1, 31, 22, 0),
            endWork = mskMillis(2025, 2, 1, 6, 0)
        )
        val result = listOf(routeNormal, routeTransition).getWorkTime(januaryMonthOfYear(), offsetMoscow)
        assertEquals(14 * oneHourMs, result) // 12 + 2
    }

    @Test
    fun getWorkTime_Ekaterinburg_shiftMovesRouteToNextMonth() {
        // 31 янв 22:00 MSK → 1 фев 04:00 MSK (6 часов)
        // В EKB: 1 фев 00:00 → 1 фев 06:00 — не transition, полностью в феврале
        // Для января: isTransition = false, route.getWorkTime() = 6ч, но...
        // Wait: getWorkTime() returns full 6h regardless of which month.
        // The method only splits via getTimeInCurrentMonth when isTransition=true.
        // When isTransition=false, it returns full route time.
        // This is a limitation: the route is in February for EKB, but getWorkTime for January still counts it.
        // That's because route filtering by month is done at a higher level (in RouteUseCase).
        // So for January with offset=EKB, this route is NOT transition → returns full time (6h).
        // This test checks that isTransition correctly returns false.
        val route = routeOf(
            startWork = mskMillis(2025, 1, 31, 22, 0),
            endWork = mskMillis(2025, 2, 1, 4, 0)
        )
        // isTransition = false for EKB (both dates in February)
        assertFalse(route.isTransition(offsetEkaterinburg))
        // For February: isTransition=false → full time
        val resultFeb = listOf(route).getWorkTime(februaryMonthOfYear(), offsetEkaterinburg)
        assertEquals(6 * oneHourMs, resultFeb)
    }

    @Test
    fun getWorkTime_Ekaterinburg_transitionRoute_returnsPartialTime() {
        // 31 янв 20:00 MSK → 1 фев 01:00 MSK (5 часов)
        // В EKB: 31 янв 22:00 → 1 фев 03:00 — transition!
        // Январская часть: 22:00 → 00:00 = 2 часа (getTimeInCurrentMonth для adjusted timestamps)
        val route = routeOf(
            startWork = mskMillis(2025, 1, 31, 20, 0),
            endWork = mskMillis(2025, 2, 1, 1, 0)
        )
        assertTrue(route.isTransition(offsetEkaterinburg))
        val result = listOf(route).getWorkTime(januaryMonthOfYear(), offsetEkaterinburg)
        assertEquals(2 * oneHourMs, result)
    }

    // ==================== getTimeInCurrentMonth ====================

    @Test
    fun getTimeInCurrentMonth_startInCurrentMonth_returnsTimeTillEndOfDay() {
        // Start: 31 янв 22:00 MSK, End: 1 фев 06:00 MSK, month = январь
        // startLdt.month == январь → nextDayStart - startTime = 00:00(1фев) - 22:00(31янв) = 2ч
        val january = januaryMonthOfYear()
        val startTime = mskMillis(2025, 1, 31, 22, 0)
        val endTime = mskMillis(2025, 2, 1, 6, 0)
        val result = january.getTimeInCurrentMonth(startTime, endTime)
        assertEquals(2 * oneHourMs, result)
    }

    @Test
    fun getTimeInCurrentMonth_endInCurrentMonth_returnsTimeFromStartOfDay() {
        // Start: 31 янв 22:00 MSK, End: 1 фев 06:00 MSK, month = февраль
        // startLdt.month == январь ≠ февраль → endTime - dayStart = 06:00 - 00:00 = 6ч
        val february = februaryMonthOfYear()
        val startTime = mskMillis(2025, 1, 31, 22, 0)
        val endTime = mskMillis(2025, 2, 1, 6, 0)
        val result = february.getTimeInCurrentMonth(startTime, endTime)
        assertEquals(6 * oneHourMs, result)
    }

    @Test
    fun getTimeInCurrentMonth_Ekaterinburg_adjustedTimestamps() {
        // 31 янв 20:00 MSK + 2ч offset = 31 янв 22:00 (adjusted)
        // 1 фев 01:00 MSK + 2ч offset = 1 фев 03:00 (adjusted)
        // month = январь → startLdt.month = январь → 00:00(1фев) - 22:00(31янв) = 2ч
        val january = januaryMonthOfYear()
        val startAdjusted = mskMillis(2025, 1, 31, 20, 0) + offsetEkaterinburg
        val endAdjusted = mskMillis(2025, 2, 1, 1, 0) + offsetEkaterinburg
        val result = january.getTimeInCurrentMonth(startAdjusted, endAdjusted)
        assertEquals(2 * oneHourMs, result)
    }

    @Test
    fun getTimeInCurrentMonth_complementaryParts_sumToTotal() {
        // Январь + Февраль = полное время маршрута
        val startTime = mskMillis(2025, 1, 31, 22, 0)
        val endTime = mskMillis(2025, 2, 1, 6, 0)
        val janPart = januaryMonthOfYear().getTimeInCurrentMonth(startTime, endTime)
        val febPart = februaryMonthOfYear().getTimeInCurrentMonth(startTime, endTime)
        assertEquals(endTime - startTime, janPart + febPart)
    }
}
