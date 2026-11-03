package com.z_company.core.gps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Разрешения, нужные для записи GPS в foreground service. Фонового доступа не требуется. */
object GpsPermissions {

    fun hasLocation(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun hasNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** Что запросить у пользователя перед стартом (для ActivityResultContracts.RequestMultiplePermissions). */
    fun missing(context: Context): Array<String> = buildList {
        if (!hasLocation(context)) {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (!hasNotifications(context)) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()
}
