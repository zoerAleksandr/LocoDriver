package com.z_company.route.autorecord

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import com.z_company.core.ResultState
import com.z_company.core.gps.GpsPermissions
import com.z_company.core.gps.GpsPoint
import com.z_company.core.gps.GpsRecorder
import com.z_company.core.gps.GpsRecorderConfig
import com.z_company.core.sendToSentry
import com.z_company.domain.autorecord.AutoStationLocator
import com.z_company.domain.autorecord.AutoTrainWriter
import com.z_company.domain.autorecord.MotionSample
import com.z_company.domain.autorecord.StopDetector
import com.z_company.domain.autorecord.StopEvent
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.UtilsForEntities.findCurrentRoute
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.StationDirectoryUseCase
import com.z_company.domain.use_cases.TrainUseCase
import com.z_company.domain.util.StationDirectory
import com.z_company.route.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Состояние кнопки «АВТО»: включена ли и до какого момента (автовыключение). */
data class AutoRecordState(val enabled: Boolean = false, val untilMs: Long = 0L)

/**
 * Кнопка «АВТО» (SCREEN_SPECS §6.2 п.6а): записывает стоянки и отправления текущего поезда по
 * скорости GPS. Первый потребитель переиспользуемого [GpsRecorder]. Состояние (вкл/время) живёт
 * в SharedPreferences устройства, на сервер не уходит; точки трека не хранятся, координаты не
 * логируются.
 */
