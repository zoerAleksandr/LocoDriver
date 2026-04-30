package com.z_company.domain.util_test

import com.z_company.domain.util.CalculateNightTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Тесты для CalculateNightTime.getNightTime() с учётом часового пояса.
 *
 * Ночное время: 22:00–06:00 по местному.
 * offsetInMoscow = 0 → Москва (UTC+3).
 * offsetInMoscow = 7_200_000 → Екатеринбург (UTC+5, +2ч от Москвы).
 *
 * Метод getNightTime:
 * 1. Добавляет offsetInMoscow к startMillis/endMillis
 * 2. Строит ночные интервалы через getTimeZone(offset) → "GMT+N"
 * 3. Ночной интервал в часовом поясе пользователя
 */
class NightTimeTimezoneTest {

    private val msk = TimeZone.of("Europe/Moscow")
    private val oneHourMs = 3_600_000L
    private val offsetMoscow = 0L
    private val offsetEkaterinburg = 7_200_000L

    // Ночное время 22:00–06:00
    private val nightStartHour = 22
    private val nightStartMinute = 0
    private val nightEndHour = 6
    private val nightEndMinute = 0

    /** Epoch millis из даты/времени по Москве */
    private fun mskMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long {
        return LocalDateTime(year, month, day, hour, minute)
            .toInstant(msk)
            .toEpochMilliseconds()
    }

    private suspend fun nightTime(
        startMillis: Long,
        endMillis: Long,
        offset: Long,
        breakStart: Long? = null,
        breakEnd: Long? = null
    ): Long {
        return CalculateNightTime.getNightTime(
            startMillis = startMillis,
            endMillis = endMillis,
            hourStart = nightStartHour,
            minuteStart = nightStartMinute,
            hourEnd = nightEndHour,
            minuteEnd = nightEndMinute,
            offsetInMoscow = offset,
            breakStartMillis = breakStart,
            breakEndMillis = breakEnd
        ).first() ?: 0L
    }

    // ==================== Москва (offset = 0) ====================

    @Test
    fun moscow_routeOverlapsNightPartially_returns4Hours() = runTest {
        // Маршрут 20:00–02:00 MSK → ночные = 22:00–02:00 = 4 часа
        val start = mskMillis(2025, 1, 15, 20, 0)
        val end = mskMillis(2025, 1, 16, 2, 0)
        val result = nightTime(start, end, offsetMoscow)
        assertEquals(4 * oneHourMs, result)
    }

    @Test
    fun moscow_routeDayOnly_returnsZero() = runTest {
        // Маршрут 08:00–20:00 MSK → полностью дневной, ночных = 0
        val start = mskMillis(2025, 1, 15, 8, 0)
        val end = mskMillis(2025, 1, 15, 20, 0)
        val result = nightTime(start, end, offsetMoscow)
        assertEquals(0L, result)
    }

    @Test
    fun moscow_routeFullyInNight_returnsFullDuration() = runTest {
        // Маршрут 23:00–05:00 MSK → полностью в ночное время = 6 часов
        val start = mskMillis(2025, 1, 15, 23, 0)
        val end = mskMillis(2025, 1, 16, 5, 0)
        val result = nightTime(start, end, offsetMoscow)
        assertEquals(6 * oneHourMs, result)
    }

    @Test
    fun moscow_routeStartAtNightBoundary_returns8Hours() = runTest {
        // Маршрут 22:00–06:00 MSK → ровно ночной интервал = 8 часов
        val start = mskMillis(2025, 1, 15, 22, 0)
        val end = mskMillis(2025, 1, 16, 6, 0)
        val result = nightTime(start, end, offsetMoscow)
        assertEquals(8 * oneHourMs, result)
    }

    // ==================== Екатеринбург (offset = +2ч) ====================

    @Test
    fun ekaterinburg_routeShiftsToMoreNight() = runTest {
        // Маршрут 20:00–02:00 MSK (6ч работы)
        // В EKB (MSK+2): 22:00–04:00 → ночные = 22:00–04:00 = 6 часов
        val start = mskMillis(2025, 1, 15, 20, 0)
        val end = mskMillis(2025, 1, 16, 2, 0)
        val result = nightTime(start, end, offsetEkaterinburg)
        assertEquals(6 * oneHourMs, result)
    }

