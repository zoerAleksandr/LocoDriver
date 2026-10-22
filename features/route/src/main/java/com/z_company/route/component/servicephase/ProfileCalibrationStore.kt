package com.z_company.route.component.servicephase

import android.content.Context

/**
 * Ручные поправки положения на профиле (км по оси) по участку и хэшу его геометрии. Хранятся на устройстве и
 * на сервер не уходят; новая привязка заменяет прежнюю, «Сбросить» удаляет.
 */
internal class ProfileCalibrationStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("profile_calibration", Context.MODE_PRIVATE)

    private fun key(sectionId: String, axisHash: String) = "$sectionId:$axisHash"

    fun get(sectionId: String, axisHash: String): Double =
        runCatching { prefs.getFloat(key(sectionId, axisHash), 0f).toDouble() }.getOrDefault(0.0)

    fun set(sectionId: String, axisHash: String, shiftKm: Double) {
        val k = key(sectionId, axisHash)
        runCatching {
            if (kotlin.math.abs(shiftKm) < 1e-6) prefs.edit().remove(k).apply() else prefs.edit().putFloat(k, shiftKm.toFloat()).apply()
        }
    }
}
