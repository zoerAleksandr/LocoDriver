package com.z_company.domain.entities

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
enum class WorkScheduleMode {
    STANDARD,
    SIX_DAY_7_5,
    CUSTOM,
}

/**
 * Локальный профиль продолжительности рабочей недели.
 *
 * Не входит в [com.z_company.domain.entities.setting.UserSettings] и не
 * синхронизируется с сервером: производственный календарь остаётся общим, а
 * продолжительность дня является личной настройкой пользователя.
 */
@Serializable
data class WorkScheduleProfile(
    val mode: WorkScheduleMode = WorkScheduleMode.STANDARD,
    val mondayHours: Int = 8,
    val tuesdayHours: Int = 8,
    val wednesdayHours: Int = 8,
    val thursdayHours: Int = 8,
    val fridayHours: Int = 8,
    val saturdayHours: Int = 0,
    val sundayHours: Int = 0,
    /** Клиентская версия для LWW-синхронизации между устройствами. */
    val updatedAt: Long = 0L,
) {
    fun hoursFor(dayOfWeek: DayOfWeek): Int = when (dayOfWeek) {
        DayOfWeek.MONDAY -> mondayHours
        DayOfWeek.TUESDAY -> tuesdayHours
        DayOfWeek.WEDNESDAY -> wednesdayHours
        DayOfWeek.THURSDAY -> thursdayHours
        DayOfWeek.FRIDAY -> fridayHours
        DayOfWeek.SATURDAY -> saturdayHours
        DayOfWeek.SUNDAY -> sundayHours
    }

    fun effectiveHours(date: LocalDate, tag: TagForDay): Int {
        if (mode == WorkScheduleMode.STANDARD) {
            return when (tag) {
                TagForDay.WORKING_DAY -> 8
                TagForDay.SHORTENED_DAY -> 7
                TagForDay.NON_WORKING_DAY, TagForDay.HOLIDAY -> 0
            }
        }
        return when (tag) {
            TagForDay.HOLIDAY -> 0
            TagForDay.SHORTENED_DAY -> (hoursFor(date.dayOfWeek) - 1).coerceAtLeast(0)
            TagForDay.WORKING_DAY -> hoursFor(date.dayOfWeek)
            // Для индивидуального графика обычная суббота может быть рабочей.
            // Отдельные официальные праздники имеют тег HOLIDAY и остаются нулевыми.
            TagForDay.NON_WORKING_DAY -> hoursFor(date.dayOfWeek)
        }
    }

    fun withHours(dayOfWeek: DayOfWeek, hours: Int): WorkScheduleProfile {
        val safeHours = hours.coerceIn(0, 24)
        return when (dayOfWeek) {
            DayOfWeek.MONDAY -> copy(mode = WorkScheduleMode.CUSTOM, mondayHours = safeHours)
            DayOfWeek.TUESDAY -> copy(mode = WorkScheduleMode.CUSTOM, tuesdayHours = safeHours)
            DayOfWeek.WEDNESDAY -> copy(mode = WorkScheduleMode.CUSTOM, wednesdayHours = safeHours)
            DayOfWeek.THURSDAY -> copy(mode = WorkScheduleMode.CUSTOM, thursdayHours = safeHours)
            DayOfWeek.FRIDAY -> copy(mode = WorkScheduleMode.CUSTOM, fridayHours = safeHours)
            DayOfWeek.SATURDAY -> copy(mode = WorkScheduleMode.CUSTOM, saturdayHours = safeHours)
            DayOfWeek.SUNDAY -> copy(mode = WorkScheduleMode.CUSTOM, sundayHours = safeHours)
        }
    }

    companion object {
        fun standard(): WorkScheduleProfile = WorkScheduleProfile()

        fun sixDaySevenFive(): WorkScheduleProfile = WorkScheduleProfile(
            mode = WorkScheduleMode.SIX_DAY_7_5,
            mondayHours = 7,
            tuesdayHours = 7,
            wednesdayHours = 7,
            thursdayHours = 7,
            fridayHours = 7,
            saturdayHours = 5,
            sundayHours = 0,
        )
    }
}
