package com.z_company.domain.util

import com.z_company.domain.track.TrackAnalysis
import com.z_company.domain.track.TrackPoint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min

/**
 * Очистка пройденного пути перед отрисовкой на карте поезда (SCREEN_SPECS §6.9, «Пройденный путь без зигзагов»).
 * На стоянке и при маневрах в парке GPS «гуляет» на десятки метров (скорость при этом ~0), и сырая ломаная даёт
 * зигзаги, «ёлочки» и петли поперёк путей. Порядок: [clusterStops] -> [removeSpikes] -> [simplify] (по участкам,
 * разрыв записи > [TrackAnalysis.GAP_MS] участки разделяет). Чистая логика: время и скорость точек сохраняются.
 */
object PathCleaner {
    /** Точка медленнее этого порога (км/ч) - стоянка/маневр: не удлиняет линию. */
    const val STILL_KMH = 3.0

    /** Радиус кластера стоянки от его центроида, м. */
    const val STILL_RADIUS_M = 50.0

    /** Ноги «шипа» (туда-и-обратно) не короче, м; короче - это обычный шум, не трогаем. */
    const val SPIKE_MIN_LEG_M = 20.0

    /** ... и не длиннее, м: длинный возврат - настоящий реверс, его не прячем. */
    const val SPIKE_MAX_LEG_M = 300.0

    /** Допуск упрощения Дугласа-Пекера, м. */
    const val SIMPLIFY_EPS_M = 4.0

    fun clean(points: List<TrackPoint>): List<TrackPoint> {
        if (points.size < 3) return points
        val clustered = clusterStops(points)
        val out = ArrayList<TrackPoint>(clustered.size)
        for (section in sections(clustered)) out += simplify(removeSpikes(section))
        return out
    }

    /** Участки без разрывов записи (> [TrackAnalysis.GAP_MS]). */
    fun sections(points: List<TrackPoint>): List<List<TrackPoint>> {
        val res = ArrayList<List<TrackPoint>>()
        var cur = ArrayList<TrackPoint>()
        for (p in points) {
            if (cur.isNotEmpty() && p.timeMs - cur.last().timeMs > TrackAnalysis.GAP_MS) {
                res += cur
                cur = ArrayList()
            }
            cur += p
        }
        if (cur.isNotEmpty()) res += cur
        return res
    }

    /**
     * Стоянка: подряд идущие точки со скоростью < [STILL_KMH] в пределах [STILL_RADIUS_M] от центроида кластера
     * заменяются ОДНОЙ точкой-центроидом (время первой точки; если стоянка длилась - ещё и копия на время последней,
     * чтобы длинная стоянка не рвала линию «разрывом по времени» между приездом и отправлением). Точки без скорости
     * считаются движением. Разрыв записи > [TrackAnalysis.GAP_MS] кластер закрывает.
     */
    fun clusterStops(points: List<TrackPoint>): List<TrackPoint> {
        val out = ArrayList<TrackPoint>(points.size)
        val cluster = ArrayList<TrackPoint>()
        var cLat = 0.0
        var cLon = 0.0
        // Бегущие суммы вместо sumOf по всему кластеру на каждой точке: было O(k^2) на долгой стоянке; порядок сложения
        // тот же, поэтому центроид получается бит-в-бит прежним.
        var sumLat = 0.0
        var sumLon = 0.0

        fun close() {
            if (cluster.isEmpty()) return
            val first = cluster.first()
            val last = cluster.last()
            val c = first.copy(
                latitude = cLat,
                longitude = cLon,
                speedKmh = cluster.mapNotNull { it.speedKmh }.average().takeIf { !it.isNaN() },
                accuracyM = cluster.mapNotNull { it.accuracyM }.minOrNull(),
            )
            out += c
            if (last.timeMs != first.timeMs) out += c.copy(timeMs = last.timeMs)
            cluster.clear()
            sumLat = 0.0
            sumLon = 0.0
        }

        for (p in points) {
            val still = (p.speedKmh ?: Double.MAX_VALUE) < STILL_KMH
            if (!still) {
                close()
                out += p
                continue
            }
            if (cluster.isNotEmpty()) {
                val gap = p.timeMs - cluster.last().timeMs > TrackAnalysis.GAP_MS
                val far = TrackAnalysis.distanceM(cLat, cLon, p.latitude, p.longitude) > STILL_RADIUS_M
                if (gap || far) close()
            }
            cluster += p
            sumLat += p.latitude
            sumLon += p.longitude
            cLat = sumLat / cluster.size
            cLon = sumLon / cluster.size
        }
        close()
        return out
    }

