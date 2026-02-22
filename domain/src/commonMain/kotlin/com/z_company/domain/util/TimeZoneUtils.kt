package com.z_company.domain.util

/**
 * Конвертирует смещение часового пояса (в миллисекундах относительно Москвы, UTC+3)
 * в строковый идентификатор TimeZone формата "GMT+N".
 *
 * Используется вместо SettingsUseCase.getTimeZone() в domain-слое,
 * чтобы не привносить зависимость от Koin/DI-инфраструктуры.
 */
fun getTimeZone(timeZoneInMillis: Long = 0L): String {
    val offsetInMillis = timeZoneInMillis + 10_800_000L
    val offset = offsetInMillis / 3_600_000L
    return "GMT+$offset"
}
