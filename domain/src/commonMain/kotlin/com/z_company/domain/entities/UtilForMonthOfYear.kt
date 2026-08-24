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
    // «Выходной» (DayOff) — это перенос выходного дня, а не отвлечение: работник
    // отдыхает в этот день, но отрабатывает другой, поэтому месячная норма НЕ
    // меняется (в отличие от отпуска/больничного). Такой день не уменьшает норму
    // и не входит в пул «оплата по среднему» — единственный эффект DayOff —
    // ×2 при работе (см. UtilsForEntities.isHolidayTimeInRoute).
    private fun Day.reducesNorma(): Boolean =
        isReleaseDay &&
            releaseType != ReleaseType.DayOff &&
            // Командировка — рабочий период: её оплата вынесена в
            // отдельную строку, но месячную норму её дни не уменьшают.
            releaseType != ReleaseType.BusinessTrip &&
            // «Технические занятия» оплачиваются отдельно по среднему часу и на
            // норму не влияют (решение пользователя): норма остаётся полной.
            releaseType != ReleaseType.TechnicalStudy

    // Оставляем безаргументную перегрузку: Android-виджет и старые уже
    // скомпилированные модули вызывают именно JVM-сигнатуру
    // getPersonalNormaHours(MonthOfYear). Значение по умолчанию у параметра
    // не создаёт совместимую JVM-перегрузку.
    fun MonthOfYear.getPersonalNormaHours(): Int =
        getPersonalNormaHours(WorkScheduleProfile.standard())

    fun MonthOfYear.getPersonalNormaHours(profile: WorkScheduleProfile): Int {
        var normaOfMonth = 0
        this.days.forEach { day ->
            if (!day.reducesNorma()) {
                val date = LocalDate(year, month + 1, day.dayOfMonth)
                normaOfMonth += profile.effectiveHours(date, day.tag)
            }
        }
        return normaOfMonth
    }

    fun MonthOfYear.getDayoffHours(): Int =
        getDayoffHours(WorkScheduleProfile.standard())

    fun MonthOfYear.getDayoffHours(profile: WorkScheduleProfile): Int {
        var totalRelease = 0
        this.days.forEach { day ->
            // «Технические занятия» не входят в общий счётчик часов отвлечений —
            // у них собственные часы и отдельная оплата по среднему.
            if (day.isReleaseDay && day.releaseType != ReleaseType.TechnicalStudy) {
                val date = LocalDate(year, month + 1, day.dayOfMonth)
                totalRelease += profile.effectiveHours(date, day.tag)
            }
        }
        return totalRelease
    }

    fun MonthOfYear.getDayoffHoursIncludingWeekends(): Int =
        getDayoffHoursIncludingWeekends(WorkScheduleProfile.standard())

    fun MonthOfYear.getDayoffHoursIncludingWeekends(profile: WorkScheduleProfile): Int {
        var totalRelease = 0
        this.days.forEach { day ->
            // «Командировка» оплачивается отдельно (по среднему за отработанные
            // маршруты), поэтому её дни НЕ попадают в общий пул «оплата по среднему».
            // «Выходной» (DayOff) — перенос выходного, а не отвлечение: не входит
            // в пул «оплата по среднему» (см. Day.reducesNorma / getPersonalNormaHours).
            if (day.isReleaseDay &&
                day.releaseType != ReleaseType.ChildCare &&
                day.releaseType != ReleaseType.BusinessTrip &&
                day.releaseType != ReleaseType.DayOff &&
                // «Технические занятия» оплачиваются собственной строкой по
                // введённым часам, а не через общий пул «оплата по среднему».
                day.releaseType != ReleaseType.TechnicalStudy
            ) {
                val date = LocalDate(year, month + 1, day.dayOfMonth)
                totalRelease += profile.effectiveHours(date, day.tag)
            }
        }
        return totalRelease
    }

    // Сумма явно заданных часов «Технических занятий» за месяц (ReleaseType.
    // TechnicalStudy). Оплачивается по среднему часу отдельной строкой.
    fun MonthOfYear.getTechnicalStudyHours(): Double {
        var total = 0.0
        this.days.forEach { day ->
            if (day.isReleaseDay && day.releaseType == ReleaseType.TechnicalStudy) {
                total += day.hours ?: 0.0
            }
        }
        return total
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

    fun MonthOfYear.getStandardNormaHours(): Int =
        getStandardNormaHours(WorkScheduleProfile.standard())

    fun MonthOfYear.getStandardNormaHours(profile: WorkScheduleProfile): Int {
        var normaOfMonth = 0
        this.days.forEach { day ->
            val date = LocalDate(year, month + 1, day.dayOfMonth)
            normaOfMonth += profile.effectiveHours(date, day.tag)
        }
        return normaOfMonth
    }

    fun MonthOfYear.getNormaHoursInDate(dateInMillis: Long): Int {
        val tz = TimeZone.currentSystemDefault()
        val currentDate = Instant.fromEpochMilliseconds(dateInMillis).toLocalDateTime(tz)
        var normaOfMonth = 0
        if (currentDate.monthNumber - 1 == this.month) {
            this.days.forEach { day ->
                if (day.reducesNorma()) {
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
            if (!day.reducesNorma()) {
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
