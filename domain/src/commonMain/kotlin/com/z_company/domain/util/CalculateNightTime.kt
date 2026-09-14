package com.z_company.domain.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

object CalculateNightTime {

    enum class ConsecutiveNightSource { WORK, TURNOVER_REST }

    data class ConsecutiveNightPeriod(
        val source: ConsecutiveNightSource,
        val startMillis: Long,
        val endMillis: Long,
        val intervals: List<Pair<Long, Long>>,
    )

    /**
     * Периоды, образующие «вторую ночь подряд» в фиксированном окне 00:00–05:00.
     * Рабочая ночь учитывается независимо от вида отдыха после маршрута. Ночной
     * отдых в пункте оборота также считается ночью, а домашний — нет.
     */
    fun getConsecutiveNightPeriods(
        previousStartMillis: Long?,
        previousEndMillis: Long?,
        previousRestPointOfTurnover: Boolean,
        currentStartMillis: Long?,
        currentEndMillis: Long?,
        offsetInMoscow: Long,
    ): List<ConsecutiveNightPeriod> {
        val currentStart = currentStartMillis ?: return emptyList()
        val currentEnd = currentEndMillis ?: return emptyList()
        if (currentEnd <= currentStart) return emptyList()

        fun intervals(start: Long, end: Long) = getNightIntervals(
            startMillis = start,
            endMillis = end,
            hourStart = 0,
            minuteStart = 0,
            hourEnd = 5,
            minuteEnd = 0,
            offsetInMoscow = offsetInMoscow,
        )

        val currentIntervals = intervals(currentStart, currentEnd)
        if (currentIntervals.isEmpty()) return emptyList()
        val currentPeriod = ConsecutiveNightPeriod(
            ConsecutiveNightSource.WORK, currentStart, currentEnd, currentIntervals
        )
        if (currentIntervals.size >= 2) return listOf(currentPeriod)

        val previousStart = previousStartMillis ?: return emptyList()
        val previousEnd = previousEndMillis ?: return emptyList()
        if (
            previousEnd <= previousStart || previousEnd > currentStart ||
            currentStart - previousEnd > 36L * 3_600_000L
        ) return emptyList()

        val timeZone = TimeZone.of(getTimeZone(offsetInMoscow))
        fun nightDate(interval: Pair<Long, Long>) =
            Instant.fromEpochMilliseconds(interval.first).toLocalDateTime(timeZone).date
        val requiredPreviousDate = nightDate(currentIntervals.first()).plus(-1, DateTimeUnit.DAY)

        val previousWorkIntervals = intervals(previousStart, previousEnd)
            .filter { nightDate(it) == requiredPreviousDate }
        if (previousWorkIntervals.isNotEmpty()) {
            return listOf(
                ConsecutiveNightPeriod(
                    ConsecutiveNightSource.WORK,
                    previousStart,
                    previousEnd,
                    previousWorkIntervals,
                ),
                currentPeriod,
            )
        }

        if (!previousRestPointOfTurnover) return emptyList()
        val turnoverIntervals = intervals(previousEnd, currentStart)
            .filter { nightDate(it) == requiredPreviousDate }
        if (turnoverIntervals.isEmpty()) return emptyList()
        return listOf(
            ConsecutiveNightPeriod(
                ConsecutiveNightSource.TURNOVER_REST,
                previousEnd,
                currentStart,
                turnoverIntervals,
            ),
            currentPeriod,
        )
    }

    /** Пересечение двух интервалов [a1,a2) и [b1,b2). */
    private fun overlapDuration(a1: Long, a2: Long, b1: Long, b2: Long): Long {
        val start = maxOf(a1, b1)
        val end = minOf(a2, b2)
        return if (end > start) end - start else 0L
    }

