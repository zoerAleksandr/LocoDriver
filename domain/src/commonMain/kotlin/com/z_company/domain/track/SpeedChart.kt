package com.z_company.domain.track

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlinx.datetime.offsetAt

/** Ось X графика скорости: путь вдоль маршрута (метры) или время (мс от начала записи). */
enum class ChartAxis { PATH, TIME }

/** Отсчёт ряда: время, пройденный путь от начала записи (м), скорость, индекс исходной точки трека. */
data class SpeedSample(val timeMs: Long, val distM: Double, val speedKmh: Double?, val index: Int)

/** Отметка остановки на графике: [x]..[xEnd] в единицах оси (xEnd = x для точки или оси «Путь»). */
data class ChartStop(val x: Double, val xEnd: Double, val name: String?, val arrivalMs: Long?, val departureMs: Long?)

/** Окно просмотра по оси X в единицах оси (м или мс от начала). */
data class ChartWindow(val start: Double, val end: Double) {
    val span: Double get() = end - start
}

/** Шкала цвета скорости (SCREEN_SPECS §6.9): общая для линии на карте и графика. */
object SpeedScale {
    /** (км/ч, RGB). */
    val STOPS: List<Pair<Int, Int>> = listOf(
        0 to 0xD32F2F, 30 to 0xF57C00, 60 to 0xFBC02D, 90 to 0x7CB342, 120 to 0x00897B, 160 to 0x1E88E5,
    )
    const val UNKNOWN_RGB = 0x78909C

    fun rgb(speedKmh: Double?): Int {
        if (speedKmh == null || speedKmh < 0) return UNKNOWN_RGB
        val first = STOPS.first()
        val last = STOPS.last()
        if (speedKmh <= first.first) return first.second
        if (speedKmh >= last.first) return last.second
        for (i in 1 until STOPS.size) {
            val (v1, c1) = STOPS[i]
            if (speedKmh <= v1) {
                val (v0, c0) = STOPS[i - 1]
                val t = (speedKmh - v0) / (v1 - v0)
                fun ch(shift: Int): Int {
                    val a = (c0 shr shift) and 0xFF
                    val b = (c1 shr shift) and 0xFF
                    return (a + (b - a) * t).toInt().coerceIn(0, 255)
                }
                return (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
            }
        }
        return last.second
    }
}

/** Чистая логика графика скорости (SCREEN_SPECS §6.9, этап 4b): ряды, зум/окно, прореживание, поиск точки. */
object SpeedChart {
    /** Ряды из точек трека (по времени): накопленный путь по прямым между соседними точками (через разрыв записи > 5 мин не растёт). */
    fun buildSeries(points: List<TrackPoint>): List<SpeedSample> {
        val out = ArrayList<SpeedSample>(points.size)
        var dist = 0.0
        for (i in points.indices) {
            val p = points[i]
            if (i > 0) {
                val q = points[i - 1]
                // Разрыв записи (> 5 мин, например другой сеанс «АВТО») путь не наращивает: прыжок не считается пройденным.
                if (p.timeMs - q.timeMs <= TrackAnalysis.GAP_MS) {
                    dist += TrackAnalysis.distanceM(q.latitude, q.longitude, p.latitude, p.longitude)
                }
            }
            out += SpeedSample(p.timeMs, dist, p.speedKmh, i)
        }
        return out
    }

    fun xOf(s: SpeedSample, axis: ChartAxis, t0: Long): Double =
        if (axis == ChartAxis.PATH) s.distM else (s.timeMs - t0).toDouble()

    fun total(samples: List<SpeedSample>, axis: ChartAxis): Double =
        if (samples.isEmpty()) 0.0 else xOf(samples.last(), axis, samples.first().timeMs)

    fun fullWindow(total: Double) = ChartWindow(0.0, total)

    /** Минимальный охват окна при зуме: 300 м или 2 минуты (но не больше всего ряда). */
    fun minSpan(axis: ChartAxis, total: Double): Double =
        (if (axis == ChartAxis.PATH) 300.0 else 120_000.0).coerceAtMost(total).coerceAtLeast(1.0)

