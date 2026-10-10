package com.z_company.domain.util

import com.z_company.domain.track.TrackAnalysis
import com.z_company.domain.track.TrackPoint
import com.z_company.domain.util.TrainMapRoute.SnappedPoint

/**
 * «Фактические рельсы» карты поезда (SCREEN_SPECS §6.9, «вне путей»): кроме рельсовой линии плеча, по которой
 * проложен маршрут, поезд может ехать по соседнему/параллельному пути многопутного участка. Тогда «вне путей» и
 * привязка считаются относительно ЛЮБОЙ из известных линий (линия плеча + «линии по факту»), а линия по факту
 * строится сервером (`route/length` с via-точками из трека) только когда трек устойчиво ушёл от всех линий.
 * Чистая логика без платформенных зависимостей.
 */
object ActualRails {
    /** Отклонение от всех известных линий, с которого участок трека считается «ушедшим на другой путь», м. */
    const val DEVIATION_M = 15.0

    /** Минимум точек и длина участка по треку, чтобы строить линию по факту (отсекает выбросы GPS). */
    const val MIN_RUN_POINTS = 5
    const val MIN_RUN_M = 200.0

    /** Via-точек в запросе не больше [MAX_VIA] (включая первую и последнюю); шаг между ними ~[VIA_STEP_M]. */
    const val MAX_VIA = 6
    const val VIA_STEP_M = 400.0

    /** Линия по факту не длиннее участка трека больше чем в [MAX_DETOUR_RATIO] раз плюс [DETOUR_SLACK_M]. */
    const val MAX_DETOUR_RATIO = 1.5
    const val DETOUR_SLACK_M = 300.0

    /** Сколько линий по факту хранится на экране (самые новые). */
    const val MAX_FACT_LINES = 6

    /** Скорость, ниже которой точка не считается движением (стоянка рядом с путями даёт шум), км/ч. */
    private const val MOVING_KMH = 3.0

    /** Проекция на ближайшую из линий; [lineIndex] — индекс в переданном списке. */
    data class Projection(val projection: TrainMapRoute.LineProjection, val lineIndex: Int)

    /** Ближайшая из линий [lines] к точке (линии короче двух точек пропускаются); null — подходящих линий нет. */
    fun project(lines: List<List<Pair<Double, Double>>>, lat: Double, lon: Double): Projection? {
        var best: Projection? = null
        for ((i, line) in lines.withIndex()) {
            if (line.size < 2) continue
            val pr = TrainMapRoute.project(line, TrainMapRoute.cumulativeM(line), lat, lon) ?: continue
            if (best == null || pr.offsetM < best.projection.offsetM) best = Projection(pr, i)
        }
        return best
    }

    /** Отклонение (м) от ближайшей из линий; null — линий нет. */
    fun offsetFromAny(lines: List<List<Pair<Double, Double>>>, lat: Double, lon: Double): Double? =
        project(lines, lat, lon)?.projection?.offsetM

    /**
     * Точки пути для отрисовки: точка не дальше [toleranceM] от ближайшей из линий заменяется проекцией на неё
     * (`onRail = true`), остальные остаются сырыми («вне путей»). Нет ни одной линии — все точки сырые и `onRail = true`.
     */
    fun snapMarked(
        points: List<TrackPoint>,
        lines: List<List<Pair<Double, Double>>>,
        toleranceM: Double = TrainMapRoute.SNAP_TOLERANCE_M,
    ): List<SnappedPoint> {
        val usable = lines.filter { it.size >= 2 }
        if (usable.isEmpty()) return points.map { SnappedPoint(it, true) }
        val cums = usable.map { TrainMapRoute.cumulativeM(it) }
        return points.map { p ->
            val best = nearest(usable, cums, p.latitude, p.longitude)
            if (best != null && best.offsetM <= toleranceM) {
                SnappedPoint(p.copy(latitude = best.latitude, longitude = best.longitude), true)
            } else {
                SnappedPoint(p, false)
            }
        }
    }

    private fun nearest(
        lines: List<List<Pair<Double, Double>>>,
        cums: List<DoubleArray>,
        lat: Double,
        lon: Double,
    ): TrainMapRoute.LineProjection? {
        var best: TrainMapRoute.LineProjection? = null
        for ((i, line) in lines.withIndex()) {
            val pr = TrainMapRoute.project(line, cums[i], lat, lon) ?: continue
            if (best == null || pr.offsetM < best.offsetM) best = pr
        }
        return best
    }

    /**
     * Положение для расчётов ВДОЛЬ линии плеча (остаток до станций, план прибытия): если машинист на «линии по факту»
     * (дальше [TrainMapRoute.SNAP_TOLERANCE_M] от линии плеча, но на рельсах), берётся проекция на линию плеча —
     * иначе расчёт сообщил бы «вне путей», хотя поезд на рельсах. Иначе положение без изменений.
     */
    fun alongRouteFix(
        routeLine: List<Pair<Double, Double>>?,
        lines: List<List<Pair<Double, Double>>>,
        fix: GeoFix?,
    ): GeoFix? {
        if (fix == null || routeLine == null || routeLine.size < 2) return fix
        val onRoute = TrainMapRoute.project(routeLine, TrainMapRoute.cumulativeM(routeLine), fix.latitude, fix.longitude) ?: return fix
        if (onRoute.offsetM <= TrainMapRoute.SNAP_TOLERANCE_M) return fix
        val any = offsetFromAny(lines, fix.latitude, fix.longitude) ?: return fix
        return if (any <= TrainMapRoute.SNAP_TOLERANCE_M) fix.copy(latitude = onRoute.latitude, longitude = onRoute.longitude) else fix
    }

