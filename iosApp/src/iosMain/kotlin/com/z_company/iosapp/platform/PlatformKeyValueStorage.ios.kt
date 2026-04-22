@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.iosapp.platform

import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.countryCode
import platform.Foundation.currentLocale

actual class PlatformKeyValueStorage actual constructor() {

    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun getBoolean(key: String, default: Boolean): Boolean =
        if (defaults.objectForKey(key) == null) default else defaults.boolForKey(key)

    actual fun setBoolean(key: String, value: Boolean) {
        defaults.setBool(value, forKey = key)
    }

    actual fun getLong(key: String, default: Long): Long =
        if (defaults.objectForKey(key) == null) default else defaults.integerForKey(key)

    actual fun setLong(key: String, value: Long) {
        defaults.setInteger(value, forKey = key)
    }

    actual fun getString(key: String): String? = defaults.stringForKey(key)

    actual fun setString(key: String, value: String?) {
        if (value == null) defaults.removeObjectForKey(key)
        else defaults.setObject(value, forKey = key)
    }

    actual fun getStringSet(key: String): Set<String>? {
        val raw = defaults.stringForKey(key) ?: return null
        if (raw.isEmpty()) return emptySet()
        return raw.split('\u0001').toSet()
    }

    actual fun setStringSet(key: String, values: Set<String>) {
        defaults.setObject(values.joinToString("\u0001"), forKey = key)
    }

    actual fun getLongList(key: String): List<Long> {
        val raw = defaults.stringForKey(key).orEmpty()
        if (raw.isEmpty()) return emptyList()
        return raw.split(',').mapNotNull { it.toLongOrNull() }
    }

    actual fun setLongList(key: String, values: List<Long>) {
        defaults.setObject(values.joinToString(","), forKey = key)
    }
}

actual fun detectInitialCountryAndOffset(): InitialCountryAndOffset {
    val deviceCountry: String = NSLocale.currentLocale.countryCode ?: "RU"
    val supported = listOf("RU", "KZ", "BY")
    val country = if (deviceCountry in supported) deviceCountry else "RU"

    val moscowOffsetMs = 3L * 3_600_000L
    val secondsFromGmt = TimeZone.currentSystemDefault()
        .offsetAt(Clock.System.now())
        .totalSeconds.toLong()
    val deviceOffsetMs = secondsFromGmt * 1_000L
    val offsetFromMoscow = deviceOffsetMs - moscowOffsetMs
    val oneHour = 3_600_000L
    val rounded = (offsetFromMoscow / oneHour) * oneHour
    return InitialCountryAndOffset(country = country, offsetFromMoscowMs = rounded)
}