    fun clamp(w: ChartWindow, total: Double): ChartWindow {
        val span = w.span.coerceIn(0.0, total)
        val start = w.start.coerceIn(0.0, (total - span).coerceAtLeast(0.0))
        return ChartWindow(start, start + span)
    }

    /** Масштабирование в [factor] раз (>1 — приблизить) вокруг якоря [anchorX] (точка под пальцем остаётся на месте). */
    fun zoom(w: ChartWindow, total: Double, factor: Double, anchorX: Double, minSpan: Double): ChartWindow {
        if (total <= 0.0 || factor <= 0.0) return w
        val newSpan = (w.span / factor).coerceIn(minSpan, total)
        val ratio = if (w.span > 0) ((anchorX - w.start) / w.span).coerceIn(0.0, 1.0) else 0.5
        val start = anchorX - ratio * newSpan
        return clamp(ChartWindow(start, start + newSpan), total)
    }

    /** Сдвиг окна на [dx] единиц оси (положительный — вправо). */
    fun pan(w: ChartWindow, total: Double, dx: Double): ChartWindow =
        clamp(ChartWindow(w.start + dx, w.end + dx), total)

    /**
     * Отсчёты для отрисовки в окне: не более ~4 на «столбец» ([columns] — ширина графика в px; первый, минимум,
     * максимум, последний), плюс по одному соседу за краями окна, чтобы линия доходила до границы.
     */
    fun visible(samples: List<SpeedSample>, axis: ChartAxis, w: ChartWindow, columns: Int): List<SpeedSample> {
        if (samples.isEmpty()) return emptyList()
        val t0 = samples.first().timeMs
        val lo = (lowerBound(samples, axis, t0, w.start) - 1).coerceAtLeast(0)
        val hi = lowerBound(samples, axis, t0, w.end).coerceAtMost(samples.lastIndex)
        if (hi - lo + 1 <= columns * 4 || w.span <= 0.0) return samples.subList(lo, hi + 1)
        val out = ArrayList<SpeedSample>(columns * 4)
        var i = lo
        while (i <= hi) {
            val col = column(xOf(samples[i], axis, t0), w, columns)
            var j = i
            var mn = i
            var mx = i
            while (j + 1 <= hi && column(xOf(samples[j + 1], axis, t0), w, columns) == col) {
                j++
                val v = samples[j].speedKmh ?: -1.0
                if (v < (samples[mn].speedKmh ?: -1.0)) mn = j
                if (v > (samples[mx].speedKmh ?: -1.0)) mx = j
            }
            listOf(i, mn, mx, j).distinct().sorted().forEach { out += samples[it] }
            i = j + 1
        }
        return out
    }

    private fun column(x: Double, w: ChartWindow, columns: Int): Int =
        floor(((x - w.start) / w.span) * columns).toInt()

    /** Первый индекс с x >= [x] (или последний, если таких нет). */
    private fun lowerBound(samples: List<SpeedSample>, axis: ChartAxis, t0: Long, x: Double): Int {
        var lo = 0
        var hi = samples.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (xOf(samples[mid], axis, t0) < x) lo = mid + 1 else hi = mid
        }
        return lo.coerceAtMost(samples.lastIndex)
    }

    /** Ближайший к [x] отсчёт (индекс в [samples]); -1, если ряд пуст. */
    fun nearest(samples: List<SpeedSample>, axis: ChartAxis, x: Double): Int {
        if (samples.isEmpty()) return -1
        val t0 = samples.first().timeMs
        val i = lowerBound(samples, axis, t0, x)
        if (i == 0) return 0
        val a = abs(xOf(samples[i - 1], axis, t0) - x)
        val b = abs(xOf(samples[i], axis, t0) - x)
        return if (a <= b) i - 1 else i
    }