    /**
     * Сколько отдельных ночных окон захватывает маршрут. Ночное окно каждого
     * дня — [день hourStart:minuteStart, (день/след.день) hourEnd:minuteEnd].
     * Используется для предупреждения «вторая ночь подряд» внутри одного маршрута.
     */
    fun getNightWindowsCount(
        startMillis: Long?,
        endMillis: Long?,
        hourStart: Int,
        minuteStart: Int,
        hourEnd: Int,
        minuteEnd: Int,
        offsetInMoscow: Long,
    ): Int {
        if (startMillis == null || endMillis == null || endMillis <= startMillis) return 0
        val tzStr = getTimeZone(offsetInMoscow)
        val tz = TimeZone.of(tzStr)
        val crossesMidnight = hourStart > hourEnd ||
            (hourStart == hourEnd && minuteStart > minuteEnd)

        var day = Instant.fromEpochMilliseconds(startMillis).toLocalDateTime(tz).date
        if (crossesMidnight) day = day.plus(-1, DateTimeUnit.DAY)
        val endDay = Instant.fromEpochMilliseconds(endMillis).toLocalDateTime(tz).date
        var count = 0
        while (day <= endDay) {
            val dayStart = day.atStartOfDayIn(tz).toEpochMilliseconds()
            val nightStart = withTime(dayStart, hourStart, minuteStart, tzStr)
            val nightEnd = if (crossesMidnight) {
                val nextDayStart = day.plus(1, DateTimeUnit.DAY)
                    .atStartOfDayIn(tz).toEpochMilliseconds()
                withTime(nextDayStart, hourEnd, minuteEnd, tzStr)
            } else {
                withTime(dayStart, hourEnd, minuteEnd, tzStr)
            }
            if (overlapDuration(nightStart, nightEnd, startMillis, endMillis) > 0L) {
                count++
            }
            day = day.plus(1, DateTimeUnit.DAY)
        }
        return count
    }

    /**
     * Интервалы пересечения маршрута с ночными окнами (абсолютные ms).
     * Для графической линии предупреждения «вторая ночь подряд».
     */
    fun getNightIntervals(
        startMillis: Long?,
        endMillis: Long?,
        hourStart: Int,
        minuteStart: Int,
        hourEnd: Int,
        minuteEnd: Int,
        offsetInMoscow: Long,
    ): List<Pair<Long, Long>> {
        if (startMillis == null || endMillis == null || endMillis <= startMillis) return emptyList()
        val tzStr = getTimeZone(offsetInMoscow)
        val tz = TimeZone.of(tzStr)
        val crossesMidnight = hourStart > hourEnd ||
            (hourStart == hourEnd && minuteStart > minuteEnd)

        var day = Instant.fromEpochMilliseconds(startMillis).toLocalDateTime(tz).date
        if (crossesMidnight) day = day.plus(-1, DateTimeUnit.DAY)
        val endDay = Instant.fromEpochMilliseconds(endMillis).toLocalDateTime(tz).date
        val result = mutableListOf<Pair<Long, Long>>()
        while (day <= endDay) {
            val dayStart = day.atStartOfDayIn(tz).toEpochMilliseconds()
            val nightStart = withTime(dayStart, hourStart, minuteStart, tzStr)
            val nightEnd = if (crossesMidnight) {
                val nextDayStart = day.plus(1, DateTimeUnit.DAY)
                    .atStartOfDayIn(tz).toEpochMilliseconds()
                withTime(nextDayStart, hourEnd, minuteEnd, tzStr)
            } else {
                withTime(dayStart, hourEnd, minuteEnd, tzStr)
            }
            val overlapStart = maxOf(nightStart, startMillis)
            val overlapEnd = minOf(nightEnd, endMillis)
            if (overlapEnd > overlapStart) {
                result.add(overlapStart to overlapEnd)
            }
            day = day.plus(1, DateTimeUnit.DAY)
        }
        return result
    }

