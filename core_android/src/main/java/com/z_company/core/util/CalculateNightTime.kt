package com.z_company.core.util

import androidx.core.util.rangeTo
import org.koin.core.component.KoinComponent
import java.util.Calendar
import java.util.Calendar.DAY_OF_MONTH
import java.util.Calendar.HOUR_OF_DAY
import java.util.Calendar.MILLISECOND
import java.util.Calendar.MINUTE
import java.util.Calendar.SECOND
import java.util.Calendar.getInstance

object CalculateNightTime : KoinComponent {
    fun getNightTime(
        startMillis: Long?,
        endMillis: Long?,
        hourStart: Int,
        minuteStart: Int,
        hourEnd: Int,
        minuteEnd: Int
    ): Long? {
        if (startMillis == null || endMillis == null) {
            return null
        }
        val startLocalDateTime = getInstance().also {
            it.timeInMillis = startMillis
        }

        val endLocalDateTime = getInstance().also {
            it.timeInMillis = endMillis
        }

        val dateList = mutableListOf<Calendar>()
        dateList.add(startLocalDateTime)
        val dayOfWork = getInstance().also {
            it.timeInMillis = startLocalDateTime.timeInMillis
            it.set(HOUR_OF_DAY, 0)
            it.set(MINUTE, 0)
        }
        while (isBeforeDay(dayOfWork, endLocalDateTime)) {
            dayOfWork.set(DAY_OF_MONTH, dayOfWork.get(DAY_OF_MONTH) + 1)
            val day = getInstance().also {
                it.timeInMillis = dayOfWork.timeInMillis
            }
            dateList.add(day)
        }
        var countNightTime = 0L
        dateList.forEach { calendar ->
            val startNight = getInstance().also {
                it.timeInMillis = calendar.timeInMillis
                it.set(HOUR_OF_DAY, hourStart)
                it.set(MINUTE, minuteStart)
            }

            val endNight = getInstance().also {
                it.timeInMillis = calendar.timeInMillis
                it.set(HOUR_OF_DAY, hourEnd)
                it.set(MINUTE, minuteEnd)
            }

            if (hourStart <= hourEnd) {
                if (startNight.rangeTo(endNight).contains(calendar)) {
                    val endNightTime =
                        if (endNight.before(endLocalDateTime)) endNight else endLocalDateTime
                    val nightTime = endNightTime.timeInMillis - calendar.timeInMillis
                    countNightTime += nightTime
                }

            } else {
                val startNightTime = if (calendar.before(startNight)) {
                    startNight
                } else {
                    calendar
                }
                val endTimeThisDay =
                    if (calendar.get(DAY_OF_MONTH) == endLocalDateTime.get(DAY_OF_MONTH)) {
                        endLocalDateTime
                    } else {
                        getInstance().also {
                            it.timeInMillis = calendar.timeInMillis
                            it.set(DAY_OF_MONTH, calendar.get(DAY_OF_MONTH) + 1)
                            it.set(HOUR_OF_DAY, 0)
                            it.set(MINUTE, 0)
                            it.set(SECOND, 0)
                            it.set(MILLISECOND, 0)
                        }
                    }
                val endNightTime = if (endLocalDateTime.before(endNight)) {
                    endLocalDateTime
                } else {
                    endNight
                }
                // First part night
                if (calendar.before(endNightTime)) {
                    val nightTime = endNightTime.timeInMillis - calendar.timeInMillis
                    countNightTime += nightTime
                }
                // Second part night
                if (endTimeThisDay.after(startNightTime)) {
                    val nightTime = endTimeThisDay.timeInMillis - startNightTime.timeInMillis
                    countNightTime += nightTime
                }
            }
        }
        return countNightTime
    }

    fun getNightTimeTransitionRoute(
        month: Int,
        year: Int,
        startMillis: Long?,
        endMillis: Long?,
        hourStart: Int,
        minuteStart: Int,
        hourEnd: Int,
        minuteEnd: Int
    ): Long? {
        if (startMillis == null || endMillis == null) {
            return null
        }
        val startWorkCalendar = getInstance().also {
            it.timeInMillis = startMillis
        }

        val endWorkCalendar = getInstance().also {
            it.timeInMillis = endMillis
        }
        if (startWorkCalendar.get(Calendar.MONTH) == month) {
            val endCurrentDay = getInstance().also {
                it.timeInMillis = startMillis
                it.set(DAY_OF_MONTH, it.get(DAY_OF_MONTH) + 1)
                it.set(HOUR_OF_DAY, 0)
                it.set(MINUTE, 0)
                it.set(MILLISECOND, 0)
            }
            val startWorkCalendarInMillis = startWorkCalendar.timeInMillis
            val endCurrentDayInMillis = endCurrentDay.timeInMillis
            return getNightTime(
                startMillis = startWorkCalendarInMillis,
                endMillis = endCurrentDayInMillis,
                hourStart = hourStart,
                minuteStart = minuteStart,
                hourEnd = hourEnd,
                minuteEnd = minuteEnd
            )
        } else {
            val startCurrentDay = getInstance().also {
                it.timeInMillis = endMillis
                it.set(HOUR_OF_DAY, 0)
                it.set(MINUTE, 0)
                it.set(MILLISECOND, 0)
            }
            val endWorkCalendarInMillis = endWorkCalendar.timeInMillis
            val startCurrentDayInMillis = startCurrentDay.timeInMillis
            return getNightTime(
                startMillis = startCurrentDayInMillis,
                endMillis = endWorkCalendarInMillis,
                hourStart = hourStart,
                minuteStart = minuteStart,
                hourEnd = hourEnd,
                minuteEnd = minuteEnd
            )
        }
    }
}

fun isBeforeDay(firstDay: Calendar, secondDay: Calendar): Boolean {
    val firstDateWithOfTime = getInstance().also {
        it.timeInMillis = firstDay.timeInMillis
        it.set(HOUR_OF_DAY, 0)
        it.set(MINUTE, 0)
        it.set(SECOND, 0)
        it.set(MILLISECOND, 0)
    }
    val secondDateWithOfTime = getInstance().also {
        it.timeInMillis = secondDay.timeInMillis
        it.set(HOUR_OF_DAY, 0)
        it.set(MINUTE, 0)
        it.set(SECOND, 0)
        it.set(MILLISECOND, 0)
    }

    return firstDateWithOfTime.before(secondDateWithOfTime)
}