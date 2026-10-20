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
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.SystemClock
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.z_company.domain.gnss.GnssQuality
import com.z_company.domain.gnss.GnssQualityEvaluator
import com.z_company.domain.gnss.GpsFixDiagnostics
import com.z_company.domain.track.AltitudeMath
import com.z_company.domain.util.DriftFilter
import com.z_company.domain.util.GnssPlausibility

/**
 * Foreground service записи GPS (тип location). Данные не пишет на диск и не логирует:
 * точки уходят только в [GpsRecorder.points]. Настройки уведомления сохраняются в prefs,
 * чтобы переживать перезапуск службы системой (START_STICKY).
 */
class GpsRecordingService : Service() {

    private var listener: LocationListener? = null
    private var pressureListener: SensorEventListener? = null

    /** Последнее показание барометра, гПа; null - датчика нет или показаний ещё не было. */
    @Volatile
    private var pressureHpa: Float? = null

    // Качество GNSS (SCREEN_SPECS §6.9): статус спутников + возраст/точность последнего fix, раз в секунду.
    private var gnssCallback: GnssStatus.Callback? = null
    private val qualityEvaluator = GnssQualityEvaluator()
    private val qualityHandler = Handler(Looper.getMainLooper())
    private var qualityStartedMs = 0L
    private var lastFixElapsedMs: Long? = null
    private var lastFixAccuracyM: Float? = null
    private var satsUsed: Int? = null
    private var satsVisible: Int? = null

    // Подмена/скачки GNSS: отвергнутые фиксы не уходят в GpsRecorder.points (SCREEN_SPECS §6.9).
    private val plausibility = GnssPlausibility()

    // Дрейф на стоянке: ДО GnssPlausibility; позиция дрейфующего фикса заменяется якорем стоянки (SCREEN_SPECS §6.9).
    private val drift = DriftFilter()
    private var networkRefHeld = false

    // Диагностика дыр без координат (SCREEN_SPECS §6.9): счётчики отказов по причинам, дыры, возраст последнего фикса;
    // сохраняется в prefs gps_recorder (читается через run-as).
    private val diag = GpsFixDiagnostics()
    private var networkIgnored = 0
    private var tickCount = 0
    private val qualityTick = object : Runnable {
        override fun run() {
            publishQuality()
            if (++tickCount % DIAG_SAVE_EVERY_TICKS == 0) saveDiagnostics()
            qualityHandler.postDelayed(this, QUALITY_TICK_MS)
        }
    }

    private fun publishQuality() {
        GpsRecorder.setQuality(
            qualityEvaluator.update(
                nowMs = SystemClock.elapsedRealtime(),
                startedMs = qualityStartedMs,
                lastFixMs = lastFixElapsedMs,
                accuracyM = lastFixAccuracyM,
                satellitesUsed = satsUsed,
                satellitesVisible = satsVisible,
                providerEnabled = GpsRecorder.gpsAvailable.value,
                rejectedRecent = plausibility.rejectedCount(SystemClock.elapsedRealtime()),
            ),
        )
    }

    @SuppressLint("MissingPermission")
    private fun startQuality(lm: LocationManager) {
        qualityEvaluator.reset()
        plausibility.reset()
        drift.reset()
        diag.reset()
        networkIgnored = 0
        if (!networkRefHeld) {
            NetworkLocationReference.acquire(this)
            networkRefHeld = true
        }
        qualityStartedMs = SystemClock.elapsedRealtime()
        lastFixElapsedMs = null
        lastFixAccuracyM = null
        satsUsed = null
        satsVisible = null
        val cb = object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatus) {
                var used = 0
                for (i in 0 until status.satelliteCount) if (status.usedInFix(i)) used++
                satsUsed = used
                satsVisible = status.satelliteCount
            }

