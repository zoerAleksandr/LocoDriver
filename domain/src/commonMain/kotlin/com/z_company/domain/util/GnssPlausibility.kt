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
 * новую область. Сетевая позиция блокирует фикс, только если она свежая ([NETWORK_MAX_AGE_MS]), достаточно точная
 * (< [NETWORK_MAX_ACCURACY_M]) и совпадает с последней принятой позицией (подтверждает старую область); устаревшая сеть,
 * «залипшая» в старой области, не может блокировать вечно: согласованная серия фиксов с реальным перемещением
 * >= [NETWORK_OVERRIDE_DISPLACEMENT_M] за >= [NETWORK_OVERRIDE_DURATION_MS] принимается вопреки ней. Непрерывная цепочка (фикс
 * не позже [CONTINUITY_MAX_GAP_MS] после предыдущего принятого, без прыжка) сетью не блокируется: сеть проверяет только старт
 * цепочки и прыжки (в поезде сетевая позиция уезжает на километры - иначе дыра ~2 мин на ходу). Якорь «последняя принятая»
 * снимается после [STALE_ANCHOR_MS] без принятых фиксов. Координаты не логируются.
 */
class GnssPlausibility {

    /** Сетевая позиция (вышки/Wi-Fi) - только ориентир для проверки, в трек не пишется. [ageMs] - возраст на момент проверки. */
    data class NetworkFix(val latitude: Double, val longitude: Double, val accuracyM: Float?, val ageMs: Long)

    enum class Reason { OK, FIRST, NEW_AREA, SPEED_CLAIM, JUMP, NETWORK_MISMATCH }

    /**
     * [accepted] - можно использовать. [newArea] - принята новая область после серии согласованных фиксов.
     * [landmark] - при [Reason.NETWORK_MISMATCH] сетевая оценка как ориентир (не точка трека).
     */
    data class Result(val accepted: Boolean, val reason: Reason, val landmark: NetworkFix? = null, val networkIgnored: Boolean = false)

    private class Anchor(val timeMs: Long, val lat: Double, val lon: Double, val accuracyM: Float? = null)