class AutoRecordManager(
    private val context: Context,
    private val routeUseCase: RouteUseCase,
    private val trainUseCase: TrainUseCase,
    private val stationDirectory: StationDirectoryUseCase,
    private val prefs: SharedPreferences,
    private val scope: CoroutineScope,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
) {
    private val _state = MutableStateFlow(AutoRecordState())
    val state: StateFlow<AutoRecordState> = _state.asStateFlow()

    private var detector = StopDetector()
    private var pointsJob: Job? = null
    private var watchJob: Job? = null
    private var expiryJob: Job? = null
    private val writeLock = Mutex()

    /** Открытый экран «Поезд» регистрируется здесь, чтобы события шли в его форму, а не мимо неё. */
    private class Editor(val trainId: String, val apply: suspend (StopEvent, String?) -> Boolean)

    @Volatile
    private var editor: Editor? = null

    fun registerEditor(trainId: String, apply: suspend (StopEvent, String?) -> Boolean) {
        editor = Editor(trainId, apply)
    }

    fun unregisterEditor(trainId: String) {
        if (editor?.trainId == trainId) editor = null
    }

    /** Нужны ли разрешения перед [enable] (для запроса из UI). */
    fun missingPermissions(): Array<String> = GpsPermissions.missing(context)

    /**
     * Включить. Вызывать, когда приложение на экране и геолокация разрешена (иначе false).
     * Автовыключение — через [AUTO_OFF_MS].
     */
    fun enable(): Boolean {
        if (!GpsPermissions.hasLocation(context)) return false
        if (_state.value.enabled) return true
        val until = nowMs() + AUTO_OFF_MS
        prefs.edit().putLong(KEY_UNTIL, until).apply()
        _state.value = AutoRecordState(true, until)
        return begin(until)
    }

    fun disable() = stopInternal(notifyExpired = false)

    /** Из Application после старта графа: возобновить, если включено и не истекло. */
    fun restore() {
        val until = prefs.getLong(KEY_UNTIL, 0L)
        if (until == 0L) return
        if (nowMs() >= until) {
            prefs.edit().remove(KEY_UNTIL).apply()
            GpsRecorder.stop(context)
            return
        }
        _state.value = AutoRecordState(true, until)
        if (!begin(until)) stopInternal(notifyExpired = false)
    }

    private fun begin(until: Long): Boolean {
        detector = StopDetector()
        pointsJob?.cancel()
        watchJob?.cancel()
        expiryJob?.cancel()

        pointsJob = scope.launch {
            GpsRecorder.points.collect { onPoint(it) }
        }
        // Служба остановилась снаружи (кнопка «Выключить» в уведомлении) — выключаемся и мы.
        watchJob = scope.launch {
            var sawRunning = false
            GpsRecorder.isRunning.collect { running ->
                if (running) sawRunning = true
                else if (sawRunning) {
                    stopInternal(notifyExpired = false, stopService = false)
                }
            }
        }
        expiryJob = scope.launch {
            // Служба не поднялась (например, нельзя стартовать из фона) — не оставляем «включено» без GPS.
            delay(START_TIMEOUT_MS)
            if (_state.value.enabled && !GpsRecorder.isRunning.value) stopInternal(notifyExpired = false)
            val left = until - nowMs()
            if (left > 0) delay(left)
            if (_state.value.enabled) stopInternal(notifyExpired = true)
        }
        return try {
            GpsRecorder.start(context, recorderConfig(until))
            true
        } catch (e: Exception) {
            // ForegroundServiceStartNotAllowedException и т. п. — без координат и ПДн.
            e.sendToSentry("AutoRecordManager", "start")
            false
        }
    }

    private fun stopInternal(notifyExpired: Boolean, stopService: Boolean = true) {
        pointsJob?.cancel()
        watchJob?.cancel()
        expiryJob?.cancel()
        prefs.edit().remove(KEY_UNTIL).apply()
        val wasEnabled = _state.value.enabled
        _state.value = AutoRecordState()
        if (stopService) GpsRecorder.stop(context)
        if (wasEnabled && notifyExpired) showExpiredNotification()
    }

    private suspend fun onPoint(p: GpsPoint) {
        val s = _state.value
        if (!s.enabled) return
        if (nowMs() >= s.untilMs) {
            stopInternal(notifyExpired = true)
            return
        }
        val event = detector.onSample(
            MotionSample(p.timeMs, p.latitude, p.longitude, p.speedKmh, p.accuracyM),
        ) ?: return
        try {
            writeLock.withLock { handleEvent(event) }
        } catch (e: Exception) {
            e.sendToSentry("AutoRecordManager", "handleEvent")
        }
    }

    private suspend fun handleEvent(event: StopEvent) {
        val name = withContext(Dispatchers.Default) {
            val dir = stationDirectory.stations()
            AutoStationLocator.nearestName(
                StationDirectory.points(dir), StationDirectory.names(dir),
                event.latitude, event.longitude,
            )
        }
        val route = withContext(Dispatchers.Default) {
            routeUseCase.getListRoutes().findCurrentRoute(nowMs(), UserSettings())
        } ?: return // нет текущего маршрута — писать некуда
        val train = route.trains.lastOrNull() ?: Train(basicId = route.basicData.id)

        val open = editor
        if (open != null && open.trainId == train.trainId) {
            // Форма «Поезд» открыта на этом поезде: пишем через неё, иначе её автосохранение затрёт запись.
            if (withContext(Dispatchers.Main) { open.apply(event, name) }) return
        }
        val updated = AutoTrainWriter.apply(train, event, name) ?: return
        withContext(Dispatchers.Default) {
            trainUseCase.updateTrain(updated).first { it !is ResultState.Loading }
        }
    }

    private fun recorderConfig(until: Long) = GpsRecorderConfig(
        notificationTitle = "LocoDriver",
        notificationText = "АВТО включено до ${formatTime(until)}",
        stopActionLabel = "Выключить",
        smallIconRes = R.drawable.ic_stat_auto,
        channelName = "Запись АВТО",
    )

    private fun showExpiredNotification() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(EXPIRED_CHANNEL, "АВТО: автовыключение", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
        val n = NotificationCompat.Builder(context, EXPIRED_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_auto)
            .setContentTitle("LocoDriver")
            .setContentText("АВТО выключено: прошло 12 часов")
            .setAutoCancel(true)
            .build()
        runCatching { nm.notify(EXPIRED_ID, n) } // без разрешения на уведомления — молча
    }

    companion object {
        const val AUTO_OFF_MS = 12 * 60 * 60 * 1000L
        const val PREFS_NAME = "auto_record"
        private const val KEY_UNTIL = "until_ms"
        private const val START_TIMEOUT_MS = 15_000L
        private const val EXPIRED_CHANNEL = "auto_record_expired"
        private const val EXPIRED_ID = 7402

        /** «чч:мм» по часам устройства — так же, как в уведомлении. */
        fun formatTime(ms: Long): String =
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))
    }
}
