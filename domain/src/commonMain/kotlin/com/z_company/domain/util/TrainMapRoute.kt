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

    /** Накопленная длина линии (м) к каждой вершине. */
    fun cumulativeM(line: List<Pair<Double, Double>>): DoubleArray {
        val cum = DoubleArray(line.size)
        for (i in 1 until line.size) {
            cum[i] = cum[i - 1] + TrackAnalysis.distanceM(line[i - 1].first, line[i - 1].second, line[i].first, line[i].second)
        }
        return cum
    }

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
        for (i in fromSegment.coerceAtLeast(0) until line.size - 1) {
            val (aLat, aLon) = line[i]
            val (bLat, bLon) = line[i + 1]
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

    /**
     * Точки пути для отрисовки, привязанные к рельсовой линии: точка с отклонением от линии ≤ [toleranceM] (≈30 м,
     * сглаживает шум GPS) заменяется своей проекцией на линию, остальные остаются сырыми. Время и скорость сохраняются
     * (в тапе по пути берутся из исходных точек, а не из этих).
     */
    fun snapToLine(
        points: List<TrackPoint>,
        line: List<Pair<Double, Double>>?,
        toleranceM: Double = 30.0,
    ): List<TrackPoint> {
        if (line == null || line.size < 2) return points
        val cum = cumulativeM(line)
        return points.map { p ->
            val pr = project(line, cum, p.latitude, p.longitude)
            if (pr != null && pr.offsetM <= toleranceM) p.copy(latitude = pr.latitude, longitude = pr.longitude) else p
        }
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
}
