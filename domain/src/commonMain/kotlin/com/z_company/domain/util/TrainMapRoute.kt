package com.z_company.domain.util

import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.entities.setting.ServicePhaseStationKind
import kotlin.math.PI
import kotlin.math.cos

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

    /**
     * Индекс ближайшей станции впереди по ходу для [fix] или null, если местоположение неизвестно.
     * Берётся ближайший к [fix] отрезок ломаной по станциям; впереди — его конец. Если телефон до начала
     * маршрута (проекция на первый отрезок не дальше его начала) — впереди первая станция.
     */
    fun nextAheadIndex(stops: List<TrainMapStop>, fix: GeoFix?): Int? {
        if (fix == null || stops.isEmpty()) return null
        if (stops.size == 1) return 0
        val kx = cos(fix.latitude * PI / 180.0)
        var best = -1
        var bestDist = Double.MAX_VALUE
        var bestT = 0.0
        for (i in 0 until stops.size - 1) {
            val ax = (stops[i].longitude - fix.longitude) * kx
            val ay = stops[i].latitude - fix.latitude
            val bx = (stops[i + 1].longitude - fix.longitude) * kx
            val by = stops[i + 1].latitude - fix.latitude
            val dx = bx - ax
            val dy = by - ay
            val len2 = dx * dx + dy * dy
            val t = if (len2 == 0.0) 0.0 else ((-ax * dx - ay * dy) / len2)
            val tc = t.coerceIn(0.0, 1.0)
            val px = ax + dx * tc
            val py = ay + dy * tc
            val d = px * px + py * py
            if (d < bestDist) {
                bestDist = d
                best = i
                bestT = t
            }
        }
        return if (best == 0 && bestT <= 0.0) 0 else best + 1
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
