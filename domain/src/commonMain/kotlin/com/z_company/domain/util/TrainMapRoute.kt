package com.z_company.domain.util

import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.entities.setting.ServicePhaseStationKind
import kotlin.math.PI
import kotlin.math.cos
import com.z_company.domain.track.TrackAnalysis
import com.z_company.domain.track.TrackPoint

/** Станция плеча на карте поезда (SCREEN_SPECS §6.9). [kind] null — крайняя (отправление/прибытие). */
data class TrainMapStop(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val kind: ServicePhaseStationKind? = null,
)

/** Чистая логика будущего маршрута на карте поезда (состояние 2, SCREEN_SPECS §6.9). */
object TrainMapRoute {

    /** Станции плеча с координатами по порядку. Маршрут строится, только если их не меньше двух. */
    fun stops(phase: ServicePhase): List<TrainMapStop> = buildList {
        phase.departurePoint?.let { add(TrainMapStop(phase.departureStation, it.latitude, it.longitude)) }
        phase.intermediateStations.forEach {
            val lat = it.latitude
            val lon = it.longitude
            if (lat != null && lon != null) add(TrainMapStop(it.name, lat, lon, it.kind))
        }
        phase.arrivalPoint?.let { add(TrainMapStop(phase.arrivalStation, it.latitude, it.longitude)) }
    }

    fun hasRoute(stops: List<TrainMapStop>): Boolean = stops.size >= 2

    /** Проекция точки на рельсовую линию: расстояние вдоль линии от её начала, отклонение и точка на линии. */
    data class LineProjection(
        val alongM: Double,
        val offsetM: Double,
        val latitude: Double,
        val longitude: Double,
        val segment: Int,
    )

    /**
     * Накопленная длина линии (м) к каждой вершине. Линии (рельсовая линия плеча, «линии по факту») неизменяемы и живут
     * долго, а расчёт нужен на каждое обновление GPS в нескольких местах - результат запоминается по ИДЕНТИЧНОСТИ списка
     * (небольшой кэш из последних линий). Возвращённый массив не изменять.
     */
    fun cumulativeM(line: List<Pair<Double, Double>>): DoubleArray {
        if (line.size < CUM_CACHE_MIN_SIZE) return computeCumulative(line)
        val cached = cumCache
        for (e in cached) if (e.line === line && e.cum.size == line.size) return e.cum
        val cum = computeCumulative(line)
        cumCache = (listOf(CumEntry(line, cum)) + cached).take(CUM_CACHE_SIZE)
        return cum
    }

    private class CumEntry(val line: List<Pair<Double, Double>>, val cum: DoubleArray)

    // Копирование-при-записи: чтение без блокировок из любого потока, гонка даёт максимум лишний пересчёт.
    private var cumCache: List<CumEntry> = emptyList()
    private const val CUM_CACHE_SIZE = 8
    private const val CUM_CACHE_MIN_SIZE = 32

    private fun computeCumulative(line: List<Pair<Double, Double>>): DoubleArray {
        val cum = DoubleArray(line.size)
        for (i in 1 until line.size) {
            cum[i] = cum[i - 1] + TrackAnalysis.distanceM(line[i - 1].first, line[i - 1].second, line[i].first, line[i].second)
        }
        return cum
    }

    /** Метров в градусе широты - нижняя граница расстояния до точки по одной широте (distanceM >= это * |dLat|). */
    private const val LAT_BOUND_M_PER_DEG = 111_000.0

