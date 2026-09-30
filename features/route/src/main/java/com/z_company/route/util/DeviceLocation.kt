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

    private fun ageMs(l: Location) = (SystemClock.elapsedRealtimeNanos() - l.elapsedRealtimeNanos) / 1_000_000

    private fun Location.toFix() = GeoFix(latitude, longitude, if (hasAccuracy()) accuracy else null)
}
