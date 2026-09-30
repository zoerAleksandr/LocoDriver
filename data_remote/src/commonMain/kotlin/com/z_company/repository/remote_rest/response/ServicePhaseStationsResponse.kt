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
)

@Serializable
data class LatLon(val latitude: Double, val longitude: Double)

@Serializable
data class ServicePhaseLookupResponse(val candidates: List<OsmStationCandidate> = emptyList())

/** `POST /v1/service_phase/stations/nearest` — ближайшая станция к точке (форма поезда без плеча). */
@Serializable
data class NearestStationRequest(val latitude: Double, val longitude: Double, val radiusM: Double = 2000.0)

@Serializable
data class NearestStationResponse(val station: OsmStationCandidate? = null)

/** `POST /v1/service_phase/route/length` — длина плеча по ж/д путям через станции по порядку. */
@Serializable
data class RouteLengthRequest(val points: List<LatLon>)

@Serializable
data class RouteLengthResponse(val lengthKm: Double, val kmAt: List<Double> = emptyList())