    /**
     * Проекция (lat, lon) на ломаную [line] (пары «широта, долгота») среди отрезков, начиная с [fromSegment].
     * null — линия короче двух точек.
     */
    fun project(
        line: List<Pair<Double, Double>>,
        cum: DoubleArray,
        lat: Double,
        lon: Double,
        fromSegment: Int = 0,
    ): LineProjection? {
        if (line.size < 2) return null
        val kx = cos(lat * PI / 180.0)
        var best: LineProjection? = null
        val start = fromSegment.coerceAtLeast(0)
        // Отсев по широте: расстояние до отрезка не меньше |разности широт| * 111 км/град. Верхняя граница ответа -
        // расстояние до ближайшей из ~64 равномерно выбранных вершин; отрезки, у которых вся широта дальше этой границы,
        // не могут быть ни лучшими, ни равными лучшему (строго больше), поэтому результат тот же, но без тригонометрии
        // на каждом отрезке (раньше: O(длина линии) haversine-подобных расчётов на каждую точку трека).
        var bound = Double.MAX_VALUE
        if (line.size - start > 128) {
            val step = (line.size - start) / 64
            var i = start
            while (i < line.size) {
                val d = TrackAnalysis.distanceM(lat, lon, line[i].first, line[i].second)
                if (d < bound) bound = d
                i += step
            }
        }
        for (i in start until line.size - 1) {
            val (aLat, aLon) = line[i]
            val (bLat, bLon) = line[i + 1]
            if (bound != Double.MAX_VALUE) {
                val lo = if (aLat < bLat) aLat else bLat
                val hi = if (aLat < bLat) bLat else aLat
                val outside = if (lat < lo) lo - lat else if (lat > hi) lat - hi else 0.0
                val limit = if (best != null && best.offsetM < bound) best.offsetM else bound
                // Запас 1% + 1 м против разницы линейной модели проекции и distanceM.
                if (outside * LAT_BOUND_M_PER_DEG > limit * 1.01 + 1.0) continue
            }
            val dx = (bLon - aLon) * kx
            val dy = bLat - aLat
            val len2 = dx * dx + dy * dy
            val t = if (len2 == 0.0) 0.0 else ((((lon - aLon) * kx) * dx + (lat - aLat) * dy) / len2).coerceIn(0.0, 1.0)
            val pLat = aLat + dy * t
            val pLon = aLon + (bLon - aLon) * t
            val offset = TrackAnalysis.distanceM(lat, lon, pLat, pLon)
            if (best == null || offset < best.offsetM) {
                best = LineProjection(cum[i] + (cum[i + 1] - cum[i]) * t, offset, pLat, pLon, i)
            }
        }
        return best
    }

    /** Ближайшая станция впереди по ходу и расстояние до неё ВДОЛЬ рельсовой линии. */
    data class Ahead(val index: Int, val distanceM: Double)

    /**
     * Ближайшая станция впереди для [fix], считая по рельсовой линии [line] с сервера (не по прямым между станциями):
     * положение и станции проецируются на линию, впереди — первая станция, чья проекция не ближе положения (с допуском 1 м);
     * проекции станций берутся не убывающими (поиск продолжается с отрезка предыдущей станции). До начала маршрута —
     * первая станция. null — нет положения, линии (меньше двух точек) или станций.
     */
    fun nextAhead(stops: List<TrainMapStop>, line: List<Pair<Double, Double>>?, fix: GeoFix?): Ahead? {
        if (fix == null || line == null || line.size < 2 || stops.isEmpty()) return null
        val cum = cumulativeM(line)
        val me = project(line, cum, fix.latitude, fix.longitude) ?: return null
        var seg = 0
        for ((i, st) in stops.withIndex()) {
            val p = project(line, cum, st.latitude, st.longitude, seg) ?: return null
            seg = p.segment
            if (p.alongM >= me.alongM - 1.0) return Ahead(i, (p.alongM - me.alongM).coerceAtLeast(0.0))
        }
        // Все станции позади — впереди последняя (дистанция 0).
        return Ahead(stops.lastIndex, 0.0)
    }

    /** Допуск привязки пройденного пути к рельсам, м (SCREEN_SPECS §6.9); дальше — сырая линия «вне путей». */
    const val SNAP_TOLERANCE_M = 50.0

    /** Точка пути для отрисовки; [onRail] — привязана к рельсовой линии (иначе сырая, «вне путей»). */
    data class SnappedPoint(val point: TrackPoint, val onRail: Boolean)

    /**
     * Точки пути для отрисовки, привязанные к рельсовой линии: точка с отклонением от линии ≤ [toleranceM]
     * заменяется своей проекцией на линию (сглаживает шум GPS), остальные остаются сырыми и помечаются
     * `onRail = false`. Время и скорость сохраняются. Без линии все точки сырые и `onRail = true`
     * (отмечать «вне путей» не от чего).
     */
    fun snapMarked(
        points: List<TrackPoint>,
        line: List<Pair<Double, Double>>?,
        toleranceM: Double = SNAP_TOLERANCE_M,
    ): List<SnappedPoint> {
        if (line == null || line.size < 2) return points.map { SnappedPoint(it, true) }
        val cum = cumulativeM(line)
        return points.map { p ->
            val pr = project(line, cum, p.latitude, p.longitude)
            if (pr != null && pr.offsetM <= toleranceM) SnappedPoint(p.copy(latitude = pr.latitude, longitude = pr.longitude), true)
            else SnappedPoint(p, false)
        }
    }

