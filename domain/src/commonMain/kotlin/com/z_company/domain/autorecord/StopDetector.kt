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
 * Событие стоянки/отправления. [timeMs] — момент начала окна (когда скорость упала ниже
 * порога / превысила порог), с точностью до отсчёта; в запись идёт [minuteTimeMs].
 */
data class StopEvent(
    val type: StopEventType,
    val timeMs: Long,
    val latitude: Double,
    val longitude: Double,
) {
    /** Время, кратное минуте (SCREEN_SPECS §3.3 п.6). */
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
    private var windowStart: MotionSample? = null
    private var lastTimeMs: Long? = null

    /** Подать очередной отсчёт; возвращает событие, если оно только что подтвердилось. */
    fun onSample(sample: MotionSample): StopEvent? {
        val speed = sample.speedKmh ?: return null
        if (sample.accuracyM != null && sample.accuracyM > config.maxAccuracyM) return null

        val last = lastTimeMs
        if (last != null && sample.timeMs - last > config.maxGapMs) windowStart = null
        lastTimeMs = sample.timeMs

        return if (!stopped) {
            if (speed < config.stopSpeedKmh) {
                val start = windowStart ?: sample.also { windowStart = it }
                if (sample.timeMs - start.timeMs >= config.stopMinDurationMs) {
                    stopped = true
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
                    StopEvent(StopEventType.DEPARTURE, start.timeMs, start.latitude, start.longitude)
                } else null
            } else {
                windowStart = null
                null
            }
        }
    }
}
