package com.z_company.core.gps

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

/**
 * Foreground service записи GPS (тип location). Данные не пишет на диск и не логирует:
 * точки уходят только в [GpsRecorder.points]. Настройки уведомления сохраняются в prefs,
 * чтобы переживать перезапуск службы системой (START_STICKY).
 */
class GpsRecordingService : Service() {

    private var listener: LocationListener? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                shutdown()
                return START_NOT_STICKY
            }
            ACTION_START -> savePrefs(intent)
        }
        // intent == null — системный перезапуск после убийства процесса: берём сохранённое.
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (intent == null && !prefs.getBoolean(KEY_ACTIVE, false)) {
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            ServiceCompat.startForeground(
                this, NOTIFICATION_ID, buildNotification(prefs),
                if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0,
            )
        } catch (e: Exception) {
            // Нельзя стартовать из фона / нет разрешения: выключаемся, потребитель увидит isRunning=false.
            shutdown()
            return START_NOT_STICKY
        }
        if (listener == null && !beginUpdates()) {
            shutdown()
            return START_NOT_STICKY
        }
        prefs.edit().putBoolean(KEY_ACTIVE, true).apply()
        GpsRecorder.setRunning(true)
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun beginUpdates(): Boolean {
        if (!GpsPermissions.hasLocation(this)) return false
        val lm = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        val l = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                GpsRecorder.emit(location.toPoint())
            }

            override fun onProviderEnabled(provider: String) {
                if (provider == LocationManager.GPS_PROVIDER) GpsRecorder.setGpsAvailable(true)
            }

            override fun onProviderDisabled(provider: String) {
                if (provider == LocationManager.GPS_PROVIDER) GpsRecorder.setGpsAvailable(false)
            }
        }
        return try {
            lm.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, UPDATE_INTERVAL_MS, 0f, l, Looper.getMainLooper(),
            )
            GpsRecorder.setGpsAvailable(lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
            listener = l
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun removeUpdates() {
        listener?.let { l ->
            (getSystemService(Context.LOCATION_SERVICE) as? LocationManager)?.removeUpdates(l)
        }
        listener = null
    }

    private fun shutdown() {
        removeUpdates()
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ACTIVE, false).apply()
        GpsRecorder.setRunning(false)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        removeUpdates()
        super.onDestroy()
    }

    private fun savePrefs(intent: Intent) {
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(EXTRA_TITLE, intent.getStringExtra(EXTRA_TITLE))
            .putString(EXTRA_TEXT, intent.getStringExtra(EXTRA_TEXT))
            .putString(EXTRA_STOP_LABEL, intent.getStringExtra(EXTRA_STOP_LABEL))
            .putString(EXTRA_CHANNEL, intent.getStringExtra(EXTRA_CHANNEL))
            .putInt(EXTRA_ICON, intent.getIntExtra(EXTRA_ICON, 0))
            .apply()
    }

    private fun buildNotification(prefs: SharedPreferences): Notification {
        val channelName = prefs.getString(EXTRA_CHANNEL, null) ?: "Запись GPS"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_LOW),
            )
        }
        val open = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, GpsRecordingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val icon = prefs.getInt(EXTRA_ICON, 0).takeIf { it != 0 } ?: android.R.drawable.ic_menu_mylocation
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(prefs.getString(EXTRA_TITLE, null))
            .setContentText(prefs.getString(EXTRA_TEXT, null))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(open)
            .addAction(0, prefs.getString(EXTRA_STOP_LABEL, null) ?: "Выключить", stop)
            .build()
    }

    private fun Location.toPoint() = GpsPoint(
        timeMs = time,
        latitude = latitude,
        longitude = longitude,
        speedMps = if (hasSpeed()) speed else null,
        accuracyM = if (hasAccuracy()) accuracy else null,
    )

    companion object {
        const val ACTION_START = "com.z_company.core.gps.START"
        const val ACTION_STOP = "com.z_company.core.gps.STOP"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TEXT = "text"
        const val EXTRA_STOP_LABEL = "stop_label"
        const val EXTRA_ICON = "icon"
        const val EXTRA_CHANNEL = "channel"
        private const val PREFS = "gps_recorder"
        private const val KEY_ACTIVE = "active"
        private const val CHANNEL_ID = "gps_recording"
        private const val NOTIFICATION_ID = 7401
        private const val UPDATE_INTERVAL_MS = 1_000L
    }
}
