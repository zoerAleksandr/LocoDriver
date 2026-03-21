package com.z_company.data_local.route.mapping

import com.zcompany.datalocal.route.db.Train as TrainRow
import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.TrainAssist
import com.z_company.domain.entities.setting.ServicePhase
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

/**
 * Compat row for deserializing stations stored by old GSON code
 * (field "stationName") or by new kotlinx.serialization (field "name").
 */
@Serializable
private data class StationRow(
    val stationId: String = "",
    val trainId: String = "",
    @SerialName("stationName") val stationNameGson: String? = null,
    @SerialName("name") val stationNameNew: String? = null,
    val timeArrival: Long? = null,
    val timeDeparture: Long? = null,
    val orderIndex: Int = 0
) {
    fun toStation(): Station {
        val rawName = stationNameGson ?: stationNameNew
        // Сервер (Python) конвертирует null → "None" — убираем
        val cleanName = if (rawName == "None") null else rawName
        return Station(
            stationId = stationId,
            trainId = trainId,
            stationName = cleanName,
            timeArrival = timeArrival,
            timeDeparture = timeDeparture,
            orderIndex = orderIndex
        )
    }
}

internal object TrainMapper {

    fun encodeStations(stations: List<Station>): String {
        val indexed = stations.mapIndexed { index, station ->
            station.copy(orderIndex = index)
        }
        return json.encodeToString(indexed)
    }

    fun encodeServicePhase(phase: ServicePhase?): String? =
        phase?.let { json.encodeToString(it) }

    fun encodeTrainAssist(assist: TrainAssist?): String? =
        assist?.let { json.encodeToString(it) }

    fun encodeAdditionalNumbers(numbers: List<String>): String? =
        if (numbers.isEmpty()) null else json.encodeToString(numbers)

    private fun decodeStations(value: String): MutableList<Station> =
        runCatching {
            json.decodeFromString<List<StationRow>>(value)
                .map { it.toStation() }
                .sortedBy { it.orderIndex }
                .toMutableList()
        }.getOrElse { e ->
            println("TrainMapper: Failed to decode stations: ${e.message}, raw=$value")
            mutableListOf()
        }

    private fun decodeServicePhase(value: String?): ServicePhase? =
        value?.let {
            runCatching { json.decodeFromString<ServicePhase>(it) }.getOrNull()
        }

    private fun decodeTrainAssist(value: String?): TrainAssist? =
        value?.let {
            runCatching { json.decodeFromString<TrainAssist>(it) }.getOrNull()
        }

    private fun decodeAdditionalNumbers(value: String?): MutableList<String> =
        value?.let {
            runCatching { json.decodeFromString<List<String>>(it) }.getOrElse { mutableListOf() }
        }?.toMutableList() ?: mutableListOf()

    fun toData(row: TrainRow): Train = Train(
        trainId = row.trainId,
        basicId = row.basicId,
        number = row.number,
        additionalNumbers = decodeAdditionalNumbers(row.additionalNumbers),
        distance = row.distance,
        weight = row.weight,
        axle = row.axle,
        conditionalLength = row.conditionalLength,
        isHeavyLongDistance = row.isHeavyLongDistance != 0L,
        stations = decodeStations(row.stations),
        servicePhase = decodeServicePhase(row.servicePhase),
        pusher = decodeTrainAssist(row.pusher),
        doubleTraction = decodeTrainAssist(row.doubleTraction),
        doubledTrain = decodeTrainAssist(row.doubledTrain)
    )
}