            override fun onStopped() {
                satsUsed = 0
            }
        }
        val ok = runCatching { lm.registerGnssStatusCallback(cb, qualityHandler) }.getOrDefault(false)
        if (ok) gnssCallback = cb
        qualityHandler.removeCallbacks(qualityTick)
        qualityHandler.post(qualityTick)
    }

    private fun saveDiagnostics() {
        runCatching {
            getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt("diag_delivered", diag.delivered)
                .putInt("diag_accepted", diag.accepted)
                .putInt("diag_net_ignored", networkIgnored)
                .putString("diag_rejected", diag.rejectedTotals().entries.joinToString(",") { "${it.key}:${it.value}" })
                .putLong("diag_max_gap_s", diag.maxGapMs / 1000)
                .putLong("diag_last_delivery_wall_ms", diag.lastDeliveryWallMs ?: 0L)
                .putLong("diag_last_accepted_wall_ms", diag.lastAcceptedWallMs ?: 0L)
                .putString("diag_gaps", diag.recentGaps().joinToString("\n") { it.serialize() })
                .apply()
        }
    }

    private fun stopQuality() {
        saveDiagnostics()
        if (networkRefHeld) {
            NetworkLocationReference.release()
            networkRefHeld = false
        }
        qualityHandler.removeCallbacks(qualityTick)
        gnssCallback?.let { cb ->
            runCatching { (getSystemService(Context.LOCATION_SERVICE) as? LocationManager)?.unregisterGnssStatusCallback(cb) }
        }
        gnssCallback = null
        GpsRecorder.setQuality(GnssQuality())
    }

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
                val nowElapsed = SystemClock.elapsedRealtime()
                val wallNow = System.currentTimeMillis()
                diag.onDelivered(wallNow)
                // Подмена времени (GNSS-спуфинг сдвигает часы на недели): такой фикс нельзя писать в трек.
                if (!GnssPlausibility.isTimeSane(location.time, wallNow)) {
                    diag.onRejected("TIME")
                    return
                }
                val accuracy = if (location.hasAccuracy()) location.accuracy else null
                val speedKmh = if (location.hasSpeed()) location.speed * 3.6 else null
                // Дрейф стоянки: ничего не отбрасывается, позиция дрейфующего фикса = якорь (held), скорость/время исходные.
                val steady = drift.process(
                    DriftFilter.Fix(location.time, location.latitude, location.longitude, speedKmh, accuracy, providerBearing(location)),
                )
                val verdict = plausibility.check(
                    timeMs = location.time,
                    lat = steady.latitude,
                    lon = steady.longitude,
                    speedKmh = speedKmh,
                    accuracyM = accuracy,
                    network = NetworkLocationReference.current(),
                    nowMs = nowElapsed,
                )
                // Подмена/скачок: точку не принимаем нигде (ни в трек, ни в детектор, ни в «текущую позицию»).
                if (!verdict.accepted) {
                    diag.onRejected(verdict.reason.name)
                    return
                }
                if (verdict.networkIgnored) networkIgnored++
                if (diag.onAccepted(wallNow, accuracy) != null) saveDiagnostics() // дыра - сразу на диск
                lastFixElapsedMs = nowElapsed
                lastFixAccuracyM = accuracy
                GpsRecorder.emit(
                    location.toPoint(baroAltitudeM()).copy(latitude = steady.latitude, longitude = steady.longitude, held = steady.held),
                )
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
            startQuality(lm)
            startPressure()
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Барометр (если есть): только запоминаем последнее давление, высота считается при каждой точке. */
    private fun startPressure() {
        if (pressureListener != null) return
        val sm = getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
        val sensor = sm.getDefaultSensor(Sensor.TYPE_PRESSURE) ?: return
        val l = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                pressureHpa = event.values.firstOrNull()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        // Редкие обновления (SENSOR_DELAY_NORMAL): точке нужно значение раз в секунду, экономим батарею.
        if (sm.registerListener(l, sensor, SensorManager.SENSOR_DELAY_NORMAL)) pressureListener = l
    }

    private fun stopPressure() {
        pressureListener?.let { l ->
            (getSystemService(Context.SENSOR_SERVICE) as? SensorManager)?.unregisterListener(l)
        }
        pressureListener = null
        pressureHpa = null
    }

    /** Барометрическая высота по стандартной атмосфере; null - датчика нет / давление неправдоподобно. */
    private fun baroAltitudeM(): Double? {
        val hpa = pressureHpa
        if (!AltitudeMath.isPlausiblePressureHpa(hpa)) return null
        return AltitudeMath.sanitize(SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, hpa!!).toDouble())
    }

    private fun removeUpdates() {
        stopQuality()
        stopPressure()
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

    /** Курс датчика; 0.0 (эмулятор, mock) считается неизвестным. */
    private fun providerBearing(l: Location): Float? = if (l.hasBearing() && l.bearing != 0f) l.bearing else null

    private fun Location.toPoint(baroAltitudeM: Double?) = GpsPoint(
        timeMs = time,
        latitude = latitude,
        longitude = longitude,
        speedMps = if (hasSpeed()) speed else null,
        accuracyM = if (hasAccuracy()) accuracy else null,
        // Над уровнем моря (MSL) там, где система его отдаёт (API 34+), иначе высота над эллипсоидом WGS84.
        altitudeM = AltitudeMath.sanitize(
            when {
                Build.VERSION.SDK_INT >= 34 && hasMslAltitude() -> mslAltitudeMeters
                hasAltitude() -> altitude
                else -> null
            },
        ),
        baroAltitudeM = baroAltitudeM,
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
        private const val QUALITY_TICK_MS = 1_000L
        private const val DIAG_SAVE_EVERY_TICKS = 15
    }
}
