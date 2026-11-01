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
    /**
     * Норма по конкретным датам (календарь нормы, «Своя настройка»): ключ — ISO-дата `yyyy-MM-dd`, значение — часы 0..[MAX_CUSTOM_DAY_HOURS].
     * Хранятся только изменённые пользователем дни; остальные считаются по дням недели.
     *
     * Локальные данные: в `PUT /v1/work_schedule/` не уходят (контракт сервера не менялся) и
     * при приёме профиля с сервера сохраняются (см. SyncManager).
     */
    val customDays: Map<String, Int> = emptyMap(),
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

    /**
     * Норма на дату с учётом личного «Выходного» ([isDayOff]): в режиме «Своя настройка»
     * такой день нормы не имеет (как «В» в бумажном графике), в остальных режимах
     * «Выходной» норму не меняет — это перенос выходного дня.
     */
    fun effectiveHours(date: LocalDate, tag: TagForDay, isDayOff: Boolean): Int =
        if (isDayOff && mode == WorkScheduleMode.CUSTOM) 0 else effectiveHours(date, tag)

    fun effectiveHours(date: LocalDate, tag: TagForDay): Int {
        // Правки пользователя по дням действуют при любой выбранной неделе (праздник всегда 0).
        if (tag != TagForDay.HOLIDAY) {
            customDays[date.toString()]?.let { return it }
        }
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

    /** Норма на дату, заданная пользователем в календаре нормы; null — считается по дню недели. */
    fun customHoursFor(date: LocalDate): Int? = customDays[date.toString()]

    /** Задать норму на дату ([hours] = null — вернуть значение по умолчанию). */
    fun withCustomDay(date: LocalDate, hours: Int?): WorkScheduleProfile {
        val key = date.toString()
        val days = if (hours == null) customDays - key
        else customDays + (key to hours.coerceIn(0, MAX_CUSTOM_DAY_HOURS))
        return copy(mode = WorkScheduleMode.CUSTOM, customDays = days)
    }

    /** Убрать правки по дням за один месяц ([month] 1..12). */
    fun withoutCustomDaysOfMonth(year: Int, month: Int): WorkScheduleProfile {
        val prefix = LocalDate(year, month, 1).toString().substring(0, 8) // "yyyy-MM-"
        return copy(customDays = customDays.filterKeys { !it.startsWith(prefix) })
    }

    /** Профиль в том виде, в котором он хранится на сервере (без локальных данных). */
    fun withoutCustomDays(): WorkScheduleProfile =
        if (customDays.isEmpty()) this else copy(customDays = emptyMap())

    companion object {
        const val MAX_CUSTOM_DAY_HOURS = 12

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