    /**
     * Последний непрерывный (без разрывов > [TrackAnalysis.GAP_MS]) участок трека в движении, где точки отклонены
     * больше [deviationM] от ВСЕХ линий [lines]; null — такого участка нет или он короче [MIN_RUN_POINTS]/[MIN_RUN_M]
     * (выбросы GPS). Закончившийся съезд тоже годится: линию по факту стоит построить.
     */
    fun deviationRun(
        points: List<TrackPoint>,
        lines: List<List<Pair<Double, Double>>>,
        deviationM: Double = DEVIATION_M,
    ): List<TrackPoint>? {
        val usable = lines.filter { it.size >= 2 }
        if (usable.isEmpty() || points.size < MIN_RUN_POINTS) return null
        val cums = usable.map { TrainMapRoute.cumulativeM(it) }
        var best: List<TrackPoint>? = null
        val cur = ArrayList<TrackPoint>()
        fun flush() {
            if (cur.size >= MIN_RUN_POINTS && pathLengthM(cur) >= MIN_RUN_M) best = ArrayList(cur)
            cur.clear()
        }
        var prev: TrackPoint? = null
        for (p in points) {
            if (prev != null && p.timeMs - prev.timeMs > TrackAnalysis.GAP_MS) flush()
            prev = p
            if ((p.speedKmh ?: Double.MAX_VALUE) < MOVING_KMH) continue
            val off = nearest(usable, cums, p.latitude, p.longitude)?.offsetM ?: continue
            if (off > deviationM) cur += p else flush()
        }
        flush()
        return best
    }

    fun pathLengthM(points: List<TrackPoint>): Double {
        var sum = 0.0
        for (i in 1 until points.size) {
            sum += TrackAnalysis.distanceM(points[i - 1].latitude, points[i - 1].longitude, points[i].latitude, points[i].longitude)
        }
        return sum
    }

    /**
     * Via-точки для запроса линии по факту: первая и последняя точки участка и промежуточные примерно через
     * [VIA_STEP_M] по треку, всего не больше [MAX_VIA]. Пары (широта, долгота).
     */
    fun viaPoints(run: List<TrackPoint>): List<Pair<Double, Double>> {
        if (run.size < 2) return emptyList()
        val total = pathLengthM(run)
        val segments = (total / VIA_STEP_M).toInt().coerceIn(1, MAX_VIA - 1)
        val out = ArrayList<Pair<Double, Double>>(segments + 1)
        out += run.first().latitude to run.first().longitude
        var acc = 0.0
        var next = 1
        for (i in 1 until run.size) {
            acc += TrackAnalysis.distanceM(run[i - 1].latitude, run[i - 1].longitude, run[i].latitude, run[i].longitude)
            if (next < segments && acc >= total * next / segments) {
                out += run[i].latitude to run[i].longitude
                next++
            }
        }
        out += run.last().latitude to run.last().longitude
        return out
    }

    /**
     * Принять ли линию [line], полученную от сервера для участка [run]: все точки участка не дальше
     * [TrainMapRoute.SNAP_TOLERANCE_M] от неё (линия проходит там, где поезд едет) и она не делает большой объезд
     * (длина ≤ [MAX_DETOUR_RATIO] × длина участка + [DETOUR_SLACK_M]).
     */
    fun accept(run: List<TrackPoint>, line: List<Pair<Double, Double>>): Boolean {
        if (line.size < 2 || run.size < 2) return false
        val cum = TrainMapRoute.cumulativeM(line)
        for (p in run) {
            val off = TrainMapRoute.project(line, cum, p.latitude, p.longitude)?.offsetM ?: return false
            if (off > TrainMapRoute.SNAP_TOLERANCE_M) return false
        }
        return cum.last() <= pathLengthM(run) * MAX_DETOUR_RATIO + DETOUR_SLACK_M
    }

    /** Ключ участка для «не запрашивать повторно»: концы участка, округление ~100 м. */
    fun runKey(run: List<TrackPoint>): String {
        fun r(v: Double) = (v * 1000).toLong()
        val a = run.first()
        val b = run.last()
        return "${r(a.latitude)},${r(a.longitude)};${r(b.latitude)},${r(b.longitude)}"
    }

    /** Добавить линию, не превышая [MAX_FACT_LINES] (старые вытесняются). */
    fun withFactLine(
        existing: List<List<Pair<Double, Double>>>,
        line: List<Pair<Double, Double>>,
    ): List<List<Pair<Double, Double>>> = (existing + listOf(line)).takeLast(MAX_FACT_LINES)
}
