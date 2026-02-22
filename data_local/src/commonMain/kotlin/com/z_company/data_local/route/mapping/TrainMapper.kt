package com.z_company.data_local.route.mapping

import com.z_company.data_local.route.db.Train as TrainRow
import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.setting.ServicePhase
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

/**
 * Compat row for deserializing stations stored by old GSON code
 * (field "stationName") or by new kotlinx.serialization (field "name").
 */
@Serializable
private data class StationRow(
    val stationId: String,
    val trainId: String,
    @SerialName("stationName") val stationNameGson: String? = null,
    @SerialName("name") val stationNameNew: String? = null,
    val timeArrival: Long? = null,
    val timeDeparture: Long? = null
) {
    fun toStation(): Station = Station(
        stationId = stationId,
        trainId = trainId,
        stationName = stationNameGson ?: stationNameNew,
        timeArrival = timeArrival,
        timeDeparture = timeDeparture
    )
}

internal object TrainMapper {

    fun encodeStations(stations: List<Station>): String =
        json.encodeToString(stations)

    fun encodeServicePhase(phase: ServicePhase?): String? =
        phase?.let { json.encodeToString(it) }

    private fun decodeStations(value: String): MutableList<Station> =
        runCatching {
            json.decodeFromString<List<StationRow>>(value).map { it.toStation() }.toMutableList()
        }.getOrElse { mutableListOf() }

    private fun decodeServicePhase(value: String?): ServicePhase? =
        value?.let {
            runCatching { json.decodeFromString<ServicePhase>(it) }.getOrNull()
        }

    fun toData(row: TrainRow): Train = Train(
        trainId = row.trainId,
        basicId = row.basicId,
        remoteObjectId = row.remoteObjectId,
        number = row.number,
        distance = row.distance,
        weight = row.weight,
        axle = row.axle,
        conditionalLength = row.conditionalLength,
        isHeavyLongDistance = row.isHeavyLongDistance != 0L,
        stations = decodeStations(row.stations),
        servicePhase = decodeServicePhase(row.servicePhase)
    )
}
