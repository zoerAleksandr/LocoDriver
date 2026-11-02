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
    /**
     * Время последней правки нормы по датам, по месяцам: ключ — `yyyy-MM` (месяц 1..12), значение — ms.
     * Единица LWW-синхронизации `GET/PUT /v1/work_schedule/days/`. Месяц, где правок не осталось (сброс),
     * хранится здесь же — чтобы сброс победил на других устройствах. Локальное поле, в `PUT /v1/work_schedule/` не уходит.
     */
    val customMonthStamps: Map<String, Long> = emptyMap(),
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
        if (customDays.isEmpty() && customMonthStamps.isEmpty()) this
        else copy(customDays = emptyMap(), customMonthStamps = emptyMap())

    /**
     * Проставить [now] месяцам, где норма по датам изменилась относительно [previous].
     * Вызывается на пользовательских правках (не при приёме данных с сервера).
     */
    fun stampChangedMonths(previous: WorkScheduleProfile, now: Long): WorkScheduleProfile {
        val before = previous.customDays.entries.groupBy({ monthKeyOf(it.key) }, { it.key to it.value })
            .mapValues { it.value.toMap() }
        val after = customDays.entries.groupBy({ monthKeyOf(it.key) }, { it.key to it.value })
            .mapValues { it.value.toMap() }
        val changed = (before.keys + after.keys).filter { before[it].orEmpty() != after[it].orEmpty() }
        if (changed.isEmpty()) return this
        return copy(customMonthStamps = customMonthStamps + changed.associateWith { now })
    }

    /** Месяцы с нормой по датам для отправки на сервер (месяц без правок, но с меткой, — сброс). */
    fun customMonths(): List<WorkScheduleMonth> {
        val byMonth = customDays.entries.groupBy({ monthKeyOf(it.key) }, { it.key to it.value })
        return (byMonth.keys + customMonthStamps.keys).sorted().map { key ->
            WorkScheduleMonth(
                monthKey = key,
                days = byMonth[key].orEmpty().toMap(),
                // Правки, сделанные до появления меток, — «старше любой метки», но новее отсутствия.
                updatedAt = customMonthStamps[key] ?: LEGACY_MONTH_STAMP,
            )
        }
    }

    /** Принять месяцы с сервера: дни и метка месяца заменяются целиком. */
    fun adoptMonths(months: List<WorkScheduleMonth>): WorkScheduleProfile {
        if (months.isEmpty()) return this
        val adopted = months.map { it.monthKey }.toSet()
        val keptDays = customDays.filterKeys { monthKeyOf(it) !in adopted }
        return copy(
            customDays = keptDays + months.flatMap { it.days.entries }.associate { it.key to it.value },
            customMonthStamps = customMonthStamps + months.associate { it.monthKey to it.updatedAt },
        )
    }

    companion object {
        const val MAX_CUSTOM_DAY_HOURS = 12

        /** Метка месяца, правленного до появления синхронизации: новее «нет на сервере», старше любой настоящей. */
        const val LEGACY_MONTH_STAMP = 1L

        /** `yyyy-MM-dd` → `yyyy-MM`. */
        fun monthKeyOf(isoDate: String): String = isoDate.take(7)

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

/**
 * Норма по датам одного месяца в обмене с сервером (`/v1/work_schedule/days/`).
 * [monthKey] — `yyyy-MM`, месяц 1..12 (не 0-based, как в остальном контракте): совпадает с префиксом ключей [days].
 */
@Serializable
data class WorkScheduleMonth(
    val monthKey: String,
    val days: Map<String, Int> = emptyMap(),
    val updatedAt: Long = 0L,
)

/** Результат сверки локальной нормы по датам с серверной (LWW по месяцам). */
data class CustomDaysSyncPlan(
    /** Месяцы, где локальная версия свежее: отправить на сервер. */
    val toPush: List<WorkScheduleMonth>,
    /** Месяцы, где серверная версия свежее: принять локально. */
    val toAdopt: List<WorkScheduleMonth>,
)

fun planCustomDaysSync(local: WorkScheduleProfile, remote: List<WorkScheduleMonth>): CustomDaysSyncPlan {
    val localByKey = local.customMonths().associateBy { it.monthKey }
    val remoteByKey = remote.associateBy { it.monthKey }
    val push = mutableListOf<WorkScheduleMonth>()
    val adopt = mutableListOf<WorkScheduleMonth>()
    (localByKey.keys + remoteByKey.keys).sorted().forEach { key ->
        val l = localByKey[key]
        val r = remoteByKey[key]
        when {
            r == null -> if (l != null) push += l
            l == null -> adopt += r
            l.updatedAt > r.updatedAt -> push += l
            l.updatedAt < r.updatedAt -> adopt += r
            // Метки равны, а содержимое разное (метка legacy и т.п.): серверная версия считается общей.
            l.days != r.days -> adopt += r
        }
    }
    return CustomDaysSyncPlan(toPush = push, toAdopt = adopt)
}
