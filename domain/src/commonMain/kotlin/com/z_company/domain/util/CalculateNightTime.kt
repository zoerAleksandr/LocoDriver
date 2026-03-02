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

    /** Пересечение двух интервалов [a1,a2) и [b1,b2). */
    private fun overlapDuration(a1: Long, a2: Long, b1: Long, b2: Long): Long {
        val start = maxOf(a1, b1)
        val end = minOf(a2, b2)
        return if (end > start) end - start else 0L
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
                val startLocalMillis = startMillis + offsetInMoscow
                val endLocalMillis = endMillis + offsetInMoscow

                val hasBreak = breakStartMillis != null && breakEndMillis != null && breakEndMillis > breakStartMillis
                val bStart = if (hasBreak) breakStartMillis!! + offsetInMoscow else 0L
                val bEnd = if (hasBreak) breakEndMillis!! + offsetInMoscow else 0L

                val dateList = mutableListOf<Long>()
                dateList.add(startLocalMillis)

                val tz = TimeZone.currentSystemDefault()
                var dayOfWorkMillis = Instant.fromEpochMilliseconds(startLocalMillis)
                    .toLocalDateTime(tz).date.atStartOfDayIn(tz).toEpochMilliseconds()

                while (isBeforeDay(dayOfWorkMillis, endLocalMillis)) {
                    dayOfWorkMillis = Instant.fromEpochMilliseconds(dayOfWorkMillis)
                        .toLocalDateTime(tz).date.plus(1, DateTimeUnit.DAY)
                        .atStartOfDayIn(tz).toEpochMilliseconds()
                    dateList.add(dayOfWorkMillis)
                }

                var countNightTime = 0L
                var breakNightOverlap = 0L
                val timeZoneStr = getTimeZone(offsetInMoscow)

                dateList.forEach { calMillis ->
                    val startNightMillis = withTime(calMillis, hourStart, minuteStart, timeZoneStr)
                    val endNightMillis = withTime(calMillis, hourEnd, minuteEnd, timeZoneStr)

                    if (hourStart <= hourEnd) {
                        if (calMillis in startNightMillis..endNightMillis) {
                            val endNightTime =
                                if (endNightMillis < endLocalMillis) endNightMillis else endLocalMillis
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
                            if (dayOfMonthSystemTZ(calMillis) == dayOfMonthSystemTZ(endLocalMillis)) {
                                endLocalMillis
                            } else {
                                Instant.fromEpochMilliseconds(calMillis)
                                    .toLocalDateTime(tz).date.plus(1, DateTimeUnit.DAY)
                                    .atStartOfDayIn(tz).toEpochMilliseconds()
                            }
                        val endNightTime = if (endLocalMillis < endNightMillis) {
                            endLocalMillis
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
                val tz = TimeZone.currentSystemDefault()
                val startWorkMonthNumber =
                    Instant.fromEpochMilliseconds(startMillis).toLocalDateTime(tz).monthNumber - 1

                val hasBreak = breakStartMillis != null && breakEndMillis != null && breakEndMillis > breakStartMillis
                val bStart = if (hasBreak) breakStartMillis!! + offsetInMoscow else 0L
                val bEnd = if (hasBreak) breakEndMillis!! + offsetInMoscow else 0L

                if (startWorkMonthNumber == month) {
                    val startLocalMillis = startMillis + offsetInMoscow
                    val endLocalMillis = endMillis + offsetInMoscow

                    val dateList = mutableListOf<Long>()
                    dateList.add(startLocalMillis)

                    var dayOfWorkMillis = Instant.fromEpochMilliseconds(startLocalMillis)
                        .toLocalDateTime(tz).date.atStartOfDayIn(tz).toEpochMilliseconds()

                    while (isBeforeDay(dayOfWorkMillis, endLocalMillis)) {
                        dayOfWorkMillis = Instant.fromEpochMilliseconds(dayOfWorkMillis)
                            .toLocalDateTime(tz).date.plus(1, DateTimeUnit.DAY)
                            .atStartOfDayIn(tz).toEpochMilliseconds()
                        dateList.add(dayOfWorkMillis)
                    }

                    var countNightTime = 0L
                    var breakNightOverlap = 0L
                    val timeZoneStr = getTimeZone(offsetInMoscow)

                    dateList.forEach { calMillis ->
                        val startNightMillis = withTime(calMillis, hourStart, minuteStart, timeZoneStr)
                        val endNightMillis = withTime(calMillis, hourEnd, minuteEnd, timeZoneStr)

                        if (hourStart <= hourEnd) {
                            if (calMillis in startNightMillis..endNightMillis) {
                                val endNightTime =
                                    if (endNightMillis < endLocalMillis) endNightMillis else endLocalMillis
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
                                if (dayOfMonthSystemTZ(calMillis) == dayOfMonthSystemTZ(endLocalMillis)) {
                                    endLocalMillis
                                } else {
                                    Instant.fromEpochMilliseconds(calMillis)
                                        .toLocalDateTime(tz).date.plus(1, DateTimeUnit.DAY)
                                        .atStartOfDayIn(tz).toEpochMilliseconds()
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
                    val startLocalMillis = startMillis + offsetInMoscow
                    val endLocalMillis = endMillis + offsetInMoscow

                    val dateList = mutableListOf<Long>()
                    dateList.add(startLocalMillis)

                    var dayOfWorkMillis = Instant.fromEpochMilliseconds(startLocalMillis)
                        .toLocalDateTime(tz).date.atStartOfDayIn(tz).toEpochMilliseconds()

                    while (isBeforeDay(dayOfWorkMillis, endLocalMillis)) {
                        dayOfWorkMillis = Instant.fromEpochMilliseconds(dayOfWorkMillis)
                            .toLocalDateTime(tz).date.plus(1, DateTimeUnit.DAY)
                            .atStartOfDayIn(tz).toEpochMilliseconds()
                        dateList.add(dayOfWorkMillis)
                    }

                    var countNightTime = 0L
                    var breakNightOverlap = 0L
                    dateList.forEach { calMillis ->
                        val startNightMillis = withTime(calMillis, hourStart, minuteStart, null)
                        val endNightMillis = withTime(calMillis, hourEnd, minuteEnd, null)

                        if (hourStart <= hourEnd) {
                            if (calMillis in startNightMillis..endNightMillis) {
                                val endNightTime =
                                    if (endNightMillis < endLocalMillis) endNightMillis else endLocalMillis
                                val nightTime = endNightTime - calMillis
                                countNightTime += nightTime
                                if (hasBreak) {
                                    breakNightOverlap += overlapDuration(bStart, bEnd, calMillis, endNightTime)
                                }
                            }
                        } else {
                            val endNightTime = if (endLocalMillis < endNightMillis) {
                                endLocalMillis
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

    private fun dayOfMonthSystemTZ(millis: Long): Int =
        Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault()).dayOfMonth
}

fun isBeforeDay(millis1: Long, millis2: Long): Boolean {
    val tz = TimeZone.currentSystemDefault()
    val day1 = Instant.fromEpochMilliseconds(millis1).toLocalDateTime(tz).date
    val day2 = Instant.fromEpochMilliseconds(millis2).toLocalDateTime(tz).date
    return day1 < day2
}
