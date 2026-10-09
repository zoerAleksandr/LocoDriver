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

/** Диапазон скорости на общей цветовой шкале карты и графика. */
enum class SpeedBand { GREEN, YELLOW, ORANGE, RED, UNKNOWN }

/** Профиль шкалы: грузовой (по умолчанию) или пассажирский/пригородный поезд. */
enum class SpeedProfile { FREIGHT, PASSENGER }

/**
 * Шкала цвета скорости (SCREEN_SPECS §6.9): ОДНА для линии на карте, графика и легенды.
 * Грузовой: до 20 км/ч включительно - зелёный, (20; 70] - жёлтый, (70; 80] - оранжевый, свыше 80 - красный.
 * Пассажирский/пригородный (допущение: пороги выше 20 км/ч не заданы, кроме красного > 160): до 20 - зелёный,
 * (20; 120] - жёлтый, (120; 160] - оранжевый, свыше 160 - красный. Неизвестная (null/отрицательная) - серый.
 */
object SpeedScale {
    const val UNKNOWN_RGB = 0x78909C

    /** Верхние границы (включительно) зелёного, жёлтого и оранжевого диапазонов, км/ч. */
    fun limits(profile: SpeedProfile): Triple<Double, Double, Double> = when (profile) {
        SpeedProfile.FREIGHT -> Triple(20.0, 70.0, 80.0)
        SpeedProfile.PASSENGER -> Triple(20.0, 120.0, 160.0)
    }

    fun band(speedKmh: Double?, profile: SpeedProfile = SpeedProfile.FREIGHT): SpeedBand {
        if (speedKmh == null || speedKmh < 0 || speedKmh.isNaN()) return SpeedBand.UNKNOWN
        val (g, y, o) = limits(profile)
        return when {
            speedKmh <= g -> SpeedBand.GREEN
            speedKmh <= y -> SpeedBand.YELLOW
            speedKmh <= o -> SpeedBand.ORANGE
            else -> SpeedBand.RED
        }
    }

    /** RGB диапазона; [dark] - более светлые оттенки для тёмного фона (контраст ≥ 3:1 к фону в обеих темах). */
    fun rgb(band: SpeedBand, dark: Boolean = false): Int = when (band) {
        SpeedBand.GREEN -> if (dark) 0x66BB6A else 0x2E7D32
        SpeedBand.YELLOW -> if (dark) 0xFDD835 else 0xF9A825
        SpeedBand.ORANGE -> if (dark) 0xFFA040 else 0xEF6C00
        SpeedBand.RED -> if (dark) 0xEF5350 else 0xD32F2F
        SpeedBand.UNKNOWN -> UNKNOWN_RGB
    }

    fun rgb(speedKmh: Double?, profile: SpeedProfile = SpeedProfile.FREIGHT, dark: Boolean = false): Int =
        rgb(band(speedKmh, profile), dark)

    /** Подписи порогов для легенды: (диапазон, подпись). */
    fun legend(profile: SpeedProfile): List<Pair<SpeedBand, String>> {
        val (g, y, o) = limits(profile)
        fun n(v: Double) = v.toInt().toString()
        return listOf(
            SpeedBand.GREEN to "≤${n(g)}",
            SpeedBand.YELLOW to "${n(g)}–${n(y)}",
            SpeedBand.ORANGE to "${n(y)}–${n(o)}",
            SpeedBand.RED to ">${n(o)}",
        )
    }

    /**
     * Пассажирский/пригородный ли поезд по категории `Train.trainCategory()` (по номеру поезда). Грузовые, служебные,
     * «без пассажиров» (порожний моторвагон), толкачи/резервные локомотивы, почтово-багажные и грузопассажирские -
     * грузовая шкала.
     */
    fun isPassengerCategory(category: String?): Boolean {
        if (category.isNullOrBlank()) return false
        val c = category.trim().lowercase()
        return listOf(
            "скорые", "скоростные", "высокоскоростные", "пассажирские", "обслуживаемые моторвагон",
            "пригородные", "туристические", "людские",
        ).any { c.startsWith(it) }
    }

    /** Пассажирский ли поезд по его категории; поезд неизвестен - false (грузовая шкала). */
    fun isPassengerTrain(train: com.z_company.domain.entities.route.Train?): Boolean =
        train != null && isPassengerCategory(
            with(com.z_company.domain.entities.route.UtilsForEntities) { train.trainCategory() },
        )

    fun profileForCategory(category: String?): SpeedProfile =
        if (isPassengerCategory(category)) SpeedProfile.PASSENGER else SpeedProfile.FREIGHT
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

    /**
     * Окно просмотра при смене оси X («Путь» <-> «Время»): те же точки трека. Левая граница - первая точка ряда
     * внутри окна, правая - последняя; их индексы пересчитываются в единицы новой оси. Полное окно остаётся полным.
     * [w] - в единицах оси [from]; результат - в единицах оси [to].
     */
    fun convertWindow(samples: List<SpeedSample>, from: ChartAxis, to: ChartAxis, w: ChartWindow): ChartWindow {
        if (samples.isEmpty() || from == to) return w
        val t0 = samples.first().timeMs
        val totalFrom = total(samples, from)
        val totalTo = total(samples, to)
        val eps = 1e-6 * totalFrom.coerceAtLeast(1.0)
        if (w.start <= eps && w.end >= totalFrom - eps) return fullWindow(totalTo)
        val iStart = lowerBound(samples, from, t0, w.start)
        // Последний индекс с x <= end (на стоянке «Пути» несколько точек с одним x - берём последнюю).
        var lo = 0
        var hi = samples.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (xOf(samples[mid], from, t0) <= w.end + eps) lo = mid + 1 else hi = mid
        }
        val iEnd = (lo - 1).coerceAtLeast(0)
        val a = xOf(samples[iStart], to, t0)
        val b = xOf(samples[maxOf(iEnd, iStart)], to, t0)
        val min = minSpan(to, totalTo)
        val span = (b - a).coerceAtLeast(min)
        val centre = (a + b) / 2
        return clamp(ChartWindow(centre - span / 2, centre + span / 2), totalTo)
    }

    /** Допуск «край виден»: 0,1 % ряда (не меньше 1 единицы оси). */
    private fun edgeTolerance(total: Double) = (total * 1e-3).coerceAtLeast(1.0)

    /** Показывать ли «В начало» (левый край ряда за окном). Если виден весь ряд - нет. */
    fun canScrollToStart(w: ChartWindow, total: Double): Boolean = total > 0.0 && w.start > edgeTolerance(total)

    /** Показывать ли «В конец» (правый край ряда за окном). */
    fun canScrollToEnd(w: ChartWindow, total: Double): Boolean = total > 0.0 && w.end < total - edgeTolerance(total)

    /** Окно того же охвата, прижатое к началу ([toEnd] = false) или к концу ряда. */
    fun edgeWindow(w: ChartWindow, total: Double, toEnd: Boolean): ChartWindow {
        val span = w.span.coerceIn(0.0, total)
        return if (toEnd) ChartWindow(total - span, total) else ChartWindow(0.0, span)
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
