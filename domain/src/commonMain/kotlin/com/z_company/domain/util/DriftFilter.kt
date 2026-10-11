package com.z_company.domain.util

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * «Умный» фильтр дрейфа GNSS на стоянке (SCREEN_SPECS §6.9, «Дрейф на стоянке»). Чистый код без платформы и времени
 * «сейчас»; не потокобезопасен (один экземпляр - один поток). Координаты не логируются.
 *
 * Приёмник на стоянке показывает скорость 0 и хорошую точность (2-7 м), но координаты «плавают» на 20-90 м, иногда
 * скачут на сотни метров. [GnssPlausibility] такое не ловит (скорость перемещения невелика). Фильтр ничего не отбрасывает:
 * для КАЖДОГО фикса [process] возвращает позицию - либо исходную, либо «якорь стоянки» (центроид), и признак [Result.held].
 * Скорость и время фикса не меняются (детектор остановок и анализ трека видят заявленную скорость датчика).
 *
 * Фазы. MOVING (по умолчанию, первый фикс, после потери сигнала): всё пропускается как есть. После
 * [Config.enterStillMs] подряд с заявленной скоростью < [Config.stillSpeedKmh] и всеми фиксами в радиусе стоянки от центроида
 * фаза STILL: якорь = взвешенный центроид (вес 1/точность^2), он замораживается после [Config.anchorFreezeFixes] фиксов.
 *
 * В STILL фикс считается дрейфом (позиция = якорь, held), пока движение не ПОДТВЕРЖДЕНО одним из способов:
 *  1) допплер: [Config.confirmFixes] подряд фиксов со скоростью >= [Config.confirmSpeedKmh] (серия продолжается, пока
 *     >= [Config.continueSpeedKmh]) И согласованность: смещение первого->последнего фикса серии D совместимо с путём
 *     E = sum(v*dt) (D <= E + допуск, D >= E/2 - допуск; допуск = 3 x шум, шум = max(5 м, средняя точность)) и, если
 *     курс известен и D заметно больше шума, направление смещения совпадает с курсом (до [Config.maxBearingDiffDeg]);
 *  2) новая область: фиксы дальше радиуса от якоря, но согласованные между собой (в пределах ~40 м друг от друга) не менее
 *     [Config.newAreaFixes] штук за >= [Config.newAreaMs] - якорь переносится туда (одиночный скачок, возвращающийся к якорю,
 *     игнорируется);
 *  3) страховка: фиксы дальше радиуса от якоря непрерывно >= [Config.farLeaveMs] - это уход со стоянки, дальше MOVING.
 * Любой фикс после паузы > [Config.gapResetMs] (потеря сигнала) начинает всё заново: поезд мог уехать далеко.
 */
class DriftFilter(private val cfg: Config = Config()) {

    data class Config(
        /** Радиус стоянки от якоря, м (дрейф внутри него не записывается); для плохой точности - не меньше 2 x точности. */
        val radiusM: Double = 100.0,
        /** Допплер ниже этой скорости - «стоим» (вход в STILL). */
        val stillSpeedKmh: Double = 2.0,
        /** Сколько подряд «стоим», чтобы включить фильтр. */
        val enterStillMs: Long = 15_000L,
        val enterStillMinFixes: Int = 3,
        /** Допплер не ниже этого - кандидат на подтверждение движения. */
        val confirmSpeedKmh: Double = 3.0,
        /** Серия кандидатов продолжается, пока скорость не ниже этой. */
        val continueSpeedKmh: Double = 2.0,
        val confirmFixes: Int = 4,
        val maxRunFixes: Int = 10,
        /** Макс. разрыв между фиксами серии, мс; больше - серия начинается заново. */
        val maxRunGapMs: Long = 15_000L,
        val maxBearingDiffDeg: Double = 70.0,
        /** Фиксы с точностью хуже этой не участвуют в решениях (шум). */
        val maxAccuracyM: Float = 100f,
        val newAreaFixes: Int = 5,
        val newAreaMs: Long = 20_000L,
        /** Фиксы нового места согласованы, если лежат в этом радиусе от центроида группы (не меньше 2 x точности), м. */
        val clusterRadiusM: Double = 40.0,
        val farLeaveMs: Long = 30_000L,
        /** Пауза между фиксами больше этого - потеря сигнала, фильтр сбрасывается. */
        val gapResetMs: Long = 30_000L,
        val anchorFreezeFixes: Int = 30,
    )

    data class Fix(
        val timeMs: Long,
        val latitude: Double,
        val longitude: Double,
        val speedKmh: Double?,
        val accuracyM: Float? = null,
        /** Курс датчика, градусы от севера; null - нет (или ненадёжен). */
        val bearingDeg: Float? = null,
    )

    enum class Reason { MOVING, ENTER_STILL, DRIFT, JUMP_PENDING, CONFIRMED_MOVE, NEW_AREA, LEFT_STILL, GAP_RESET }