    @Test
    fun ekaterinburg_dayRoute_returnsZero() = runTest {
        // Маршрут 06:00–18:00 MSK = 08:00–20:00 EKB → дневной, ночных = 0
        val start = mskMillis(2025, 1, 15, 6, 0)
        val end = mskMillis(2025, 1, 15, 18, 0)
        val result = nightTime(start, end, offsetEkaterinburg)
        assertEquals(0L, result)
    }

    @Test
    fun ekaterinburg_dayRouteInMoscowButNightInEkb() = runTest {
        // Маршрут 19:00–21:00 MSK → дневной по Москве
        // В EKB: 21:00–23:00 → ночные = 22:00–23:00 = 1 час
        val start = mskMillis(2025, 1, 15, 19, 0)
        val end = mskMillis(2025, 1, 15, 21, 0)
        val result = nightTime(start, end, offsetEkaterinburg)
        assertEquals(1 * oneHourMs, result)
    }

    // ==================== С перерывом ====================

    @Test
    fun moscow_breakInNightTime_subtracted() = runTest {
        // Маршрут 20:00–04:00 MSK, перерыв 00:00–01:00 MSK
        // Ночные без перерыва: 22:00–04:00 = 6 часов
        // Перерыв в ночное: 00:00–01:00 = 1 час
        // Итого: 5 часов
        val start = mskMillis(2025, 1, 15, 20, 0)
        val end = mskMillis(2025, 1, 16, 4, 0)
        val breakStart = mskMillis(2025, 1, 16, 0, 0)
        val breakEnd = mskMillis(2025, 1, 16, 1, 0)
        val result = nightTime(start, end, offsetMoscow, breakStart, breakEnd)
        assertEquals(5 * oneHourMs, result)
    }

    @Test
    fun moscow_breakInDayTime_noEffect() = runTest {
        // Маршрут 20:00–04:00 MSK, перерыв 20:30–21:00 MSK (дневное время)
        // Ночные = 22:00–04:00 = 6 часов, перерыв не пересекается → 6 часов
        val start = mskMillis(2025, 1, 15, 20, 0)
        val end = mskMillis(2025, 1, 16, 4, 0)
        val breakStart = mskMillis(2025, 1, 15, 20, 30)
        val breakEnd = mskMillis(2025, 1, 15, 21, 0)
        val result = nightTime(start, end, offsetMoscow, breakStart, breakEnd)
        assertEquals(6 * oneHourMs, result)
    }

    @Test
    fun ekaterinburg_breakInNightLocalTime_subtracted() = runTest {
        // Маршрут 20:00–04:00 MSK (8ч), перерыв 22:00–23:00 MSK
        // В EKB: маршрут 22:00–06:00, перерыв 00:00–01:00
        // Ночные EKB без перерыва: 22:00–06:00 = 8 часов
        // Перерыв в ночное (EKB): 00:00–01:00 = 1 час
        // Итого: 7 часов
        val start = mskMillis(2025, 1, 15, 20, 0)
        val end = mskMillis(2025, 1, 16, 4, 0)
        val breakStart = mskMillis(2025, 1, 15, 22, 0)
        val breakEnd = mskMillis(2025, 1, 15, 23, 0)
        val result = nightTime(start, end, offsetEkaterinburg, breakStart, breakEnd)
        assertEquals(7 * oneHourMs, result)
    }

    // ==================== Краевые случаи ====================

    @Test
    fun nullStartMillis_returnsZero() = runTest {
        val result = CalculateNightTime.getNightTime(
            startMillis = null,
            endMillis = mskMillis(2025, 1, 15, 20, 0),
            hourStart = nightStartHour,
            minuteStart = nightStartMinute,
            hourEnd = nightEndHour,
            minuteEnd = nightEndMinute,
            offsetInMoscow = offsetMoscow
        ).first()
        assertEquals(null, result)
    }

    @Test
    fun nullEndMillis_returnsNull() = runTest {
        val result = CalculateNightTime.getNightTime(
            startMillis = mskMillis(2025, 1, 15, 20, 0),
            endMillis = null,
            hourStart = nightStartHour,
            minuteStart = nightStartMinute,
            hourEnd = nightEndHour,
            minuteEnd = nightEndMinute,
            offsetInMoscow = offsetMoscow
        ).first()
        assertEquals(null, result)
    }
}