    /** Индекс отсчёта по времени [timeMs] (ближайший); -1, если ряд пуст. */
    fun indexAtTime(samples: List<SpeedSample>, timeMs: Long): Int {
        if (samples.isEmpty()) return -1
        var lo = 0
        var hi = samples.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (samples[mid].timeMs < timeMs) lo = mid + 1 else hi = mid
        }
        val i = lo.coerceAtMost(samples.lastIndex)
        if (i == 0) return 0
        return if (abs(samples[i - 1].timeMs - timeMs) <= abs(samples[i].timeMs - timeMs)) i - 1 else i
    }

    /** Остановки на оси X: по времени прибытия (иначе отправления); вне записи (> 3 мин от ряда) пропускаются. */
    fun stopsOnAxis(samples: List<SpeedSample>, stops: List<TrainStopMark>, axis: ChartAxis): List<ChartStop> {
        if (samples.isEmpty()) return emptyList()
        val t0 = samples.first().timeMs
        val tolMs = 3 * 60_000L
        return stops.mapNotNull { st ->
            val arr = st.arrivalMs ?: st.departureMs ?: return@mapNotNull null
            val dep = st.departureMs ?: arr
            val ia = indexAtTime(samples, arr)
            if (abs(samples[ia].timeMs - arr) > tolMs) return@mapNotNull null
            val x = xOf(samples[ia], axis, t0)
            val xEnd = if (axis == ChartAxis.TIME) (dep - t0).toDouble().coerceAtLeast(x) else x
            ChartStop(x, xEnd, st.name, st.arrivalMs, st.departureMs)
        }.sortedBy { it.x }
    }

    /** Верх шкалы скорости: максимум по ряду, округлённый вверх до кратного 20, не меньше 40. */
    fun speedCeiling(samples: List<SpeedSample>): Double {
        val mx = samples.maxOfOrNull { it.speedKmh ?: 0.0 } ?: 0.0
        return (ceil(mx / 20.0) * 20.0).coerceAtLeast(40.0)
    }

    /** Смещение пояса отображения (например «GMT+3») в мс на момент [atMs]; при ошибке — 0. */
    fun zoneOffsetMs(zone: String, atMs: Long): Long = runCatching {
        kotlinx.datetime.TimeZone.of(zone).offsetAt(kotlinx.datetime.Instant.fromEpochMilliseconds(atMs)).totalSeconds * 1000L
    }.getOrDefault(0L)

    private val TIME_STEPS_MIN = listOf(1, 2, 5, 10, 15, 30, 60, 120, 180, 360, 720, 1440)

    /**
     * Метки оси времени (мс от начала записи [t0Ms]) с шагом в «круглые» минуты/часы по настенным часам UTC:
     * метка кратна шагу от полуночи UTC плюс [zoneOffsetMs] (пояс отображения), чтобы подписи были «12:30», а не «12:27».
     */
    fun timeTicks(t0Ms: Long, fromMs: Double, toMs: Double, maxCount: Int, zoneOffsetMs: Long): List<Double> {
        val span = toMs - fromMs
        if (span <= 0.0 || maxCount < 1) return emptyList()
        val raw = span / maxCount
        val stepMs = (TIME_STEPS_MIN.firstOrNull { it * 60_000.0 >= raw } ?: TIME_STEPS_MIN.last()) * 60_000L
        val out = ArrayList<Double>()
        // Работаем в «местных» миллисекундах, чтобы метки совпадали с часами отображаемого пояса.
        var local = ceil((t0Ms + fromMs + zoneOffsetMs) / stepMs) * stepMs
        while (local - zoneOffsetMs - t0Ms <= toMs + 1e-6) {
            out += local - zoneOffsetMs - t0Ms
            local += stepMs
        }
        return out
    }

    /** «Красивые» метки оси в интервале [from]..[to] с шагом 1/2/5·10^n; не более ~[maxCount]. */
    fun niceTicks(from: Double, to: Double, maxCount: Int): List<Double> {
        val span = to - from
        if (span <= 0.0 || maxCount < 1) return emptyList()
        val raw = span / maxCount
        val mag = 10.0.pow(floor(log10(raw)))
        val step = listOf(1.0, 2.0, 5.0, 10.0).map { it * mag }.first { it >= raw }
        val out = ArrayList<Double>()
        var t = ceil(from / step) * step
        while (t <= to + step * 1e-9) {
            out += t
            t += step
        }
        return out
    }
}
