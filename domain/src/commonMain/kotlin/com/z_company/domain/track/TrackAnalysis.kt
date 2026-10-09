package com.z_company.domain.track

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Стоянка, найденная по треку: скорость < 2 км/ч не менее 60 с (SCREEN_SPECS §6.9). */
data class TrackDwell(
    val latitude: Double,
    val longitude: Double,
    val arrivalMs: Long,
    val departureMs: Long,
)

/** Станция поезда с временами (для отметок остановок на карте). */
data class StationTimes(
    val name: String?,
    val arrivalMs: Long?,
    val departureMs: Long?,
    /** Проходная станция — без остановки, на карте не отмечается. */
    val isPassing: Boolean = false,
)

/** Остановка поезда на карте: из станции поезда (с названием) и/или стоянки по треку. */
data class TrainStopMark(
    val latitude: Double,
    val longitude: Double,
    val name: String?,
    val arrivalMs: Long?,
    val departureMs: Long?,
)

/** Чистая геометрия и разбор записанного трека для карты поезда (SCREEN_SPECS §6.9, этап 4а). */
object TrackAnalysis {
    const val STOP_SPEED_KMH = 2.0
    const val STOP_MIN_MS = 60_000L

    /** Разрыв между записанными точками стоянки, при котором считаем, что поезд стоял всё это время. */
    const val SPARSE_MS = 50_000L

    /**
     * Разрыв между записанными точками больше этого — «нет данных» (потеря сигнала/записи): стоянка пишется точкой
     * раз в 60 с, поэтому больший разрыв не доказывает остановку. Стоянка через такой разрыв не расширяется и не склеивается.
     */
    const val DATA_GAP_MS = 90_000L

    /** Разрыв между точками больше — линия не соединяется. */
    const val GAP_MS = 5 * 60_000L
    private const val EARTH_R = 6_371_000.0

