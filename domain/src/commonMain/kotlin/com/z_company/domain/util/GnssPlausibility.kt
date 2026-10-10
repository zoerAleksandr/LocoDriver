package com.z_company.domain.util

import com.z_company.domain.track.TrackPoint
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Правдоподобие нового GNSS-фикса относительно предыдущего ПРИНЯТОГО (SCREEN_SPECS §6.9, «Подмена и скачки GPS»).
 * Чистый код без зависимостей от платформы; не потокобезопасен (один экземпляр - один поток/корутина).
 *
 * Фикс отвергается как «подмена/скачок», если: заявленная скорость > [MAX_CLAIMED_SPEED_KMH]; скорость перемещения
 * (расстояние / Δt) > [MAX_MOVE_SPEED_KMH]; прыжок > [JUMP_DISTANCE_M] быстрее чем за [JUMP_WINDOW_MS]; либо он далеко от
 * свежей сетевой позиции (вышки/Wi-Fi): расстояние > max([NETWORK_MIN_RADIUS_M], 3 x точность сети). Отвергнутые фиксы
 * нельзя использовать нигде (трек, путь на карте, маркеры старта/финиша, детектор остановок, скорость/ETA, «текущая
 * позиция»): текущая позиция остаётся последней правдоподобной. Чтобы не застрять навсегда после реального телепорта,
 * [NEW_AREA_MIN_FIXES] согласованных между собой фиксов в новой области в течение >= [NEW_AREA_MIN_DURATION_MS] принимают
 * новую область (кроме случая, когда её опровергает сетевая позиция). Координаты не логируются.
 */
class GnssPlausibility {

    /** Сетевая позиция (вышки/Wi-Fi) - только ориентир для проверки, в трек не пишется. [ageMs] - возраст на момент проверки. */
    data class NetworkFix(val latitude: Double, val longitude: Double, val accuracyM: Float?, val ageMs: Long)

    enum class Reason { OK, FIRST, NEW_AREA, SPEED_CLAIM, JUMP, NETWORK_MISMATCH }

    /**
     * [accepted] - можно использовать. [newArea] - принята новая область после серии согласованных фиксов.
     * [landmark] - при [Reason.NETWORK_MISMATCH] сетевая оценка как ориентир (не точка трека).
     */
    data class Result(val accepted: Boolean, val reason: Reason, val landmark: NetworkFix? = null)

    private class Anchor(val timeMs: Long, val lat: Double, val lon: Double)

    private var last: Anchor? = null
    private var clusterStart: Long = 0L
    private var clusterLast: Anchor? = null
    private var clusterCount = 0
    private val rejections = ArrayDeque<Long>()

    /** Последняя принятая позиция (lat, lon) или null. */
    val lastAccepted: Pair<Double, Double>? get() = last?.let { it.lat to it.lon }

    fun reset() {
        last = null
        clearCluster()
        rejections.clear()
    }

    /**
     * Проверить фикс. [timeMs] - время фикса (мс, любые монотонные часы), [nowMs] - часы для окна «отвергнуто за 60 с»
     * (по умолчанию [timeMs]), [speedKmh] - заявленная скорость или null.
     */
    fun check(
        timeMs: Long,
        lat: Double,
        lon: Double,
        speedKmh: Double? = null,
        accuracyM: Float? = null,
        network: NetworkFix? = null,
        nowMs: Long = timeMs,
    ): Result {
        val net = network?.takeIf { it.ageMs in 0..NETWORK_MAX_AGE_MS }
        if (speedKmh != null && speedKmh > MAX_CLAIMED_SPEED_KMH) return reject(Reason.SPEED_CLAIM, nowMs)
        if (net != null) {
            val limit = max(NETWORK_MIN_RADIUS_M, NETWORK_ACCURACY_FACTOR * (net.accuracyM ?: 0f))
            if (distanceM(lat, lon, net.latitude, net.longitude) > limit) {
                clearCluster()
                return reject(Reason.NETWORK_MISMATCH, nowMs, net)
            }
        }
        val prev = last
        if (prev == null) {
            last = Anchor(timeMs, lat, lon)
            return Result(true, Reason.FIRST)
        }
        if (!isJump(prev, timeMs, lat, lon)) {
            last = Anchor(timeMs, lat, lon)
            clearCluster()
            return Result(true, Reason.OK)
        }
        // Прыжок: копим согласованные между собой фиксы в новой области.
        val cl = clusterLast
        if (cl == null || isJump(cl, timeMs, lat, lon)) {
            clusterStart = timeMs
            clusterCount = 1
        } else {
            clusterCount++
        }
        clusterLast = Anchor(timeMs, lat, lon)
        if (clusterCount >= NEW_AREA_MIN_FIXES && timeMs - clusterStart >= NEW_AREA_MIN_DURATION_MS) {
            last = Anchor(timeMs, lat, lon)
            clearCluster()
            return Result(true, Reason.NEW_AREA)
        }
        return reject(Reason.JUMP, nowMs)
    }