    fun getNightTime(
        startMillis: Long?,
        endMillis: Long?,
        hourStart: Int,
        minuteStart: Int,
        hourEnd: Int,
        minuteEnd: Int,
        offsetInMoscow: Long,
        breakStartMillis: Long? = null,
        breakEndMillis: Long? = null,
    ): Flow<Long?> {
        return channelFlow {
            if (startMillis == null || endMillis == null) {
                trySend(null)
            } else {
                val timeZoneStr = getTimeZone(offsetInMoscow)
                val localTZ = TimeZone.of(timeZoneStr)

                val hasBreak = breakStartMillis != null && breakEndMillis != null && breakEndMillis > breakStartMillis
                val bStart = if (hasBreak) breakStartMillis!! else 0L
                val bEnd = if (hasBreak) breakEndMillis!! else 0L

                val dateList = mutableListOf<Long>()
                dateList.add(startMillis)

                var dayOfWorkMillis = Instant.fromEpochMilliseconds(startMillis)
                    .toLocalDateTime(localTZ).date.atStartOfDayIn(localTZ).toEpochMilliseconds()

                while (isBeforeDay(dayOfWorkMillis, endMillis, localTZ)) {
                    dayOfWorkMillis = Instant.fromEpochMilliseconds(dayOfWorkMillis)
                        .toLocalDateTime(localTZ).date.plus(1, DateTimeUnit.DAY)
                        .atStartOfDayIn(localTZ).toEpochMilliseconds()
                    dateList.add(dayOfWorkMillis)
                }

                var countNightTime = 0L
                var breakNightOverlap = 0L

                dateList.forEach { calMillis ->
                    val startNightMillis = withTime(calMillis, hourStart, minuteStart, timeZoneStr)
                    val endNightMillis = withTime(calMillis, hourEnd, minuteEnd, timeZoneStr)

                    if (hourStart <= hourEnd) {
                        if (calMillis in startNightMillis..endNightMillis) {
                            val endNightTime =
                                if (endNightMillis < endMillis) endNightMillis else endMillis
                            val nightTime = endNightTime - calMillis
                            countNightTime += nightTime
                            if (hasBreak) {
                                breakNightOverlap += overlapDuration(bStart, bEnd, calMillis, endNightTime)
                            }
                        }
                    } else {
                        val startNightTime = if (calMillis < startNightMillis) {
                            startNightMillis
                        } else {
                            calMillis
                        }
                        val endTimeThisDay =
                            if (dayOfMonthInTZ(calMillis, localTZ) == dayOfMonthInTZ(endMillis, localTZ)) {
                                endMillis
                            } else {
                                Instant.fromEpochMilliseconds(calMillis)
                                    .toLocalDateTime(localTZ).date.plus(1, DateTimeUnit.DAY)
                                    .atStartOfDayIn(localTZ).toEpochMilliseconds()
                            }
                        val endNightTime = if (endMillis < endNightMillis) {
                            endMillis
                        } else {
                            endNightMillis
                        }
                        // First part night
                        if (calMillis < endNightTime) {
                            val nightTime = endNightTime - calMillis
                            countNightTime += nightTime
                            if (hasBreak) {
                                breakNightOverlap += overlapDuration(bStart, bEnd, calMillis, endNightTime)
                            }
                        }
                        // Second part night
                        if (endTimeThisDay > startNightTime) {
                            val nightTime = endTimeThisDay - startNightTime
                            countNightTime += nightTime
                            if (hasBreak) {
                                breakNightOverlap += overlapDuration(bStart, bEnd, startNightTime, endTimeThisDay)
                            }
                        }
                    }
                }
                if (hasBreak) {
                    countNightTime = maxOf(0L, countNightTime - breakNightOverlap)
                }
                trySend(countNightTime)
            }
        }
    }