    /**
     * Возвраты и острые углы: точка, в которую линия приходит и тут же возвращается (обе ноги в
     * [SPIKE_MIN_LEG_M]..[SPIKE_MAX_LEG_M], а соседние точки сближаются: угол при вершине меньше ~60 градусов),
     * отбрасывается. Повторяется, пока есть что убирать (ограничено).
     */
    fun removeSpikes(section: List<TrackPoint>): List<TrackPoint> {
        var cur = section
        repeat(4) {
            if (cur.size < 3) return cur
            val out = ArrayList<TrackPoint>(cur.size)
            out += cur.first()
            var removed = false
            for (i in 1 until cur.size - 1) {
                val a = out.last()
                val p = cur[i]
                val n = cur[i + 1]
                val d1 = dist(a, p)
                val d2 = dist(p, n)
                val d3 = dist(a, n)
                val spike = d1 in SPIKE_MIN_LEG_M..SPIKE_MAX_LEG_M &&
                    d2 in SPIKE_MIN_LEG_M..SPIKE_MAX_LEG_M &&
                    d3 < min(d1, d2)
                if (spike) removed = true else out += p
            }
            out += cur.last()
            cur = out
            if (!removed) return cur
        }
        return cur
    }

    /**
     * Дуглас-Пекер ([epsM]) по участку; скорость оставленной точки - средняя по участку, который она «покрывает»
     * (чтобы раскраска по скорости не терялась).
     */
    fun simplify(section: List<TrackPoint>, epsM: Double = SIMPLIFY_EPS_M): List<TrackPoint> {
        val n = section.size
        if (n < 3) return section
        val lat0 = section[0].latitude
        val kx = cos(lat0 * PI / 180.0) * 111_320.0
        val ky = 110_540.0
        val xs = DoubleArray(n) { (section[it].longitude - section[0].longitude) * kx }
        val ys = DoubleArray(n) { (section[it].latitude - lat0) * ky }
        val keep = BooleanArray(n)
        keep[0] = true
        keep[n - 1] = true
        val stack = ArrayDeque<IntArray>()
        stack.addLast(intArrayOf(0, n - 1))
        while (stack.isNotEmpty()) {
            val (s, e) = stack.removeLast()
            var maxD = -1.0
            var idx = -1
            for (i in s + 1 until e) {
                val d = segDist(xs[i], ys[i], xs[s], ys[s], xs[e], ys[e])
                if (d > maxD) { maxD = d; idx = i }
            }
            if (idx >= 0 && maxD > epsM) {
                keep[idx] = true
                stack.addLast(intArrayOf(s, idx))
                stack.addLast(intArrayOf(idx, e))
            }
        }
        val out = ArrayList<TrackPoint>()
        var spanStart = 0
        for (i in 0 until n) {
            if (!keep[i]) continue
            val speeds = (spanStart..i).mapNotNull { section[it].speedKmh }
            val p = section[i]
            out += if (i == 0 || speeds.isEmpty()) p else p.copy(speedKmh = speeds.average())
            spanStart = i
        }
        return out
    }

    private fun segDist(px: Double, py: Double, ax: Double, ay: Double, bx: Double, by: Double): Double {
        val dx = bx - ax
        val dy = by - ay
        val len2 = dx * dx + dy * dy
        if (len2 == 0.0) return hypot(px - ax, py - ay)
        val t = (((px - ax) * dx + (py - ay) * dy) / len2).coerceIn(0.0, 1.0)
        return hypot(px - (ax + dx * t), py - (ay + dy * t))
    }

    private fun dist(a: TrackPoint, b: TrackPoint) = TrackAnalysis.distanceM(a.latitude, a.longitude, b.latitude, b.longitude)
}

/**
 * Кэш последней очистки пути: пройденный путь чистится в нескольких местах карты (отрисовка, поиск съезда на соседний
 * путь) по одним и тем же точкам - второй проход берётся из кэша. Ключ - число точек, первая и последняя точка.
 * Потокобезопасен без блокировок: хранится неизменяемая запись, гонка даёт максимум лишний пересчёт.
 */
class PathCleanCache {
    private class Entry(val size: Int, val first: TrackPoint?, val last: TrackPoint?, val result: List<TrackPoint>)

    private var entry: Entry? = null

    fun clean(points: List<TrackPoint>): List<TrackPoint> {
        val first = points.firstOrNull()
        val last = points.lastOrNull()
        val e = entry
        if (e != null && e.size == points.size && e.first == first && e.last == last) return e.result
        val result = PathCleaner.clean(points)
        entry = Entry(points.size, first, last, result)
        return result
    }
}