    /** Сколько фиксов отвергнуто за последние [windowMs] (по часам [nowMs]). */
    fun rejectedCount(nowMs: Long, windowMs: Long = REJECT_WINDOW_MS): Int {
        while (rejections.isNotEmpty() && nowMs - rejections.first() > windowMs) rejections.removeFirst()
        return rejections.size
    }

    private fun reject(reason: Reason, nowMs: Long, landmark: NetworkFix? = null): Result {
        rejections.addLast(nowMs)
        while (rejections.size > MAX_REJECTIONS_KEPT) rejections.removeFirst()
        return Result(false, reason, landmark)
    }

    private fun clearCluster() {
        clusterLast = null
        clusterCount = 0
        clusterStart = 0L
    }

    private fun isJump(from: Anchor, timeMs: Long, lat: Double, lon: Double): Boolean {
        val dt = max(timeMs - from.timeMs, MIN_DT_MS)
        val d = distanceM(from.lat, from.lon, lat, lon)
        val kmh = d / (dt / 1000.0) * 3.6
        return kmh > MAX_MOVE_SPEED_KMH || (d > JUMP_DISTANCE_M && dt < JUMP_WINDOW_MS)
    }

    companion object {
        const val MAX_MOVE_SPEED_KMH = 250.0
        const val MAX_CLAIMED_SPEED_KMH = 200.0
        const val JUMP_DISTANCE_M = 1_000.0
        const val JUMP_WINDOW_MS = 30_000L
        const val NEW_AREA_MIN_FIXES = 5
        const val NEW_AREA_MIN_DURATION_MS = 60_000L
        const val REJECT_WINDOW_MS = 60_000L
        /** «Помеха GPS»: отвергнуто за [REJECT_WINDOW_MS] не меньше стольких. */
        const val INTERFERENCE_MIN_REJECTS = 2
        const val NETWORK_MAX_AGE_MS = 120_000L
        const val NETWORK_MIN_RADIUS_M = 5_000.0
        const val NETWORK_ACCURACY_FACTOR = 3.0
        private const val MIN_DT_MS = 1_000L
        private const val MAX_REJECTIONS_KEPT = 64
        private const val EARTH_R = 6_371_000.0

        /** Расстояние по большому кругу, м (haversine). */
        fun distanceM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val p1 = lat1 * PI / 180.0
            val p2 = lat2 * PI / 180.0
            val dp = p2 - p1
            val dl = (lon2 - lon1) * PI / 180.0
            val a = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
            return 2 * EARTH_R * asin(min(1.0, sqrt(a)))
        }

        /**
         * Очистка уже записанного/загруженного трека (по времени) теми же правилами, без сети: старые треки, в которые
         * попали подменные точки, не должны давать ложные «старт/финиш» и линию через полстраны.
         */
        fun sanitize(points: List<TrackPoint>): List<TrackPoint> {
            if (points.size < 2) return points
            val f = GnssPlausibility()
            return points.filter { f.check(it.timeMs, it.latitude, it.longitude, it.speedKmh).accepted }
        }
    }
}
