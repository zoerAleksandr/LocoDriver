package com.z_company.domain.track

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/*
 * Привязка GPS к оси участка и нумерация километров (docs/KM_MAPPING_PLAN.md).
 * Чистая логика KMP без платформы. Координаты нигде не логируются.
 */

/** Локальная плоская метрика (метры): dy = Δlat·110540, dx = Δlon·111320·cos(lat). Для расстояний до ~15 км ошибка пренебрежимо мала. */
object FlatMetric {
    const val M_PER_DEG_LAT = 110_540.0
    const val M_PER_DEG_LON_EQ = 111_320.0
    const val DEG_TO_RAD = 0.017453292519943295
    fun kx(lat: Double): Double = cos(lat * DEG_TO_RAD) * M_PER_DEG_LON_EQ
}

/** Результат проекции: [kmM] - позиция по оси (м), [offsetM] - боковое отклонение (м), [vertex] - индекс начала лучшего отрезка. */
data class AxisHit(val kmM: Double, val offsetM: Double, val vertex: Int)

/**
 * Проекция точки на ось. Ось - накопленная длина полилинии (`AxisPoint.km`), результат интерполируется по отрезку (t в [0;1]).
 * Окно вокруг [hint] (вершины [hint-250; hint+251)) с аварийным полным перебором, если в окне d > [fullSearchAboveM] (по умолчанию 200 м)
 * и окно не покрыло всю ось. Побеждает строго меньшее расстояние (первый минимум).
 */
class AxisLocator(points: List<AxisPoint>) {
    private val n = points.size
    private val lat = DoubleArray(n) { points[it].lat }
    private val lon = DoubleArray(n) { points[it].lon }
    private val km = DoubleArray(n) { points[it].km }

    val size: Int get() = n
    val lengthKm: Double get() = if (n == 0) 0.0 else km[n - 1]

    /** null - ось короче двух точек. [hint] - индекс вершины прошлой привязки (null - полный перебор). */
    fun project(
        pLat: Double,
        pLon: Double,
        hint: Int? = null,
        back: Int = WINDOW_BACK,
        forward: Int = WINDOW_FORWARD,
        fullSearchAboveM: Double = FULL_SEARCH_ABOVE_M,
    ): AxisHit? {
        if (n < 2) return null
        val segs = n - 1
        if (hint == null) return scan(pLat, pLon, 0, segs)
        val from = max(0, hint - back)
        val to = min(segs, hint + forward)
        val inWindow = if (from < to) scan(pLat, pLon, from, to) else null
        val coversAll = from == 0 && to == segs
        if (inWindow != null && (inWindow.offsetM <= fullSearchAboveM || coversAll)) return inWindow
        return scan(pLat, pLon, 0, segs)
    }

    private fun scan(pLat: Double, pLon: Double, from: Int, to: Int): AxisHit? {
        val kx = FlatMetric.kx(pLat)
        val ky = FlatMetric.M_PER_DEG_LAT
        var best = Double.MAX_VALUE
        var bestKm = 0.0
        var bestIdx = from
        for (i in from until to) {
            val ax = (lon[i] - pLon) * kx
            val ay = (lat[i] - pLat) * ky
            val bx = (lon[i + 1] - pLon) * kx
            val by = (lat[i + 1] - pLat) * ky
            val dx = bx - ax
            val dy = by - ay
            val len2 = dx * dx + dy * dy
            val t = if (len2 <= 0.0) 0.0 else ((-ax * dx - ay * dy) / len2).coerceIn(0.0, 1.0)
            val px = ax + t * dx
            val py = ay + t * dy
            val d2 = px * px + py * py
            if (d2 < best) {
                best = d2
                bestKm = (km[i] + t * (km[i + 1] - km[i])) * 1000.0
                bestIdx = i
            }
        }
        return if (best == Double.MAX_VALUE) null else AxisHit(bestKm, sqrt(best), bestIdx)
    }

    companion object {
        const val WINDOW_BACK = 250
        const val WINDOW_FORWARD = 251
        const val FULL_SEARCH_ABOVE_M = 200.0
    }
}

