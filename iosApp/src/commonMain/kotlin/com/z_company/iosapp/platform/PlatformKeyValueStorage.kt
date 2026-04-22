package com.z_company.iosapp.platform

/**
 * Простое key-value хранилище, имитирующее Android SharedPreferences.
 * На iOS реализуется через NSUserDefaults (actual в iosMain).
 */
expect class PlatformKeyValueStorage() {
    fun getBoolean(key: String, default: Boolean): Boolean
    fun setBoolean(key: String, value: Boolean)

    fun getLong(key: String, default: Long): Long
    fun setLong(key: String, value: Long)

    fun getString(key: String): String?
    fun setString(key: String, value: String?)

    fun getStringSet(key: String): Set<String>?
    fun setStringSet(key: String, values: Set<String>)

    fun getLongList(key: String): List<Long>
    fun setLongList(key: String, values: List<Long>)
}

/**
 * Описывает начальное состояние устройства при первом запуске:
 *  - страна (RU/KZ/BY, с fallback на RU)
 *  - смещение от Москвы в миллисекундах, округлённое до часа
 */
data class InitialCountryAndOffset(val country: String, val offsetFromMoscowMs: Long)

/** Определение страны пользователя и часового пояса при первом запуске. */
expect fun detectInitialCountryAndOffset(): InitialCountryAndOffset
