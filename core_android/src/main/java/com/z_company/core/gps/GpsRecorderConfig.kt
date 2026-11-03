package com.z_company.core.gps

import androidx.annotation.DrawableRes

/**
 * Параметры постоянного уведомления foreground service. Компонент записи ничего не знает о
 * потребителе (поезд, навигатор) — тексты задаёт тот, кто запускает запись.
 */
data class GpsRecorderConfig(
    val notificationTitle: String,
    val notificationText: String,
    val stopActionLabel: String,
    @DrawableRes val smallIconRes: Int,
    val channelName: String = "Запись GPS",
)
