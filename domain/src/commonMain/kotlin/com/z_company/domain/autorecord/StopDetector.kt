package com.z_company.domain.autorecord

/** Отсчёт скорости для детектора. Не зависит от Android: GPS-слой переводит свои точки сюда. */
data class MotionSample(
    val timeMs: Long,
    val latitude: Double,
    val longitude: Double,
    /** null — провайдер скорость не сообщил (такие отсчёты игнорируются). */
    val speedKmh: Double?,
    val accuracyM: Float? = null,
)

enum class StopEventType { ARRIVAL, DEPARTURE }

/**
 * Событие стоянки/отправления. [timeMs] — момент, когда остановка/движение РЕАЛЬНО началось
 * (с поправкой на задержку детектора, см. [StopDetector]); в запись идёт [minuteTimeMs].
 */
data class StopEvent(
    val type: StopEventType,
    val timeMs: Long,
    val latitude: Double,
    val longitude: Double,
) {
    /** Время, кратное минуте: округление ВНИЗ (SCREEN_SPECS §3.3 п.6, §6.2 п.6а). */
    val minuteTimeMs: Long get() = timeMs - (timeMs % 60_000L)
}

/** Пороги детектора; уточнятся по реальным поездкам (SCREEN_SPECS §6.2 п.6а). */
data class StopDetectorConfig(
    /** Ниже этой скорости — «стоим». */
    val stopSpeedKmh: Double = 2.0,
    /** Сколько непрерывно стоим, чтобы засчитать прибытие. */
    val stopMinDurationMs: Long = 60_000L,
    /** Выше этой скорости — «едем». */
    val moveSpeedKmh: Double = 5.0,
    /** Сколько непрерывно едем после остановки, чтобы засчитать отправление. */
    val moveMinDurationMs: Long = 30_000L,
    /** Отсчёты с точностью хуже этой не используются (скорость недостоверна). */
    val maxAccuracyM: Float = 50f,
    /** Пауза между отсчётами, после которой окно ожидания сбрасывается. */
    val maxGapMs: Long = 30_000L,
    /** Скорость «≈0»: последний отсчёт не выше неё перед ростом скорости — момент реального трогания. */
    val restSpeedKmh: Double = 1.0,
    /** Насколько назад по буферу отсчётов можно отодвигать время отправления. */
    val maxRollbackMs: Long = 120_000L,
    /** Размер буфера последних отсчётов в памяти (на диск не пишется). */
    val bufferSize: Int = 300,
)

/**
 * Детектор стоянок и отправлений по скорости. Чистая логика без времени «сейчас»:
 * все решения принимаются по отметкам времени отсчётов. Один экземпляр — на одну сессию записи.
 *
 * Начальное состояние «неизвестно» ведёт себя как «едем»: первая же стоянка даёт ARRIVAL
 * (потребитель сам решит, нужна ли она, см. [AutoTrainWriter]).
 */
class StopDetector(private val config: StopDetectorConfig = StopDetectorConfig()) {

    private var stopped = false
    /** Момент начала текущей стоянки (время ARRIVAL); откат отправления не уходит раньше него. */
    private var stoppedSinceMs = 0L
    /** Буфер последних достоверных отсчётов — только в памяти, для отката времени отправления. */
    private val buffer = ArrayDeque<MotionSample>()
    private var windowStart: MotionSample? = null
    private var lastTimeMs: Long? = null

    /** Подать очередной отсчёт; возвращает событие, если оно только что подтвердилось. */
    fun onSample(sample: MotionSample): StopEvent? {
        val speed = sample.speedKmh ?: return null
        if (sample.accuracyM != null && sample.accuracyM > config.maxAccuracyM) return null

        val last = lastTimeMs
        if (last != null && sample.timeMs - last > config.maxGapMs) windowStart = null
        lastTimeMs = sample.timeMs
        buffer.addLast(sample)
        while (buffer.size > config.bufferSize) buffer.removeFirst()

        return if (!stopped) {
            if (speed < config.stopSpeedKmh) {
                val start = windowStart ?: sample.also { windowStart = it }
                if (sample.timeMs - start.timeMs >= config.stopMinDurationMs) {
                    stopped = true
                    stoppedSinceMs = start.timeMs
                    windowStart = null
                    StopEvent(StopEventType.ARRIVAL, start.timeMs, start.latitude, start.longitude)
                } else null
            } else {
                windowStart = null
                null
            }
        } else {
            if (speed > config.moveSpeedKmh) {
                val start = windowStart ?: sample.also { windowStart = it }
                if (sample.timeMs - start.timeMs >= config.moveMinDurationMs) {
                    stopped = false
                    windowStart = null
                    val real = rollbackToRest(start)
                    StopEvent(StopEventType.DEPARTURE, real.timeMs, real.latitude, real.longitude)
                } else null
            } else {
                windowStart = null
                null
            }
        }
    }

    /**
     * Скорость растёт плавно, поэтому «окно движения» начинается заметно позже реального трогания.
     * Идём назад по буферу и берём последний отсчёт «≈0» перед ростом; если такого нет (дыра GPS,
     * ползли дольше [StopDetectorConfig.maxRollbackMs]) — остаёмся на начале окна. Стоянка (arrival)
     * уже указывает на первый отсчёт ниже порога без последующего роста, откат для неё не нужен.
     */
    private fun rollbackToRest(start: MotionSample): MotionSample {
        var prevTime = start.timeMs
        for (i in buffer.indices.reversed()) {
            val s = buffer[i]
            if (s.timeMs >= start.timeMs) continue
            if (prevTime - s.timeMs > config.maxGapMs) break
            if (start.timeMs - s.timeMs > config.maxRollbackMs || s.timeMs < stoppedSinceMs) break
            if ((s.speedKmh ?: continue) <= config.restSpeedKmh) return s
            prevTime = s.timeMs
        }
        return start
    }
}