    fun snapToLine(
        points: List<TrackPoint>,
        line: List<Pair<Double, Double>>?,
        toleranceM: Double = SNAP_TOLERANCE_M,
    ): List<TrackPoint> = snapMarked(points, line, toleranceM).map { it.point }

    /** Отклонение (м) точки от рельсовой линии; null — нет линии. */
    fun offsetFromLine(line: List<Pair<Double, Double>>?, lat: Double, lon: Double): Double? {
        if (line == null || line.size < 2) return null
        return project(line, cumulativeM(line), lat, lon)?.offsetM
    }

    /** Движение по маршруту: ближайшая станция впереди и остаток до конечной, по рельсовой линии. */
    data class Progress(
        val nextIndex: Int,
        val nextDistanceM: Double,
        val terminalDistanceM: Double,
    )

    /**
     * Расстояния до ближайшей станции впереди и до конечной станции плеча (последняя в [stops]) вдоль рельсовой линии.
     * null — нет положения/линии/станций.
     */
    fun progress(stops: List<TrainMapStop>, line: List<Pair<Double, Double>>?, fix: GeoFix?): Progress? {
        if (fix == null || line == null || line.size < 2 || stops.isEmpty()) return null
        val cum = cumulativeM(line)
        val me = project(line, cum, fix.latitude, fix.longitude) ?: return null
        var seg = 0
        var next: Ahead? = null
        var terminalAlong = 0.0
        for ((i, st) in stops.withIndex()) {
            val p = project(line, cum, st.latitude, st.longitude, seg) ?: return null
            seg = p.segment
            if (next == null && p.alongM >= me.alongM - 1.0) next = Ahead(i, (p.alongM - me.alongM).coerceAtLeast(0.0))
            terminalAlong = p.alongM
        }
        val n = next ?: Ahead(stops.lastIndex, 0.0)
        return Progress(n.index, n.distanceM, (terminalAlong - me.alongM).coerceAtLeast(0.0))
    }

    /**
     * Скорость для оценки времени прибытия, км/ч: живая, если ≥ [Eta.MIN_LIVE_KMH]; иначе средняя по точкам трека со
     * скоростью ≥ [Eta.MIN_LIVE_KMH] за последние [Eta.WINDOW_MS]; иначе null (оценки нет).
     */
    fun etaSpeedKmh(liveKmh: Double?, track: List<TrackPoint>, nowMs: Long): Double? {
        if (liveKmh != null && liveKmh >= Eta.MIN_LIVE_KMH) return liveKmh
        val v = track.asSequence().filter { nowMs - it.timeMs <= Eta.WINDOW_MS }
            .mapNotNull { it.speedKmh }.filter { it >= Eta.MIN_LIVE_KMH }.toList()
        return if (v.isEmpty()) null else v.average()
    }

    /** Точки для охвата камерой: линия (если есть, иначе станции) и текущее местоположение. Пары (широта, долгота). */
    fun cameraPoints(
        stops: List<TrainMapStop>,
        line: List<Pair<Double, Double>>?,
        fix: GeoFix?,
    ): List<Pair<Double, Double>> = buildList {
        if (line != null && line.size >= 2) addAll(line) else stops.forEach { add(it.latitude to it.longitude) }
        fix?.let { add(it.latitude to it.longitude) }
    }

    /** Оценка времени прибытия. */
    object Eta {
        const val MIN_LIVE_KMH = 10.0
        const val WINDOW_MS = 10 * 60_000L

        /** Момент прибытия (мс, UTC) при скорости [speedKmh] на расстоянии [distanceM]; null — скорость неизвестна. */
        fun arrivalMs(nowMs: Long, distanceM: Double, speedKmh: Double?): Long? {
            if (speedKmh == null || speedKmh <= 0.0) return null
            return nowMs + (distanceM / (speedKmh / 3.6) * 1000.0).toLong()
        }
    }
}
