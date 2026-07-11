package com.z_company.domain.entities

import com.z_company.domain.util.getTimeZone
import com.z_company.domain.util.TimeCalculationContext
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
            // «Командировка» оплачивается отдельно (по среднему за отработанные
            // маршруты), поэтому её дни НЕ попадают в общий пул «оплата по среднему».
            if (day.isReleaseDay &&
                day.releaseType != ReleaseType.ChildCare &&
                day.releaseType != ReleaseType.BusinessTrip
            ) {
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
        offsetInMoscow: Long = 0L,
    ): Long {
        val localTZ = TimeZone.of(getTimeZone(offsetInMoscow))
        val startLdt = Instant.fromEpochMilliseconds(startTime).toLocalDateTime(localTZ)

        return if (startLdt.monthNumber - 1 == this.month) {
            // Явка в текущем месяце → маршрут переходит в следующий.
            val nextMonthStart = LocalDate(this.year, this.month + 1, 1)
                .plus(1, DateTimeUnit.MONTH)
                .atStartOfDayIn(localTZ)
                .toEpochMilliseconds()
            nextMonthStart - startTime
        } else {
            // Явка в предыдущем месяце → считаем от 00:00 1-го числа текущего месяца до сдачи.
            val monthStart = LocalDate(this.year, this.month + 1, 1)
                .atStartOfDayIn(localTZ)
                .toEpochMilliseconds()
            endTime - monthStart
        }
    }

    // Context-based overload (preferred for new code)
    fun MonthOfYear.getTimeInCurrentMonth(startTime: Long, endTime: Long, context: TimeCalculationContext): Long {
        val tz = context.crossMonthTZ
        val startLdt = Instant.fromEpochMilliseconds(startTime).toLocalDateTime(tz)
        return if (startLdt.monthNumber - 1 == this.month) {
            val nextMonthStart = LocalDate(this.year, this.month + 1, 1)
                .plus(1, DateTimeUnit.MONTH)
                .atStartOfDayIn(tz)
                .toEpochMilliseconds()
            nextMonthStart - startTime
        } else {
            val monthStart = LocalDate(this.year, this.month + 1, 1)
                .atStartOfDayIn(tz)
                .toEpochMilliseconds()
            endTime - monthStart
        }
    }
}
