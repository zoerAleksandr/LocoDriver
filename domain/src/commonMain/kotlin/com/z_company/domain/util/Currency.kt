package com.z_company.domain.util

/**
 * Символ валюты по коду страны из настроек приложения ([com.z_company.domain
 * .entities.setting.UserSettings.country]). Поддерживаются те же страны, что и
 * в остальном приложении: RU / KZ / BY.
 *
 * Важно: это касается только денег пользователя (зарплата, расчёт маршрута,
 * статистика). Цены подписок (RuStore) всегда в рублях — их не трогаем.
 */
fun currencySymbol(country: String?): String = when (country?.uppercase()) {
    "KZ" -> "₸"   // казахстанский тенге
    "BY" -> "Br"  // белорусский рубль
    else -> "₽"   // RU и значение по умолчанию
}
