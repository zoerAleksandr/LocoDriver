package com.z_company.core.gps

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Переиспользуемая GPS-запись: foreground service + поток точек. Не знает ни про поезд, ни про
 * навигатор; потребитель подписывается на [points] и сам решает, что делать с данными.
 *
 * Контракт:
 *  - [start] — запускает service (разрешение на геолокацию должно быть уже выдано, а приложение
 *    находиться на экране: иначе Android 12+ не разрешит стартовать location-service);
 *  - [stop] — останавливает; то же делает действие «Выключить» в уведомлении;
 *  - [points] — горячий поток фиксов (без повторов для опоздавших подписчиков);
 *  - [isRunning] — true между start и stop (в т.ч. если остановку инициировало уведомление).
 * Работает в одном процессе с приложением: поток живёт, пока жив service.
 */
object GpsRecorder {

    private val _points = MutableSharedFlow<GpsPoint>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val points: SharedFlow<GpsPoint> = _points.asSharedFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    /** false, пока GPS-провайдер выключен в системе. */
    private val _gpsAvailable = MutableStateFlow(true)
    val gpsAvailable: StateFlow<Boolean> = _gpsAvailable.asStateFlow()

    /** Запуск; повторный вызов при работающей записи только обновляет текст уведомления. */
    fun start(context: Context, config: GpsRecorderConfig) {
        val intent = Intent(context, GpsRecordingService::class.java)
            .setAction(GpsRecordingService.ACTION_START)
            .putExtra(GpsRecordingService.EXTRA_TITLE, config.notificationTitle)
            .putExtra(GpsRecordingService.EXTRA_TEXT, config.notificationText)
            .putExtra(GpsRecordingService.EXTRA_STOP_LABEL, config.stopActionLabel)
            .putExtra(GpsRecordingService.EXTRA_ICON, config.smallIconRes)
            .putExtra(GpsRecordingService.EXTRA_CHANNEL, config.channelName)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        context.startService(
            Intent(context, GpsRecordingService::class.java).setAction(GpsRecordingService.ACTION_STOP),
        )
    }

    internal fun emit(point: GpsPoint) {
        _points.tryEmit(point)
    }

    internal fun setRunning(value: Boolean) {
        _isRunning.value = value
    }

    internal fun setGpsAvailable(value: Boolean) {
        _gpsAvailable.value = value
    }
}