/**
 * Живая привязка потока фиксов к оси. Состояние: прошлая позиция, счётчики «сбойных» и «вне маршрута». Один экземпляр - один поток.
 *
 * Порядок на фикс: порог точности (до привязки [Config.accuracyBeforeLockM], после - [Config.accuracyAfterLockM]) -> проекция
 * (окно + аварийный полный перебор) -> фильтр скачков (расстояние > [Config.jumpDistanceM] И скорость > [Config.jumpSpeedKmh] при
 * dt > [Config.jumpMinDtS]; после [Config.glitchResync] подряд фикс принимается) -> «вне маршрута» ([Config.offRouteFixes] подряд
 * дальше [Config.offRouteDistanceM]). При «вне маршрута» [FixResult.eventsAllowed] = false (события остановок/пробега не писать),
 * прошлая позиция не обновляется. На стоянке отображаемый км не дёргается ([FixResult.displayKmM]).
 */
class AxisTracker(private val locator: AxisLocator, private val cfg: Config = Config()) {

    data class Config(
        val accuracyBeforeLockM: Float = 80f,
        val accuracyAfterLockM: Float = 150f,
        val jumpDistanceM: Double = 500.0,
        val jumpSpeedKmh: Double = 200.0,
        val jumpMinDtS: Double = 0.2,
        val glitchResync: Int = 10,
        val offRouteDistanceM: Double = 200.0,
        val offRouteFixes: Int = 3,
        val standSpeedKmh: Double = 1.0,
        val standBandM: Double = 30.0,
    )

    enum class Status { ACCEPTED, WEAK_SIGNAL, COARSE_FIX, GLITCH }

    /**
     * [kmM]/[offsetM] - null у отброшенных фиксов. [offRoute] - вне маршрута; [eventsAllowed] - можно ли писать события.
     * [displayKmM] - км для показа (с гистерезисом на стоянке).
     */
    data class FixResult(
        val status: Status,
        val kmM: Double? = null,
        val offsetM: Double? = null,
        val offRoute: Boolean = false,
        val eventsAllowed: Boolean = false,
        val displayKmM: Double? = null,
    )

    private var locked = false
    private var lastKmM = 0.0
    private var lastVertex: Int? = null
    private var lastTimeMs = 0L
    private var glitchCount = 0
    private var offRouteCount = 0
    private var displayKm: Double? = null

    val isLocked: Boolean get() = locked

    fun reset() {
        locked = false; lastVertex = null; lastTimeMs = 0L; glitchCount = 0; offRouteCount = 0; displayKm = null
    }

    fun onFix(timeMs: Long, lat: Double, lon: Double, speedKmh: Double?, accuracyM: Float?): FixResult {
        if (accuracyM != null) {
            val limit = if (locked) cfg.accuracyAfterLockM else cfg.accuracyBeforeLockM
            if (accuracyM > limit) {
                return FixResult(if (locked) Status.COARSE_FIX else Status.WEAK_SIGNAL, displayKmM = displayKm)
            }
        }
        val hit = locator.project(lat, lon, lastVertex, fullSearchAboveM = cfg.offRouteDistanceM) ?: return FixResult(Status.WEAK_SIGNAL)
        val dtS = if (lastTimeMs > 0L) (timeMs - lastTimeMs) / 1000.0 else 0.0

        val onRoute = hit.offsetM <= cfg.offRouteDistanceM
        // Скачок считаем только между фиксами на маршруте: проекция далёкой точки ничего не говорит о движении по оси.
        if (locked && onRoute && dtS > cfg.jumpMinDtS) {
            val jumpM = abs(hit.kmM - lastKmM)
            if (jumpM > cfg.jumpDistanceM && jumpM / dtS * 3.6 > cfg.jumpSpeedKmh) {
                glitchCount++
                if (glitchCount < cfg.glitchResync) {
                    return FixResult(Status.GLITCH, displayKmM = displayKm)
                }
            }
        }
        glitchCount = 0

        if (!onRoute) {
            offRouteCount++
            val off = offRouteCount >= cfg.offRouteFixes
            return FixResult(Status.ACCEPTED, hit.kmM, hit.offsetM, offRoute = off, eventsAllowed = !off, displayKmM = displayKm)
        }
        offRouteCount = 0

        locked = true
        lastKmM = hit.kmM
        lastVertex = hit.vertex
        lastTimeMs = timeMs
        val prevDisplay = displayKm
        val standing = speedKmh != null && speedKmh < cfg.standSpeedKmh
        displayKm = if (standing && prevDisplay != null && abs(hit.kmM - prevDisplay) < cfg.standBandM) prevDisplay else hit.kmM
        return FixResult(Status.ACCEPTED, hit.kmM, hit.offsetM, offRoute = false, eventsAllowed = true, displayKmM = displayKm)
    }
}

