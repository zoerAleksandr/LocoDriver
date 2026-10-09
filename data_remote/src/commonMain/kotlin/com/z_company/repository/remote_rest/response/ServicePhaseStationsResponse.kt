package com.z_company.repository.remote_rest.response

import kotlinx.serialization.Serializable

/**
 * Автоподбор станций плеча (`POST /v1/service_phase/stations/suggest`).
 * status: ok | ambiguous (side + candidates) | not_found (side) | no_path.
 */
@Serializable
data class ServicePhaseSuggestRequest(
    val departureStation: String,
    val arrivalStation: String,
    val country: String = "RU",
    val departureOsmId: Long? = null,
    val arrivalOsmId: Long? = null,
    /** Известные координаты крайних станций (справочник/плечо) — путь и км считаются от них. */
    val departurePoint: LatLon? = null,
    val arrivalPoint: LatLon? = null,
    /** Промежуточные точки выбранного маршрута по порядку: путь и станции — строго через них (необязательно). */
    val viaPoints: List<LatLon> = emptyList(),
)

@Serializable
data class ServicePhaseSuggestResponse(
    val status: String,
    /** departure | arrival — какая крайняя станция не найдена / неоднозначна. */
    val side: String? = null,
    val name: String? = null,
    val candidates: List<OsmStationCandidate> = emptyList(),
    val departure: OsmStationCandidate? = null,
    val arrival: OsmStationCandidate? = null,
    val lengthKm: Double? = null,
    val stations: List<OsmPathStation> = emptyList(),
    /** ambiguous + true — точного совпадения нет, кандидаты похожи по сокращению (только выбор). */
    val approximate: Boolean = false,
)

@Serializable
data class OsmStationCandidate(
    val osmId: Long,
    val name: String,
    val kind: String = "station",
    val esr: String? = null,
    val operator: String? = null,
    val region: String? = null,
    val latitude: Double,
    val longitude: Double,
    val neighbours: List<String> = emptyList(),
    /** Неточное совпадение (сокращённое название) — подставлять только после выбора пользователя. */
    val approximate: Boolean = false,
)

@Serializable
data class OsmPathStation(
    val name: String,
    val kind: String = "station",
    val esr: String? = null,
    val osmId: Long? = null,
    val latitude: Double,
    val longitude: Double,
    val kmFromStart: Double,
)

/** `POST /v1/service_phase/stations/lookup` — координаты одной станции. */
@Serializable
data class ServicePhaseLookupRequest(
    val name: String,
    val country: String = "RU",
    val near: List<LatLon> = emptyList(),
    /** Поиск по части названия: до [limit] (1..30) ранжированных станций; null — прежнее поведение сервера (до 5). */
    val limit: Int? = null,
)

@Serializable
data class LatLon(val latitude: Double, val longitude: Double)

@Serializable
data class ServicePhaseLookupResponse(val candidates: List<OsmStationCandidate> = emptyList())

/** `POST /v1/service_phase/stations/nearest` — ближайшая станция к точке (форма поезда без плеча). */
@Serializable
data class NearestStationRequest(
    val latitude: Double,
    val longitude: Double,
    val radiusM: Double = 2000.0,
    /** true - ближайшая станция ПО РЕЛЬСАМ (сервер зажимает radiusM в 100..3000); false - по прямой, как раньше. */
    val alongRail: Boolean = false,
)

@Serializable
data class NearestStationResponse(
    val station: OsmStationCandidate? = null,
    /** Только при alongRail=true (аддитивные поля; у старого сервера и в прямом режиме их нет). */
    val alongRail: Boolean = false,
    val distanceAlongM: Int? = null,
    val offsetM: Int? = null,
    /** При station=null: off_rails / not_found / no_rail_data. */
    val reason: String? = null,
)

/** `POST /v1/service_phase/route/length` — длина плеча по ж/д путям через станции по порядку. */
@Serializable
data class RouteLengthRequest(
    val points: List<LatLon>,
    /** true — сервер вернёт и [RouteLengthResponse.geometry] (линия по рельсам для карты плеча). */
    val withGeometry: Boolean = false,
    /** Сколько вариантов пути вернуть в [RouteLengthResponse.routes] (1..3; только без промежуточных точек). */
    val alternatives: Int = 1,
)

@Serializable
data class RouteLengthResponse(
    val lengthKm: Double,
    val kmAt: List<Double> = emptyList(),
    /** Линия пути по рельсам, [[lat, lon], …] — только при withGeometry. */
    val geometry: List<List<Double>> = emptyList(),
    /** Варианты пути (первый — кратчайший, он же в полях выше); пусто — вариант один. */
    val routes: List<RouteVariantDto> = emptyList(),
)

@Serializable
data class RouteVariantDto(
    val id: String,
    val lengthKm: Double,
    val geometry: List<List<Double>> = emptyList(),
    val viaStations: List<RouteViaStationDto> = emptyList(),
)

/** Станция (не о.п.) на пути варианта; точка лежит на пути. */
@Serializable
data class RouteViaStationDto(
    val name: String,
    val km: Double,
    val osmId: Long? = null,
    val esr: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)
