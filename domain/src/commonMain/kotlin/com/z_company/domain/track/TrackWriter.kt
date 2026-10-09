package com.z_company.domain.track

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Прореживание при записи: в движении — точка раз в ~[movingIntervalMs] (5 с), на стоянке —
 * раз в ~[stoppedIntervalMs] (60 с). Первая точка принимается всегда. Скорость неизвестна — как «в движении».
 */
class TrackThinner(
    private val movingIntervalMs: Long = 5_000L,
    private val stoppedIntervalMs: Long = 60_000L,
    private val movingSpeedKmh: Double = 2.0,
) {
    private var lastAcceptedMs: Long? = null

    fun accept(point: TrackPoint): Boolean {
        val last = lastAcceptedMs
        val moving = point.speedKmh?.let { it >= movingSpeedKmh } ?: true
        val interval = if (moving) movingIntervalMs else stoppedIntervalMs
        // Часы пошли назад (смена времени) — принимаем и начинаем отсчёт заново.
        if (last != null && point.timeMs >= last && point.timeMs - last < interval) return false
        lastAcceptedMs = point.timeMs
        return true
    }
}

/**
 * Пишет точки «АВТО» в [TrackRepository]: один трек на сеанс и поезд, прореживание, и только при
 * согласии (SCREEN_SPECS §6.9). Не потокобезопасен — вызывать из одной корутины.
 */
class TrackWriter(
    private val repository: TrackRepository,
    private val newTrackId: () -> String = ::randomTrackId,
) {
    private var trainId: String? = null
    private var trackId: String? = null
    private var thinner = TrackThinner()

    /**
     * Записать точку; false — не записана (нет согласия / нет цели / прорежена / нет сигнала).
     * [signalOk] = false (сигнал потерян, fix устарел или точность хуже порога): «точка стоянки» (скорость < 2 км/ч или
     * неизвестна) не пишется — иначе потеря сигнала выглядела бы как стоянка; точка в движении (скорость известна и высока)
     * пишется как обычно.
     */
    fun onPoint(targetTrainId: String?, point: TrackPoint, consent: Boolean, signalOk: Boolean = true): Boolean {
        if (!consent || targetTrainId == null) return false
        if (!signalOk && (point.speedKmh?.let { it < TrackAnalysis.STOP_SPEED_KMH } != false)) return false
        var id = trackId
        if (trainId != targetTrainId || id == null) {
            id = newTrackId()
            trainId = targetTrainId
            trackId = id
            thinner = TrackThinner()
        }
        if (!thinner.accept(point)) return false
        repository.append(id, targetTrainId, point)
        return true
    }

    /** Сеанс «АВТО» закончился: следующая точка откроет новый трек. */
    fun reset() {
        trainId = null
        trackId = null
        thinner = TrackThinner()
    }
}

@OptIn(ExperimentalUuidApi::class)
private fun randomTrackId(): String = Uuid.random().toString()