    fun distanceM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val kx = cos((lat1 + lat2) / 2 * PI / 180.0)
        val dx = (lon2 - lon1) * kx * PI / 180.0 * EARTH_R
        val dy = (lat2 - lat1) * PI / 180.0 * EARTH_R
        return sqrt(dx * dx + dy * dy)
    }

    /** Ближайшая к (lat, lon) точка трека в пределах [toleranceM] метров; null, если ближе ничего нет. */
    fun nearestPoint(points: List<TrackPoint>, lat: Double, lon: Double, toleranceM: Double): TrackPoint? {
        var best: TrackPoint? = null
        var bestD = Double.MAX_VALUE
        for (p in points) {
            val d = distanceM(lat, lon, p.latitude, p.longitude)
            if (d < bestD) {
                bestD = d
                best = p
            }
        }
        return if (bestD <= toleranceM) best else null
    }

    /**
     * Стоянки по треку: подряд идущие точки со скоростью < [STOP_SPEED_KMH], у которых между первой и последней
     * точкой серии не менее [STOP_MIN_MS]. Точка без скорости и разрыв во времени серию прерывают.
     * Координаты стоянки — среднее по серии.
     */
    fun detectDwells(points: List<TrackPoint>): List<TrackDwell> {
        val result = mutableListOf<TrackDwell>()
        var start = -1
        fun close(endExclusive: Int) {
            if (start < 0) return
            val run = points.subList(start, endExclusive)
            // Запись прореживает стоянку до точки раз в 60 с (TrackThinner): у стоянки в 1–2 минуты остаётся 1–2 «медленные»
            // точки, а соседние точки (в движении) стоят по краям. Если до/после серии нет точек ≥ SPARSE_MS, поезд всё это
            // время не двигался (иначе точка в движении записалась бы через 5 с) — граница стоянки отодвигается на соседнюю точку.
            val prev = points.getOrNull(start - 1)?.takeIf { run.first().timeMs - it.timeMs in SPARSE_MS..DATA_GAP_MS }
            val next = points.getOrNull(endExclusive)?.takeIf { it.timeMs - run.last().timeMs in SPARSE_MS..DATA_GAP_MS }
            val from = prev?.timeMs ?: run.first().timeMs
            val to = next?.timeMs ?: run.last().timeMs
            val dense = run.size >= 2 && run.last().timeMs - run.first().timeMs >= STOP_MIN_MS
            val sparse = to - from >= STOP_MIN_MS || (run.size >= 2 && run.last().timeMs - run.first().timeMs >= SPARSE_MS)
            if (dense || sparse) {
                result += TrackDwell(
                    run.map { it.latitude }.average(), run.map { it.longitude }.average(),
                    from, to,
                )
            }
            start = -1
        }
        for (i in points.indices) {
            val p = points[i]
            val slow = p.speedKmh?.let { it < STOP_SPEED_KMH } == true
            if (i > 0 && p.timeMs - points[i - 1].timeMs > DATA_GAP_MS) close(i)
            if (slow) {
                if (start < 0) start = i
            } else {
                close(i)
            }
        }
        close(points.size)
        return result
    }

    /** Участок трека без данных (потеря сигнала/записи): между точками больше [DATA_GAP_MS]. */
    data class DataGap(val fromMs: Long, val toMs: Long, val fromLat: Double, val fromLon: Double, val toLat: Double, val toLon: Double)

    /** Разрывы записи длиннее [DATA_GAP_MS]: на графике и карте это «нет данных», а не стоянка. */
    fun dataGaps(points: List<TrackPoint>): List<DataGap> {
        val out = mutableListOf<DataGap>()
        for (i in 1 until points.size) {
            val a = points[i - 1]
            val b = points[i]
            if (b.timeMs - a.timeMs > DATA_GAP_MS) out += DataGap(a.timeMs, b.timeMs, a.latitude, a.longitude, b.latitude, b.longitude)
        }
        return out
    }

    /** Точка трека, ближайшая по времени к [timeMs], не дальше [tolMs]; null — трек этот момент не покрывает. */
    fun positionAt(points: List<TrackPoint>, timeMs: Long, tolMs: Long = 3 * 60_000L): TrackPoint? {
        var best: TrackPoint? = null
        var bestD = Long.MAX_VALUE
        for (p in points) {
            val d = abs(p.timeMs - timeMs)
            if (d < bestD) {
                bestD = d
                best = p
            }
        }
        return if (bestD <= tolMs) best else null
    }

    /**
     * Остановки для карты: станции поезда с временами (положение — стоянка по треку, совпавшая по времени, иначе
     * точка трека в момент прибытия) и стоянки по треку, не совпавшие ни с одной станцией (без названия).
     */
    fun stopMarks(points: List<TrackPoint>, dwells: List<TrackDwell>, stations: List<StationTimes>): List<TrainStopMark> {
        val marks = mutableListOf<TrainStopMark>()
        val used = BooleanArray(dwells.size)
        for (st in stations) {
            if (st.isPassing) continue
            val arr = st.arrivalMs ?: st.departureMs ?: continue
            val dep = st.departureMs ?: arr
            val from = arr - STOP_MIN_MS
            val to = dep + STOP_MIN_MS
            val dwellIdx = dwells.indices.firstOrNull { !used[it] && dwells[it].departureMs >= from && dwells[it].arrivalMs <= to }
            val pos: Pair<Double, Double> = if (dwellIdx != null) {
                used[dwellIdx] = true
                dwells[dwellIdx].latitude to dwells[dwellIdx].longitude
            } else {
                positionAt(points, arr)?.let { it.latitude to it.longitude } ?: continue
            }
            marks += TrainStopMark(pos.first, pos.second, st.name?.takeIf { it.isNotBlank() }, st.arrivalMs, st.departureMs)
        }
        dwells.forEachIndexed { i, d ->
            if (!used[i]) marks += TrainStopMark(d.latitude, d.longitude, null, d.arrivalMs, d.departureMs)
        }
        return marks.sortedBy { it.arrivalMs ?: it.departureMs ?: 0L }
    }

    /**
     * Прореживание для отрисовки: Дуглас–Пекер с допуском [epsilonM] метров по участкам между «якорями»
     * (разрывы времени и смена 10-км/ч «корзины» скорости — чтобы не терять цвет). Если точек осталось больше
     * [maxPoints], допуск растёт. Порядок и крайние точки сохраняются.
     */
    fun decimate(points: List<TrackPoint>, epsilonM: Double = 4.0, maxPoints: Int = 2500): List<TrackPoint> {
        if (points.size <= 2) return points
        var eps = epsilonM
        var out = decimateOnce(points, eps)
        while (out.size > maxPoints && eps < 5_000) {
            eps *= 2
            out = decimateOnce(points, eps)
        }
        return out
    }

    private fun bucket(p: TrackPoint): Int = ((p.speedKmh ?: -10.0) / 10.0).toInt()

    private fun decimateOnce(points: List<TrackPoint>, eps: Double): List<TrackPoint> {
        val keep = BooleanArray(points.size)
        val anchors = mutableListOf(0, points.lastIndex)
        for (i in 1 until points.size) {
            val gap = points[i].timeMs - points[i - 1].timeMs > GAP_MS
            if (gap || bucket(points[i]) != bucket(points[i - 1])) {
                anchors += i - 1
                anchors += i
            }
        }
        val sorted = anchors.distinct().sorted()
        sorted.forEach { keep[it] = true }
        for (k in 0 until sorted.size - 1) dp(points, sorted[k], sorted[k + 1], eps, keep)
        return points.filterIndexed { i, _ -> keep[i] }
    }

    private fun dp(pts: List<TrackPoint>, first: Int, last: Int, eps: Double, keep: BooleanArray) {
        val stack = ArrayDeque<Pair<Int, Int>>()
        stack.addLast(first to last)
        while (stack.isNotEmpty()) {
            val (a, b) = stack.removeLast()
            if (b - a < 2) continue
            var maxD = -1.0
            var idx = -1
            for (i in a + 1 until b) {
                val d = perpDistM(pts[i], pts[a], pts[b])
                if (d > maxD) {
                    maxD = d
                    idx = i
                }
            }
            if (maxD > eps && idx >= 0) {
                keep[idx] = true
                stack.addLast(a to idx)
                stack.addLast(idx to b)
            }
        }
    }

    private fun perpDistM(p: TrackPoint, a: TrackPoint, b: TrackPoint): Double {
        val kx = cos(a.latitude * PI / 180.0)
        val m = PI / 180.0 * EARTH_R
        val bx = (b.longitude - a.longitude) * kx * m
        val by = (b.latitude - a.latitude) * m
        val px = (p.longitude - a.longitude) * kx * m
        val py = (p.latitude - a.latitude) * m
        val len2 = bx * bx + by * by
        if (len2 == 0.0) return sqrt(px * px + py * py)
        val t = ((px * bx + py * by) / len2).coerceIn(0.0, 1.0)
        val dx = px - bx * t
        val dy = py - by * t
        return sqrt(dx * dx + dy * dy)
    }

    /** Время «чч:мм:сс» или «чч:мм» в поясе [zone] (например «GMT+3»). */
    fun clock(ms: Long, zone: String, seconds: Boolean = false): String {
        val t = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.of(zone))
        fun two(n: Int) = n.toString().padStart(2, '0')
        return two(t.hour) + ":" + two(t.minute) + if (seconds) ":" + two(t.second) else ""
    }

    /** Стоянка целыми минутами (не меньше 1 для ненулевой). */
    fun dwellMinutes(arrivalMs: Long, departureMs: Long): Int =
        ((departureMs - arrivalMs) / 60_000.0).roundToInt().coerceAtLeast(if (departureMs > arrivalMs) 1 else 0)
}
