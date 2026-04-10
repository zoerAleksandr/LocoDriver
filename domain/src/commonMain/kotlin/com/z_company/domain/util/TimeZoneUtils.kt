package com.z_company.domain.util

import com.z_company.domain.entities.setting.UserSettings

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

/**
 * Единый центр принятия решений о часовом поясе отображения/ввода времени.
 *
 * Россия (RU) и Беларусь (BY): все времена вводятся по московскому (GMT+3).
 * Казахстан (KZ): времена вводятся по местному (KZT = GMT+5).
 *
 * Используется в DateAndTimeConverter (отображение) и DateTimePickerBottomSheet (ввод).
 */
fun UserSettings.displayTimeZone(): String =
    if (country == "KZ") getTimeZone(timeZone) else "GMT+3"
