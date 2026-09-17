package com.z_company.domain.salary

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * Оплата недоработки начисляется только по закрытому месяцу: прошлый месяц —
 * всегда, текущий — только в его последний календарный день. В середине
 * месяца недоработка не показывается (норма «на дату» для неё не используется).
 * Общее правило для Android, iOS и PWA (`effectiveNormaHours` в payrollBridge).
 *
 * @param month0 месяц 0-based, как в MonthOfYear.
 * @return true, если для месяца нужна полная норма для расчёта недоработки.
 */
fun isUnderworkPeriodClosed(year: Int, month0: Int, today: LocalDate): Boolean {
    val selectedIndex = year * 12 + month0
    val currentIndex = today.year * 12 + today.monthNumber - 1
    if (selectedIndex < currentIndex) return true
    if (selectedIndex > currentIndex) return false
    val lastDay = LocalDate(today.year, today.monthNumber, 1)
        .plus(1, DateTimeUnit.MONTH).toEpochDays() - 1
    return today.toEpochDays() == lastDay
}
