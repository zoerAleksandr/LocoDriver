package com.z_company.route.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.GnssStatus
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.location.LocationManagerCompat
import com.z_company.domain.gnss.GnssQuality
import com.z_company.domain.gnss.GnssQualityEvaluator
import com.z_company.domain.util.GnssPlausibility
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Качество GNSS-сигнала для индикатора на карте поезда (SCREEN_SPECS §6.9). В отличие от `GpsRecorder.quality` не
 * зависит от службы записи «АВТО»: считается по ТЕМ ЖЕ живым обновлениям положения, что показывают точку на карте
 * ([onFix] вызывается для каждого fix из `DeviceLocation.updates` и из точек записи), плюс подписка на статус спутников
 * (`GnssStatus`), пока карта открыта. Правила уровней и сглаживание - `domain.gnss`.
 *
 * Пока fix не было ни разу (запись выключена, положение ещё не пришло) - уровень UNKNOWN («нет данных», серый), не LOST;
 * красный (LOST) - только когда fix, приходившие ранее, пропали более чем на 15 с или определение местоположения выключено.
 * Время fix - по `SystemClock.elapsedRealtime()` в момент получения (часы устройства не используются, поэтому «возраст»
 * не зависит от рассинхрона времени GPS и телефона).
 */
class GnssQualityMonitor(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private val evaluator = GnssQualityEvaluator()
    private val _quality = MutableStateFlow(GnssQuality())
    val quality: StateFlow<GnssQuality> = _quality.asStateFlow()

    private var callback: GnssStatus.Callback? = null
    private var running = false
    private var startedMs = 0L
    private var lastFixMs: Long? = null
    private var lastAccuracyM: Float? = null
    private var satsUsed: Int? = null
    private var satsVisible: Int? = null
    private val rejects = ArrayDeque<Long>()

    private val tick = object : Runnable {
        override fun run() {
            publish()
            if (running) handler.postDelayed(this, TICK_MS)
        }
    }

    /** Запомнить, что только что пришёл fix (любой провайдер). Можно вызывать с главного потока. */
    fun onFix(accuracyM: Float?) {
        lastFixMs = SystemClock.elapsedRealtime()
        lastAccuracyM = accuracyM
    }

    /**
     * Fix отвергнут как подмена/скачок ([GnssPlausibility]): не считается «свежим fix», но копится для состояния
     * «Помеха GPS» (>= 2 за 60 с).
     */
    fun onRejected() {
        rejects.addLast(SystemClock.elapsedRealtime())
        while (rejects.size > 64) rejects.removeFirst()
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (running) return
        running = true
        evaluator.reset()
        startedMs = SystemClock.elapsedRealtime()
        lastFixMs = null
        lastAccuracyM = null
        rejects.clear()
        satsUsed = null
        satsVisible = null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (lm != null && DeviceLocation.hasPermission(context)) {
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
            if (runCatching { lm.registerGnssStatusCallback(cb, handler) }.getOrDefault(false)) callback = cb
        }
        handler.removeCallbacks(tick)
        handler.post(tick)
    }

    fun stop() {
        running = false
        handler.removeCallbacks(tick)
        callback?.let { cb ->
            runCatching { (context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager)?.unregisterGnssStatusCallback(cb) }
        }
        callback = null
        _quality.value = GnssQuality()
    }

    private fun rejectedRecent(now: Long): Int {
        while (rejects.isNotEmpty() && now - rejects.first() > GnssPlausibility.REJECT_WINDOW_MS) rejects.removeFirst()
        return rejects.size
    }

    private fun publish() {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val enabled = lm?.let { runCatching { LocationManagerCompat.isLocationEnabled(it) }.getOrDefault(true) } ?: true
        _quality.value = evaluator.update(
            nowMs = SystemClock.elapsedRealtime(),
            startedMs = startedMs,
            lastFixMs = lastFixMs,
            accuracyM = lastAccuracyM,
            satellitesUsed = satsUsed,
            satellitesVisible = satsVisible,
            // Определение местоположения выключено в системе - LOST только если fix уже были (иначе «нет данных»).
            providerEnabled = enabled || lastFixMs == null,
            lostWithoutFix = false,
            rejectedRecent = rejectedRecent(SystemClock.elapsedRealtime()),
        )
    }

    private companion object {
        const val TICK_MS = 1_000L
    }
}
