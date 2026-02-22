package com.z_company.domain.entities

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toInstant

object UtilForMonthOfYear {
    fun MonthOfYear.getPersonalNormaHours(): Int {
        var normaOfMonth = 0
        this.days.forEach { day ->
            if (!day.isReleaseDay) {
                normaOfMonth += when (day.tag) {
                    TagForDay.WORKING_DAY -> 8
                    TagForDay.SHORTENED_DAY -> 7
                    TagForDay.NON_WORKING_DAY -> 0
                    TagForDay.HOLIDAY -> 0
                }
            }
        }
        return normaOfMonth
    }

    fun MonthOfYear.getDayoffHours(): Int {
        var totalRelease = 0
        this.days.forEach { day ->
            if (day.isReleaseDay) {
                totalRelease += when (day.tag) {
                    TagForDay.WORKING_DAY -> 8
                    TagForDay.SHORTENED_DAY -> 7
                    TagForDay.NON_WORKING_DAY -> 0
                    TagForDay.HOLIDAY -> 0
                }
            }
        }
        return totalRelease
    }

    fun MonthOfYear.getDayoffHoursIncludingWeekends(): Int {
        var totalRelease = 0
        this.days.forEach { day ->
            if (day.isReleaseDay && day.releaseType != ReleaseType.ChildCare) {
                totalRelease += when (day.tag) {
                    TagForDay.WORKING_DAY -> 8
                    TagForDay.SHORTENED_DAY -> 7
                    TagForDay.NON_WORKING_DAY -> 0
                    TagForDay.HOLIDAY -> 0
                }
            }
        }
        return totalRelease
    }

    // расчет количества часов в отвлечении без учета выходных (для оплаты по уходу за ребенком-инвалидом)
    fun MonthOfYear.getDayoffHoursExcludingWeekends(): Int {
        var totalRelease = 0
        this.days.forEach { day ->
            if (day.isReleaseDay && day.releaseType == ReleaseType.ChildCare) {
                totalRelease += when (day.tag) {
                    TagForDay.WORKING_DAY -> 8
                    TagForDay.SHORTENED_DAY -> 7
                    TagForDay.NON_WORKING_DAY -> 8
                    TagForDay.HOLIDAY -> 8
                }
            }
        }
        return totalRelease
    }

    fun MonthOfYear.getStandardNormaHours(): Int {
        var normaOfMonth = 0
        this.days.forEach { day ->
            normaOfMonth += when (day.tag) {
                TagForDay.WORKING_DAY -> 8
                TagForDay.SHORTENED_DAY -> 7
                TagForDay.NON_WORKING_DAY -> 0
                TagForDay.HOLIDAY -> 0
            }
        }
        return normaOfMonth
    }

    fun MonthOfYear.getNormaHoursInDate(dateInMillis: Long): Int {
        val tz = TimeZone.currentSystemDefault()
        val currentDate = Instant.fromEpochMilliseconds(dateInMillis).toLocalDateTime(tz)
        var normaOfMonth = 0
        if (currentDate.monthNumber - 1 == this.month) {
            this.days.forEach { day ->
                if (day.isReleaseDay) {
                    return@forEach
                }
                if (currentDate.dayOfMonth >= day.dayOfMonth) {
                    normaOfMonth += when (day.tag) {
                        TagForDay.WORKING_DAY -> 8
                        TagForDay.SHORTENED_DAY -> 7
                        TagForDay.NON_WORKING_DAY -> 0
                        TagForDay.HOLIDAY -> 0
                    }
                }
            }
        }
        return normaOfMonth
    }

    fun MonthOfYear.getPersonalNormaHoursInPeriod(
        period: Pair<Int, Int>,
        monthOfYear: MonthOfYear
    ): Int {
        var normaOfMonth = 0
        this.days.forEach { day ->
            if (!day.isReleaseDay) {
                if (day.dayOfMonth in period.first..period.second) {
                    normaOfMonth += when (day.tag) {
                        TagForDay.WORKING_DAY -> 8
                        TagForDay.SHORTENED_DAY -> 7
                        TagForDay.NON_WORKING_DAY -> 0
                        TagForDay.HOLIDAY -> 0
                    }
                }
            }
        }
        return normaOfMonth
    }

    fun MonthOfYear.getTimeInCurrentMonth(
        startTime: Long,
        endTime: Long,
    ): Long {
        val tz = TimeZone.currentSystemDefault()
        val startLdt = Instant.fromEpochMilliseconds(startTime).toLocalDateTime(tz)

        return if (startLdt.monthNumber - 1 == this.month) {
            // End of current day = start of next day
            val nextDayStart = startLdt.date.plus(1, DateTimeUnit.DAY)
                .atStartOfDayIn(tz)
                .toEpochMilliseconds()
            nextDayStart - startTime
        } else {
            // Start of end's day
            val dayStart = Instant.fromEpochMilliseconds(endTime)
                .toLocalDateTime(tz)
                .date
                .atStartOfDayIn(tz)
                .toEpochMilliseconds()
            endTime - dayStart
        }
    }
}
