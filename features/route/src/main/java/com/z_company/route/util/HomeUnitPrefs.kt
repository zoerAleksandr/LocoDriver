package com.z_company.route.util

import android.content.Context

/**
 * «Всегда открывать последний» для плиток главного экрана (loco / train / passenger).
 * Хранится на устройстве (SharedPreferences `home_units_prefs`); переключается в шторке-списке
 * плитки и в Настройках → Основные.
 */
object HomeUnitPrefs {
    val types = listOf("loco", "train", "passenger")

    private const val FILE = "home_units_prefs"
    private fun key(type: String) = "always_open_last_$type"

    fun get(context: Context, type: String): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(key(type), false)

    fun set(context: Context, type: String, value: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(key(type), value).apply()
    }
}