    fun getNightTimeTransitionRoute(
        month: Int,
        startMillis: Long?,
        endMillis: Long?,
        hourStart: Int,
        minuteStart: Int,
        hourEnd: Int,
        minuteEnd: Int,
        offsetInMoscow: Long,
        breakStartMillis: Long? = null,
        breakEndMillis: Long? = null,
    ): Flow<Long?> {
        return channelFlow {
            if (startMillis == null || endMillis == null) {
                trySend(null)
            } else {
                val timeZoneStr = getTimeZone(offsetInMoscow)
                val localTZ = TimeZone.of(timeZoneStr)

                val startWorkMonthNumber =
                    Instant.fromEpochMilliseconds(startMillis).toLocalDateTime(localTZ).monthNumber - 1

                val hasBreak = breakStartMillis != null && breakEndMillis != null && breakEndMillis > breakStartMillis
                val bStart = if (hasBreak) breakStartMillis!! else 0L
                val bEnd = if (hasBreak) breakEndMillis!! else 0L

                if (startWorkMonthNumber == month) {
                    val dateList = mutableListOf<Long>()
                    dateList.add(startMillis)

                    var dayOfWorkMillis = Instant.fromEpochMilliseconds(startMillis)
                        .toLocalDateTime(localTZ).date.atStartOfDayIn(localTZ).toEpochMilliseconds()

                    while (isBeforeDay(dayOfWorkMillis, endMillis, localTZ)) {
                        dayOfWorkMillis = Instant.fromEpochMilliseconds(dayOfWorkMillis)
                            .toLocalDateTime(localTZ).date.plus(1, DateTimeUnit.DAY)
                            .atStartOfDayIn(localTZ).toEpochMilliseconds()
                        dateList.add(dayOfWorkMillis)
                    }

                    var countNightTime = 0L
                    var breakNightOverlap = 0L

                    dateList.forEach { calMillis ->
                        val startNightMillis = withTime(calMillis, hourStart, minuteStart, timeZoneStr)
                        val endNightMillis = withTime(calMillis, hourEnd, minuteEnd, timeZoneStr)

                        if (hourStart <= hourEnd) {
                            if (calMillis in startNightMillis..endNightMillis) {
                                val endNightTime =
                                    if (endNightMillis < endMillis) endNightMillis else endMillis
                                val nightTime = endNightTime - calMillis
                                countNightTime += nightTime
                                if (hasBreak) {
                                    breakNightOverlap += overlapDuration(bStart, bEnd, calMillis, endNightTime)
                                }
                            }
                        } else {
                            val startNightTime = if (calMillis < startNightMillis) {
                                startNightMillis
                            } else {
                                calMillis
                            }
                            val endTimeThisDay =
                                if (dayOfMonthInTZ(calMillis, localTZ) == dayOfMonthInTZ(endMillis, localTZ)) {
                                    endMillis
                                } else {
                                    Instant.fromEpochMilliseconds(calMillis)
                                        .toLocalDateTime(localTZ).date.plus(1, DateTimeUnit.DAY)
                                        .atStartOfDayIn(localTZ).toEpochMilliseconds()
                                }

                            // Second part night
                            if (endTimeThisDay > startNightTime) {
                                val nightTime = endTimeThisDay - startNightTime
                                countNightTime += nightTime
                                if (hasBreak) {
                                    breakNightOverlap += overlapDuration(bStart, bEnd, startNightTime, endTimeThisDay)
                                }
                            }
                        }
                    }
                    if (hasBreak) {
                        countNightTime = maxOf(0L, countNightTime - breakNightOverlap)
                    }
                    trySend(countNightTime)
                } else {
                    val dateList = mutableListOf<Long>()
                    dateList.add(startMillis)

                    var dayOfWorkMillis = Instant.fromEpochMilliseconds(startMillis)
                        .toLocalDateTime(localTZ).date.atStartOfDayIn(localTZ).toEpochMilliseconds()

                    while (isBeforeDay(dayOfWorkMillis, endMillis, localTZ)) {
                        dayOfWorkMillis = Instant.fromEpochMilliseconds(dayOfWorkMillis)
                            .toLocalDateTime(localTZ).date.plus(1, DateTimeUnit.DAY)
                            .atStartOfDayIn(localTZ).toEpochMilliseconds()
                        dateList.add(dayOfWorkMillis)
                    }

                    var countNightTime = 0L
                    var breakNightOverlap = 0L
                    dateList.forEach { calMillis ->
                        val startNightMillis = withTime(calMillis, hourStart, minuteStart, timeZoneStr)
                        val endNightMillis = withTime(calMillis, hourEnd, minuteEnd, timeZoneStr)

                        if (hourStart <= hourEnd) {
                            if (calMillis in startNightMillis..endNightMillis) {
                                val endNightTime =
                                    if (endNightMillis < endMillis) endNightMillis else endMillis
                                val nightTime = endNightTime - calMillis
                                countNightTime += nightTime
                                if (hasBreak) {
                                    breakNightOverlap += overlapDuration(bStart, bEnd, calMillis, endNightTime)
                                }
                            }
                        } else {
                            val endNightTime = if (endMillis < endNightMillis) {
                                endMillis
                            } else {
                                endNightMillis
                            }
                            // First part night
                            if (calMillis < endNightTime) {
                                val nightTime = endNightTime - calMillis
                                countNightTime += nightTime
                                if (hasBreak) {
                                    breakNightOverlap += overlapDuration(bStart, bEnd, calMillis, endNightTime)
                                }
                            }
                        }
                    }
                    if (hasBreak) {
                        countNightTime = maxOf(0L, countNightTime - breakNightOverlap)
                    }
                    trySend(countNightTime)
                }
            }
        }
    }

    /** Устанавливает час и минуту на дату из [millis] в заданном часовом поясе. */
    private fun withTime(millis: Long, hour: Int, minute: Int, tzStr: String?): Long {
        val tz = if (tzStr != null) TimeZone.of(tzStr) else TimeZone.currentSystemDefault()
        val date = Instant.fromEpochMilliseconds(millis).toLocalDateTime(tz).date
        return LocalDateTime(date, LocalTime(hour, minute, 0))
            .toInstant(tz).toEpochMilliseconds()
    }

    private fun dayOfMonthInTZ(millis: Long, tz: TimeZone): Int =
        Instant.fromEpochMilliseconds(millis).toLocalDateTime(tz).dayOfMonth
}

fun isBeforeDay(millis1: Long, millis2: Long, tz: TimeZone = TimeZone.currentSystemDefault()): Boolean {
    val day1 = Instant.fromEpochMilliseconds(millis1).toLocalDateTime(tz).date
    val day2 = Instant.fromEpochMilliseconds(millis2).toLocalDateTime(tz).date
    return day1 < day2
}
