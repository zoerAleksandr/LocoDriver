package com.z_company.route.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import androidx.core.util.Consumer
import com.z_company.domain.util.GeoFix
import android.location.LocationListener
import android.os.Bundle
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/**
 * Разовая координата для подстановки станции плеча (SCREEN_SPECS §6.6). Только если
 * разрешение уже выдано; без Google Play Services. Координата не сохраняется и никуда
 * не отправляется.
 */
object DeviceLocation {
    private val executor = Executors.newSingleThreadExecutor()

    /** Свежесть последней известной точки, при которой можно не ждать новую. */
    private const val FRESH_MS = 60_000L

    fun hasPermission(context: Context): Boolean =
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    @SuppressLint("MissingPermission")
    suspend fun currentFix(context: Context, timeoutMs: Long = 5_000): GeoFix? {
        if (!hasPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).filter {
            runCatching { lm.isProviderEnabled(it) }.getOrDefault(false)
        }
        if (providers.isEmpty()) return null
        val last = providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.elapsedRealtimeNanos }
        if (last != null && ageMs(last) < FRESH_MS) return last.toFix()
        val fresh = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Location?> { cont ->
                val signal = CancellationSignal()
                cont.invokeOnCancellation { signal.cancel() }
                LocationManagerCompat.getCurrentLocation(
                    lm, providers.first(), signal, executor,
                    Consumer<Location?> { loc -> if (cont.isActive) cont.resume(loc) },
                )
            }
        }
        return (fresh ?: last)?.toFix()
    }

    /** Живое положение: скорость (км/ч) и курс, если провайдер их сообщает. Координаты не логируются и не сохраняются. */
    data class LiveFix(
        val latitude: Double,
        val longitude: Double,
        val speedKmh: Double?,
        val bearing: Float?,
        val accuracyM: Float?,
    )

    /**
     * Подписка на обновления положения (раз в [intervalMs]), пока коллектор жив: при отмене подписка снимается —
     * экономия батареи, обновления идут только пока открыта карта поезда. Пусто и сразу закрывается, если нет
     * разрешения или включённых провайдеров (GPS, иначе сеть).
     */
    @SuppressLint("MissingPermission")
    fun updates(context: Context, intervalMs: Long = 1_000L): Flow<LiveFix> = callbackFlow {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val provider = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).firstOrNull {
            hasPermission(context) && runCatching { lm?.isProviderEnabled(it) }.getOrNull() == true
        }
        if (lm == null || provider == null) {
            close()
            return@callbackFlow
        }
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(
                    LiveFix(
                        location.latitude, location.longitude,
                        if (location.hasSpeed()) location.speed * 3.6 else null,
                        providerBearing(location),
                        if (location.hasAccuracy()) location.accuracy else null,
                    )
                )
            }
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }
        runCatching { lm.requestLocationUpdates(provider, intervalMs, 0f, listener, Looper.getMainLooper()) }
            .onFailure { close() }
        awaitClose { runCatching { lm.removeUpdates(listener) } }
    }

    /**
     * Курс провайдера. Эмулятор и часть mock-провайдеров сообщают `hasBearing() = true` (даже с точностью курса) и курсом
     * ровно 0.0: такой «курс» ненадёжен (стрелка стояла на север) — считаем его неизвестным, и курс вычисляется по двум
     * последним точкам (HeadingEstimator).
     */
    internal fun providerBearing(l: Location): Float? {
        if (!l.hasBearing()) return null
        if (l.bearing == 0f) return null
        return l.bearing
    }

    private fun ageMs(l: Location) = (SystemClock.elapsedRealtimeNanos() - l.elapsedRealtimeNanos) / 1_000_000

    private fun Location.toFix() = GeoFix(latitude, longitude, if (hasAccuracy()) accuracy else null)
}
