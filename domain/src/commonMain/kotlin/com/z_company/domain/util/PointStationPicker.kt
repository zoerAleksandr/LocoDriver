package com.z_company.domain.util

/** Откуда станция, найденная по точке на карте. */
enum class PointStationSource { DIRECTORY, OSM }

/**
 * Станция, найденная по точке, на которую нажали на карте (редактор плеча, SCREEN_SPECS §6.6).
 * [distanceM] - расстояние от нажатой точки до станции по прямой, м.
 */
data class PointStation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val distanceM: Int,
    val source: PointStationSource,
    val osmId: Long? = null,
    val esr: String? = null,
)

/** Станция OSM, нарисованная на карте под пальцем (слой станций подложки). */
data class RenderedStation(val name: String, val latitude: Double, val longitude: Double)

/**
 * Итог поиска по точке. [station] null - рядом (в [PointStationPicker.RADIUS_M]) станций нет.
 * [offline] - сервер не ответил (нет сети/ошибка/503): искали только среди станций справочника и на карте.
 */
data class PointStationResult(val station: PointStation?, val offline: Boolean)

/**
 * Выбор БЛИЖАЙШЕЙ станции по точке на карте. Порядок: станции справочника и сохранённые станции пользователя
 * с координатами (в радиусе [RADIUS_M], по рельсам - через сервер, без сети - по прямой), затем станция OSM
 * (сервер или нарисованная на карте). Станция справочника побеждает станцию OSM с тем же названием: её координаты
 * не перезаписываются (SCREEN_SPECS §17.4).
 */
object PointStationPicker {
    /** Радиус поиска станции вокруг нажатой точки. */
    const val RADIUS_M = 1_000.0

    private fun key(name: String) = ServicePhaseStationsMerge.normalizeName(name)

    private fun dist(lat: Double, lon: Double, p: Pair<Double, Double>) =
        ServicePhaseStationPicker.distanceM(lat, lon, p.first, p.second)

    /** Ближайшая станция справочника по прямой в [radiusM]; null - в радиусе нет. */
    fun nearestDirectory(
        latitude: Double,
        longitude: Double,
        points: Map<String, Pair<Double, Double>>,
        names: Map<String, String>,
        radiusM: Double = RADIUS_M,
    ): PointStation? = points.asSequence()
        .mapNotNull { (k, p) ->
            val name = names[k]?.trim()?.takeIf(String::isNotEmpty) ?: return@mapNotNull null
            val d = dist(latitude, longitude, p)
            if (d > radiusM) null else Triple(name, p, d)
        }
        .minByOrNull { it.third }
        ?.let { (name, p, d) ->
            PointStation(name, p.first, p.second, d.toInt(), PointStationSource.DIRECTORY)
        }

    /** Ответ сервера (по рельсам) -> станция; кандидат-справочник и одноимённая запись справочника сохраняют свои название и координаты. */
    fun fromRail(
        found: RailStation,
        latitude: Double,
        longitude: Double,
        points: Map<String, Pair<Double, Double>>,
        names: Map<String, String>,
    ): PointStation {
        val byKey = found.candidateKey
        if (byKey != null) {
            val p = points[byKey] ?: (found.latitude to found.longitude)
            val name = names[byKey]?.trim()?.takeIf(String::isNotEmpty) ?: found.name
            return PointStation(name, p.first, p.second, dist(latitude, longitude, p).toInt(), PointStationSource.DIRECTORY, found.osmId, found.esr)
        }
        return reconcile(found.name, found.latitude, found.longitude, found.osmId, found.esr, latitude, longitude, points, names)
    }

    /** Станция OSM, нарисованная на карте -> станция (с учётом справочника). */
    fun fromRendered(
        station: RenderedStation,
        latitude: Double,
        longitude: Double,
        points: Map<String, Pair<Double, Double>>,
        names: Map<String, String>,
    ): PointStation = reconcile(station.name, station.latitude, station.longitude, null, null, latitude, longitude, points, names)

    private fun reconcile(
        name: String,
        stLat: Double,
        stLon: Double,
        osmId: Long?,
        esr: String?,
        latitude: Double,
        longitude: Double,
        points: Map<String, Pair<Double, Double>>,
        names: Map<String, String>,
    ): PointStation {
        val k = key(name)
        val known = points[k]
        if (known != null) {
            val shown = names[k]?.trim()?.takeIf(String::isNotEmpty) ?: name.trim()
            return PointStation(shown, known.first, known.second, dist(latitude, longitude, known).toInt(), PointStationSource.DIRECTORY, osmId, esr)
        }
        val d = ServicePhaseStationPicker.distanceM(latitude, longitude, stLat, stLon).toInt()
        return PointStation(name.trim(), stLat, stLon, d, PointStationSource.OSM, osmId, esr)
    }
}
