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
    private var lastAcceptedMoving: Boolean? = null
    private var rejectedSlow: TrackPoint? = null
    private var pending: TrackPoint? = null

    /**
     * Принимать ли точку. Смена «стоим/едем» принимается сразу (точное время остановки и трогания для детектора стоянок:
     * иначе первая «медленная» точка терялась бы на до 60 с). При трогании [takePending] отдаёт последнюю отброшенную
     * «медленную» точку - момент покоя перед ростом скорости.
     */
    fun accept(point: TrackPoint, held: Boolean = false): Boolean {
        pending = null
        val last = lastAcceptedMs
        // held - позиция заменена якорем стоянки (DriftFilter): это покой, даже если датчик заявил скорость.
        val moving = if (held) false else point.speedKmh?.let { it >= movingSpeedKmh } ?: true
        val interval = if (moving) movingIntervalMs else stoppedIntervalMs
        val changed = lastAcceptedMoving != null && lastAcceptedMoving != moving
        // Часы пошли назад (смена времени) — принимаем и начинаем отсчёт заново.
        if (!changed && last != null && point.timeMs >= last && point.timeMs - last < interval) {
            if (!moving) rejectedSlow = point
            return false
        }
        if (changed && moving) pending = rejectedSlow?.takeIf { last == null || it.timeMs > last }
        rejectedSlow = null
        lastAcceptedMs = point.timeMs
        lastAcceptedMoving = moving
        return true
    }

    /** Продолжение записи после перезапуска: последняя уже записанная точка трека - точка отсчёта прореживания. */
    fun seed(last: TrackPoint) {
        lastAcceptedMs = last.timeMs
        lastAcceptedMoving = last.speedKmh?.let { it >= movingSpeedKmh } ?: true
        rejectedSlow = null
        pending = null
    }

    /** Точка, которую надо записать ПЕРЕД только что принятой (последняя «медленная» перед трогание); null - нет. */
    fun takePending(): TrackPoint? = pending.also { pending = null }
}

/**
 * Пишет точки «АВТО» в [TrackRepository]: один трек на сеанс и поезд, прореживание, и только при
 * согласии (SCREEN_SPECS §6.9). Один поезд - один трек: если последний трек этого поезда закончился не более
 * [RESUME_GAP_MS] (12 ч) назад, запись продолжает его (перезапуск сервиса/процесса, переармирование «АВТО», стоп/старт),
 * состояние берётся из хранилища, а не из памяти. Не потокобезопасен — вызывать из одной корутины.
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
     * пишется как обычно. [held] = true: позиция заменена якорем стоянки (DriftFilter, дрейф) - такая точка пишется как
     * «стоянка» (раз в [TrackThinner.stoppedIntervalMs]), а не раз в 5 с.
     */
    fun onPoint(targetTrainId: String?, point: TrackPoint, consent: Boolean, signalOk: Boolean = true, held: Boolean = false): Boolean {
        if (!consent || targetTrainId == null) return false
        if (!signalOk && (point.speedKmh?.let { it < TrackAnalysis.STOP_SPEED_KMH } != false)) return false
        var id = trackId
        if (trainId != targetTrainId || id == null) {
            thinner = TrackThinner()
            val resumed = findResumable(targetTrainId, point.timeMs)
            if (resumed != null) {
                id = resumed.first
                resumed.second?.let { thinner.seed(it) }
            } else {
                id = newTrackId()
            }
            trainId = targetTrainId
            trackId = id
        }
        if (!thinner.accept(point, held)) return false
        thinner.takePending()?.let { repository.append(id, targetTrainId, it) }
        repository.append(id, targetTrainId, point)
        return true
    }

    /** Последний трек поезда, который можно продолжить (закончился не более [RESUME_GAP_MS] = 12 ч назад), и его последняя точка. */
    private fun findResumable(targetTrainId: String, nowPointMs: Long): Pair<String, TrackPoint?>? {
        val last = try {
            repository.tracksForTrain(targetTrainId).maxByOrNull { it.endedAt }
        } catch (e: Exception) {
            null
        } ?: return null
        val gap = nowPointMs - last.endedAt
        if (gap < 0 || gap > RESUME_GAP_MS) return null
        val lastPoint = try {
            repository.points(last.trackId).lastOrNull()
        } catch (e: Exception) {
            null
        }
        return last.trackId to lastPoint
    }

    /** Сеанс «АВТО» закончился: следующая точка откроет новый трек (или продолжит последний, если прошло ≤ [RESUME_GAP_MS] = 12 ч). */
    fun reset() {
        trainId = null
        trackId = null
        thinner = TrackThinner()
    }

    companion object {
        /** Страховка: пока поезд тот же, запись продолжает его трек; только пауза дольше этого срока (отдых между сменами) открывает новый. */
        const val RESUME_GAP_MS = 12 * 60 * 60_000L
    }
}

@OptIn(ExperimentalUuidApi::class)
private fun randomTrackId(): String = Uuid.random().toString()
