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
    val totalMinutes = offsetInMillis / 60_000L
    val sign = if (totalMinutes >= 0) "+" else "-"
    val absoluteMinutes = kotlin.math.abs(totalMinutes)
    val hours = absoluteMinutes / 60
    val minutes = absoluteMinutes % 60
    return if (minutes == 0L) {
        "GMT$sign$hours"
    } else {
        "GMT$sign${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
    }
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