    /** [held] = true: позиция заменена якорем стоянки (дрейф), писать её чаще раза в минуту не нужно. */
    data class Result(val latitude: Double, val longitude: Double, val held: Boolean, val reason: Reason)

    private enum class Phase { MOVING, STILL }

    private var phase = Phase.MOVING
    private var lastTimeMs: Long? = null

    // MOVING: окно «стоим».
    private var slowStartMs = 0L
    private var slowN = 0
    private val slow = Centroid()

    // STILL.
    private val anchor = Centroid()
    private var anchorN = 0
    private val run = ArrayDeque<Fix>()
    private val cluster = Centroid()
    private var clusterN = 0
    private var clusterStartMs = 0L
    private var farStartMs = -1L

    /** Текущий якорь стоянки (lat, lon) или null, если фильтр не в STILL. */
    val anchorPosition: Pair<Double, Double>? get() = if (phase == Phase.STILL) anchor.lat to anchor.lon else null

    val isStill: Boolean get() = phase == Phase.STILL

    fun reset() {
        phase = Phase.MOVING
        lastTimeMs = null
        clearSlow()
        clearStill()
    }

    fun process(fix: Fix): Result {
        val prev = lastTimeMs
        lastTimeMs = fix.timeMs
        if (prev != null && (fix.timeMs < prev || fix.timeMs - prev > cfg.gapResetMs)) {
            val wasStill = phase == Phase.STILL
            phase = Phase.MOVING
            clearSlow()
            clearStill()
            // Фикс после паузы сам по себе - первый отсчёт нового окна.
            val r = onMoving(fix)
            return if (wasStill && r.reason == Reason.MOVING) r.copy(reason = Reason.GAP_RESET) else r
        }
        return if (phase == Phase.MOVING) onMoving(fix) else onStill(fix)
    }

    // ---- MOVING ------------------------------------------------------------------------------------------------------

    private fun onMoving(fix: Fix): Result {
        val speed = fix.speedKmh
        val usable = fix.accuracyM == null || fix.accuracyM <= cfg.maxAccuracyM
        if (speed == null || speed >= cfg.stillSpeedKmh) {
            clearSlow()
            return raw(fix, Reason.MOVING)
        }
        if (!usable) return raw(fix, Reason.MOVING)
        if (slowN > 0 && GnssPlausibility.distanceM(slow.lat, slow.lon, fix.latitude, fix.longitude) > limit(fix)) clearSlow()
        if (slowN == 0) slowStartMs = fix.timeMs
        slow.add(fix.latitude, fix.longitude, weight(fix))
        slowN++
        if (fix.timeMs - slowStartMs >= cfg.enterStillMs && slowN >= cfg.enterStillMinFixes) {
            phase = Phase.STILL
            anchor.set(slow.lat, slow.lon, slow.w)
            anchorN = slowN
            clearSlow()
            clearStill(keepAnchor = true)
            return pinned(Reason.ENTER_STILL)
        }
        return raw(fix, Reason.MOVING)
    }

    // ---- STILL -------------------------------------------------------------------------------------------------------

    private fun onStill(fix: Fix): Result {
        val speed = fix.speedKmh
        val usable = fix.accuracyM == null || fix.accuracyM <= cfg.maxAccuracyM
        val da = GnssPlausibility.distanceM(anchor.lat, anchor.lon, fix.latitude, fix.longitude)

        // 1) Допплер + согласованность.
        if (usable) {
            val candidate = speed != null && (speed >= cfg.confirmSpeedKmh || (run.isNotEmpty() && speed >= cfg.continueSpeedKmh))
            if (candidate) {
                val last = run.lastOrNull()
                if (last != null && fix.timeMs - last.timeMs > cfg.maxRunGapMs) run.clear()
                run.addLast(fix)
                while (run.size > cfg.maxRunFixes) run.removeFirst()
                if (run.size >= cfg.confirmFixes && isConsistent(run)) {
                    toMoving()
                    return raw(fix, Reason.CONFIRMED_MOVE)
                }
            } else {
                run.clear()
            }
        } else {
            return pinned(Reason.DRIFT)
        }

        // 2) Внутри радиуса стоянки - дрейф.
        if (da <= limit(fix)) {
            farStartMs = -1L
            clearCluster()
            if (anchorN < cfg.anchorFreezeFixes && da <= cfg.radiusM / 2) {
                anchor.add(fix.latitude, fix.longitude, weight(fix))
                anchorN++
            }
            return pinned(Reason.DRIFT)
        }

        // 3) Далеко от якоря без подтверждённого движения: скачок или новая область.
        if (farStartMs < 0) farStartMs = fix.timeMs
        if (clusterN > 0 && GnssPlausibility.distanceM(cluster.lat, cluster.lon, fix.latitude, fix.longitude) > max(cfg.clusterRadiusM, 2.0 * (fix.accuracyM ?: 0f))) {
            clearCluster()
        }
        if (clusterN == 0) clusterStartMs = fix.timeMs
        cluster.add(fix.latitude, fix.longitude, weight(fix))
        clusterN++
        if (clusterN >= cfg.newAreaFixes && fix.timeMs - clusterStartMs >= cfg.newAreaMs) {
            anchor.set(cluster.lat, cluster.lon, cluster.w)
            anchorN = clusterN
            clearStill(keepAnchor = true)
            return Result(anchor.lat, anchor.lon, held = false, reason = Reason.NEW_AREA)
        }
        if (fix.timeMs - farStartMs >= cfg.farLeaveMs) {
            toMoving()
            return raw(fix, Reason.LEFT_STILL)
        }
        return pinned(Reason.JUMP_PENDING)
    }

