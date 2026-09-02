package com.z_company.domain.util

import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

/**
 * Защита от «31 сентября».
 *
 * В локальной БД у части пользователей лежат MonthOfYear, у которых список
 * days не соответствует месяцу (например, сентябрь с 31 днём — наследие
 * дублей/старых импортов; дедупликация `maxByOrNull { it.days.size }` ещё и
 * предпочитает такую запись как «более полную»). Прямой вызов
 * `LocalDate(year, month + 1, day.dayOfMonth)` на таком дне бросает
 * IllegalArgumentException и роняет приложение на старте.
 *
 * Поэтому день месяца всегда превращаем в дату через [dateOfDayOrNull], а при
 * чтении из БД чистим список дней через [withValidDays].
 */

/** Число дней в месяце. [month] — 0-based, как в [MonthOfYear]. 0, если месяц некорректен. */
fun daysInMonth(year: Int, month: Int): Int {
    if (month !in 0..11) return 0
    return runCatching {
        val first = LocalDate(year, month + 1, 1)
        first.daysUntil(first.plus(1, DateTimeUnit.MONTH))
    }.getOrDefault(0)
}

/** Дата дня месяца или null, если такого дня в этом месяце не существует. */
fun dateOfDayOrNull(year: Int, month: Int, dayOfMonth: Int): LocalDate? {
    if (dayOfMonth < 1 || dayOfMonth > daysInMonth(year, month)) return null
    return runCatching { LocalDate(year, month + 1, dayOfMonth) }.getOrNull()
}

/** Дата дня этого месяца или null, если такого дня в месяце не существует. */
fun MonthOfYear.dateOfDayOrNull(dayOfMonth: Int): LocalDate? =
    dateOfDayOrNull(year, month, dayOfMonth)

/** Только те дни, которые реально существуют в этом месяце (без дублей номеров). */
fun List<Day>.validFor(year: Int, month: Int): List<Day> {
    val max = daysInMonth(year, month)
    if (max == 0) return emptyList()
    val seen = mutableSetOf<Int>()
    return filter { it.dayOfMonth in 1..max && seen.add(it.dayOfMonth) }
}

/** Копия месяца без «несуществующих» дней — применяется при чтении из БД. */
fun MonthOfYear.withValidDays(): MonthOfYear {
    val valid = days.validFor(year, month)
    return if (valid.size == days.size) this else copy(days = valid)
}