    private var last: Anchor? = null
    private var clusterStart: Long = 0L
    private var clusterLast: Anchor? = null
    private var clusterCount = 0
    private var clusterOrigin: Anchor? = null
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
        if (speedKmh != null && speedKmh > MAX_CLAIMED_SPEED_KMH) return reject(Reason.SPEED_CLAIM, nowMs)
        // Долгое молчание (нет принятых фиксов > STALE_ANCHOR_MS): якорь устарел, «залипание» снимается.
        last?.let { if (timeMs - it.timeMs > STALE_ANCHOR_MS) { last = null; clearCluster() } }
        val prev = last
        val net = network?.takeIf {
            it.ageMs in 0..NETWORK_MAX_AGE_MS && (it.accuracyM == null || it.accuracyM < NETWORK_MAX_ACCURACY_M)
        }
        if (net != null) {
            val limit = max(NETWORK_MIN_RADIUS_M, NETWORK_ACCURACY_FACTOR * (net.accuracyM ?: 0f))
            // Сеть блокирует, только если подтверждает последнюю принятую область (или принятых ещё нет).
            val confirmsOld = prev == null || distanceM(prev.lat, prev.lon, net.latitude, net.longitude) <= limit
            val farFromNet = confirmsOld && distanceM(lat, lon, net.latitude, net.longitude) > limit
            // Непрерывная цепочка: фикс сразу за предыдущим принятым без прыжка - реальное движение. Сеть в поезде врёт на
            // километры (Wi-Fi/вышки едущего состава, залипший кэш), и блокировка такого фикса даёт дыру в ~2 мин на ходу
            // при точности 2-4 м; сеть тут не опровержение (проверяет только старт цепочки и прыжки).
            if (farFromNet && prev != null && timeMs - prev.timeMs <= CONTINUITY_MAX_GAP_MS &&
                !isJump(prev, timeMs, lat, lon, accuracyM)
            ) {
                last = Anchor(timeMs, lat, lon, accuracyM)
                clearCluster()
                return Result(true, Reason.OK, networkIgnored = true)
            }
            if (farFromNet) {
                accumulate(timeMs, lat, lon, accuracyM)
                val origin = clusterOrigin
                if (clusterCount >= NEW_AREA_MIN_FIXES && timeMs - clusterStart >= NETWORK_OVERRIDE_DURATION_MS &&
                    origin != null && distanceM(origin.lat, origin.lon, lat, lon) >= NETWORK_OVERRIDE_DISPLACEMENT_M
                ) {
                    last = Anchor(timeMs, lat, lon, accuracyM)
                    clearCluster()
                    return Result(true, Reason.NEW_AREA)
                }
                return reject(Reason.NETWORK_MISMATCH, nowMs, net)
            }
        }
        if (prev == null) {
            last = Anchor(timeMs, lat, lon, accuracyM)
            return Result(true, Reason.FIRST)
        }
        if (!isJump(prev, timeMs, lat, lon, accuracyM)) {
            last = Anchor(timeMs, lat, lon, accuracyM)
            clearCluster()
            return Result(true, Reason.OK)
        }
        // Прыжок: копим согласованные между собой фиксы в новой области.
        accumulate(timeMs, lat, lon, accuracyM)
        if (clusterCount >= NEW_AREA_MIN_FIXES && timeMs - clusterStart >= NEW_AREA_MIN_DURATION_MS) {
            last = Anchor(timeMs, lat, lon, accuracyM)
            clearCluster()
            return Result(true, Reason.NEW_AREA)
        }
        return reject(Reason.JUMP, nowMs)
    }

    private fun accumulate(timeMs: Long, lat: Double, lon: Double, accuracyM: Float?) {
        val cl = clusterLast
        if (cl == null || isJump(cl, timeMs, lat, lon, accuracyM)) {
            clusterStart = timeMs
            clusterCount = 1
            clusterOrigin = Anchor(timeMs, lat, lon, accuracyM)
        } else {
            clusterCount++
        }
        clusterLast = Anchor(timeMs, lat, lon, accuracyM)
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
        clusterOrigin = null
        clusterCount = 0
        clusterStart = 0L
    }

    /**
     * Прыжок с учётом точности: смещение, которое укладывается в сумму заявленных погрешностей двух фиксов, - шум, а не
     * прыжок (фикс с точностью 200 м не должен «отравлять» якорь и отвергать следующие хорошие фиксы минутами).
     */
    private fun isJump(from: Anchor, timeMs: Long, lat: Double, lon: Double, accuracyM: Float?): Boolean {
        val dt = max(timeMs - from.timeMs, MIN_DT_MS)
        val raw = distanceM(from.lat, from.lon, lat, lon)
        val credit = min(ACCURACY_CREDIT_MAX_M, (from.accuracyM ?: 0f).toDouble()) + min(ACCURACY_CREDIT_MAX_M, (accuracyM ?: 0f).toDouble())
        val d = max(0.0, raw - credit)
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
        /** Сеть грубее этого (вышка без привязки) не используется как опровержение. */
        const val NETWORK_MAX_ACCURACY_M = 5_000f
        /** Нет принятых фиксов дольше этого - якорь «последняя принятая» снимается. */
        const val STALE_ANCHOR_MS = 5 * 60_000L
        /** Принятие области вопреки сети: согласованная серия >= 5 фиксов за >= 120 с с перемещением >= 500 м. */
        const val NETWORK_OVERRIDE_DURATION_MS = 120_000L
        const val NETWORK_OVERRIDE_DISPLACEMENT_M = 500.0
        /** Время фикса дальше этого от часов телефона - подмена времени (GNSS-спуфинг), фикс отвергается/отбрасывается. */
        const val TIME_SKEW_MS = 24 * 3_600_000L
        /** Точка трека дальше этого от медианы времени трека - ложная метка времени. */
        const val TIME_OUTLIER_MS = 72 * 3_600_000L
        /** Фикс не позже этого после предыдущего принятого и без прыжка - непрерывная цепочка, сеть её не блокирует. */
        const val CONTINUITY_MAX_GAP_MS = 30_000L
        const val NETWORK_MIN_RADIUS_M = 5_000.0
        const val NETWORK_ACCURACY_FACTOR = 3.0
        /** Больше этого заявленная точность фикса не засчитывается как допуск (спуфер не спрячет скачок огромной «погрешностью»). */
        private const val ACCURACY_CREDIT_MAX_M = 300.0
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
            return dropTimeOutliers(points).filter { f.check(it.timeMs, it.latitude, it.longitude, it.speedKmh).accepted }
        }

        /** Время фикса правдоподобно относительно часов телефона (иначе - подмена времени, фикс нельзя писать в трек). */
        fun isTimeSane(fixTimeMs: Long, wallNowMs: Long): Boolean =
            kotlin.math.abs(fixTimeMs - wallNowMs) <= TIME_SKEW_MS

        /**
         * Убирает точки с ложной меткой времени (GNSS-спуфинг может сдвинуть время на недели/месяцы): точки дальше
         * [TIME_OUTLIER_MS] от медианы времени трека отбрасываются. Результат отсортирован по времени.
         */
        fun dropTimeOutliers(points: List<TrackPoint>): List<TrackPoint> {
            if (points.size < 2) return points
            val sorted = points.sortedBy { it.timeMs }
            val median = sorted[sorted.size / 2].timeMs
            return sorted.filter { kotlin.math.abs(it.timeMs - median) <= TIME_OUTLIER_MS }
        }
    }
}