    private fun isConsistent(r: ArrayDeque<Fix>): Boolean {
        val first = r.first()
        val last = r.last()
        var e = 0.0
        var accSum = 0.0
        var accN = 0
        var prev: Fix? = null
        var path = 0.0
        for (f in r) {
            val p = prev
            if (p != null) {
                path += GnssPlausibility.distanceM(p.latitude, p.longitude, f.latitude, f.longitude)
                val dt = (f.timeMs - p.timeMs) / 1000.0
                e += ((p.speedKmh ?: 0.0) + (f.speedKmh ?: 0.0)) / 2.0 / 3.6 * dt
            }
            f.accuracyM?.let { accSum += it; accN++ }
            prev = f
        }
        val noise = max(5.0, if (accN > 0) minOf(30.0, accSum / accN) else 5.0)
        val tol = 3.0 * noise
        val d = GnssPlausibility.distanceM(first.latitude, first.longitude, last.latitude, last.longitude)
        // Смещение больше, чем позволяет скорость: скачок координат при ложном допплере.
        if (d > e + tol) return false
        // Хаотичные скачки туда-обратно (длина ломаной намного больше пути по скорости), даже если концы серии рядом.
        if (path > 2.0 * e + 2.5 * noise * (r.size - 1)) return false
        // Скорость есть, а позиции стоят: допплер врёт либо дрейф.
        if (d < e * 0.5 - tol) return false
        if (d >= 2.0 * tol) {
            val bearings = r.mapNotNull { it.bearingDeg?.toDouble() }
            if (bearings.size * 2 >= r.size) {
                val mean = circularMean(bearings)
                val az = azimuthDeg(first, last)
                if (angleDiff(mean, az) > cfg.maxBearingDiffDeg) return false
            }
        }
        return true
    }

    // ---- помощники ---------------------------------------------------------------------------------------------------

    private fun toMoving() {
        phase = Phase.MOVING
        clearSlow()
        clearStill()
    }

    private fun raw(f: Fix, reason: Reason) = Result(f.latitude, f.longitude, held = false, reason = reason)

    private fun pinned(reason: Reason) = Result(anchor.lat, anchor.lon, held = true, reason = reason)

    private fun limit(f: Fix): Double = max(cfg.radiusM, 2.0 * (f.accuracyM ?: 0f))

    private fun weight(f: Fix): Double {
        val a = max(3.0, (f.accuracyM ?: 5f).toDouble())
        return 1.0 / (a * a)
    }

    private fun clearSlow() {
        slowN = 0
        slow.clear()
    }

    private fun clearCluster() {
        clusterN = 0
        cluster.clear()
    }

    private fun clearStill(keepAnchor: Boolean = false) {
        run.clear()
        clearCluster()
        farStartMs = -1L
        if (!keepAnchor) {
            anchor.clear()
            anchorN = 0
        }
    }

    /** Взвешенный центроид (для расстояний в сотни метров широта/долгота усредняются линейно). */
    private class Centroid {
        var lat = 0.0
        var lon = 0.0
        var w = 0.0

        fun add(la: Double, lo: Double, weight: Double) {
            val nw = w + weight
            lat = (lat * w + la * weight) / nw
            lon = (lon * w + lo * weight) / nw
            w = nw
        }

        fun set(la: Double, lo: Double, weight: Double) {
            lat = la
            lon = lo
            w = weight
        }

        fun clear() {
            lat = 0.0
            lon = 0.0
            w = 0.0
        }
    }

    private companion object {
        fun azimuthDeg(a: Fix, b: Fix): Double {
            val p1 = a.latitude * PI / 180.0
            val p2 = b.latitude * PI / 180.0
            val dl = (b.longitude - a.longitude) * PI / 180.0
            val y = sin(dl) * cos(p2)
            val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
            return (atan2(y, x) * 180.0 / PI + 360.0) % 360.0
        }

        fun circularMean(deg: List<Double>): Double {
            var s = 0.0
            var c = 0.0
            for (d in deg) {
                s += sin(d * PI / 180.0)
                c += cos(d * PI / 180.0)
            }
            return (atan2(s, c) * 180.0 / PI + 360.0) % 360.0
        }

        fun angleDiff(a: Double, b: Double): Double {
            val d = abs(a - b) % 360.0
            return if (d > 180.0) 360.0 - d else d
        }
    }
}
