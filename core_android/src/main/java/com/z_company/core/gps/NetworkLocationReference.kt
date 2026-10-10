package com.z_company.core.gps

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import com.z_company.domain.util.GnssPlausibility

/**
 * Редкий опорный запрос сетевой позиции (вышки/Wi-Fi, `NETWORK_PROVIDER`) для проверки GNSS на подмену
 * ([GnssPlausibility]). Позиция только сверяется с GNSS и НИКУДА не пишется (ни в трек, ни на диск, ни в лог).
 * Интервал - раз в [INTERVAL_MS] (45 с), без минимальной дистанции; подписка общая (счётчик [acquire]/[release]):
 * запись GPS и открытая карта делят один запрос, при нуле потребителей запрос снимается. Новых разрешений не нужно
 * (ACCESS_FINE уже есть). Нет разрешения / провайдер выключен / авиарежим без Wi-Fi - позиции нет, [current] = null и
 * проверка работает по прежней логике (скорость/прыжок).
 */
object NetworkLocationReference {
    const val INTERVAL_MS = 45_000L

    private var refs = 0
    private var manager: LocationManager? = null
    private var last: Location? = null
    private var lastAtElapsedMs = 0L

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            last = location
            lastAtElapsedMs = SystemClock.elapsedRealtime()
        }

        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    @SuppressLint("MissingPermission")
    @Synchronized
    fun acquire(context: Context) {
        refs++
        if (refs != 1) return
        if (!GpsPermissions.hasLocation(context)) return
        val lm = context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        runCatching {
            if (!lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) return
            lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let {
                last = it
                lastAtElapsedMs = SystemClock.elapsedRealtime() -
                    ((SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos) / 1_000_000L).coerceAtLeast(0L)
            }
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, INTERVAL_MS, 0f, listener, Looper.getMainLooper())
            manager = lm
        }
    }

    @Synchronized
    fun release() {
        if (refs == 0) return
        refs--
        if (refs != 0) return
        runCatching { manager?.removeUpdates(listener) }
        manager = null
        last = null
    }

    /** Свежая сетевая позиция (возраст считает [GnssPlausibility]); null - нет. */
    @Synchronized
    fun current(): GnssPlausibility.NetworkFix? {
        val l = last ?: return null
        return GnssPlausibility.NetworkFix(
            l.latitude, l.longitude,
            if (l.hasAccuracy()) l.accuracy else null,
            SystemClock.elapsedRealtime() - lastAtElapsedMs,
        )
    }
}
