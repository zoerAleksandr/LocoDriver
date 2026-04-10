package com.z_company.domain.util

import com.z_company.domain.entities.setting.CrossMonthTimezone
import com.z_company.domain.entities.setting.UserSettings
import kotlinx.datetime.TimeZone

/**
 * Единый центр временных расчётов.
 * Инкапсулирует локальный часовой пояс пользователя и часовой пояс для расчёта
 * границ переходных маршрутов.
 *
 * Создаётся из [UserSettings] через [from] и передаётся в расчётные функции.
 */
data class TimeCalculationContext(
    /** Локальный ЧП пользователя (для ночных/праздничных часов). */
    val localTZ: TimeZone,
    /** ЧП для определения месячных границ переходных маршрутов. */
    val crossMonthTZ: TimeZone,
) {
    companion object {
        fun from(settings: UserSettings): TimeCalculationContext {
            val localTZ = TimeZone.of(getTimeZone(settings.timeZone))
            val crossMonthTZ = when (settings.crossMonthTimezone) {
                CrossMonthTimezone.MOSCOW -> TimeZone.of("GMT+3")
                CrossMonthTimezone.LOCAL -> localTZ
            }
            return TimeCalculationContext(localTZ = localTZ, crossMonthTZ = crossMonthTZ)
        }
    }
}