/** Километр и пикет по позиции [kmValue] (путейский км с дробной частью, 5641.6). Км = floor(x)+1; пикет с переменным [n]. */
object Picket {
    const val DEFAULT_PER_KM = 10
    private const val EPS = 1e-6

    data class KmPicket(val km: Int, val picket: Int)

    fun of(kmValue: Double, perKm: Int = DEFAULT_PER_KM): KmPicket {
        val n = perKm.coerceAtLeast(1)
        val x = max(0.0, kmValue)
        val whole = floor(x)
        val frac = x - whole
        val pk = floor(frac * n + EPS).toInt().coerceIn(0, n - 1) + 1
        return KmPicket(whole.toInt() + 1, pk)
    }
}

/**
 * Нумерация: ось (км от начала) -> путейский км. Слой отдельный от геометрии. Якоря `[geoKm, realKm]` кусочно-линейны; допускается
 * убывающая нумерация (наклон < 0). [breaks] (geoKm) делят ось на независимые куски, у каждого свои якоря; в куске < 2 якорей -
 * сдвиг по одному якорю, без якорей - identity (по оси). За крайними якорями куска - экстраполяция наклоном крайнего отрезка.
 * [reversed] - «на каком конце участка км больше»: km(конца куска) - km(x), без смены геометрии.
 */
class RailKmMap(
    anchors: List<Pair<Double, Double>>,
    breaks: List<Double> = emptyList(),
    private val reversed: Boolean = false,
) {
    private val sorted = anchors.sortedBy { it.first }
    private val cuts = breaks.sorted()

    fun at(geoKm: Double): Double {
        val (start, end) = piece(geoKm)
        val local = pieceAnchors(start, end)
        val base = interpolate(local, geoKm)
        if (!reversed) return base
        return interpolate(local, end) - base
    }

    private fun piece(x: Double): Pair<Double, Double> {
        var s = Double.NEGATIVE_INFINITY
        for (c in cuts) {
            if (x < c - 1e-9) return s to c
            s = c
        }
        return s to Double.POSITIVE_INFINITY
    }

    private fun pieceAnchors(start: Double, end: Double): List<Pair<Double, Double>> =
        sorted.filter { it.first >= start - 1e-9 && it.first < end - 1e-9 }

    private fun interpolate(a: List<Pair<Double, Double>>, x: Double): Double {
        val xx = if (x.isInfinite()) (a.lastOrNull()?.first ?: 0.0) else x
        if (a.isEmpty()) return xx
        if (a.size == 1) return a[0].second + (xx - a[0].first)
        var i = 0
        while (i < a.size - 2 && xx > a[i + 1].first) i++
        val (g1, r1) = a[i]
        val (g2, r2) = a[i + 1]
        return r1 + (xx - g1) / (g2 - g1 + 1e-9) * (r2 - r1)
    }
}

/**
 * Пользовательская калибровка: аддитивная кусочно-линейная поправка Δ(geo) = realKm_i - base(geo_i) по якорям `[geoKm, realKm]`.
 * За крайними якорями поправка константна (без экстраполяции наклона). Хранится отдельно от базовой нумерации.
 */
class KmCalibration(private val base: RailKmMap, anchors: List<Pair<Double, Double>>) {
    private val pts = anchors.sortedBy { it.first }.map { (g, r) -> g to (r - base.at(g)) }

    fun delta(geoKm: Double): Double {
        if (pts.isEmpty()) return 0.0
        if (geoKm <= pts.first().first) return pts.first().second
        if (geoKm >= pts.last().first) return pts.last().second
        var i = 0
        while (i < pts.size - 2 && geoKm > pts[i + 1].first) i++
        val (g1, d1) = pts[i]
        val (g2, d2) = pts[i + 1]
        return d1 + (geoKm - g1) / (g2 - g1 + 1e-9) * (d2 - d1)
    }

    fun at(geoKm: Double): Double = base.at(geoKm) + delta(geoKm)
}
