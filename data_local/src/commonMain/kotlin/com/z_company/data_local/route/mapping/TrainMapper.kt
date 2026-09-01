package com.z_company.data_local.route.mapping

import com.zcompany.datalocal.route.db.Train as TrainRow
import com.z_company.domain.entities.route.CarInspector
import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.TrainAssist
import com.z_company.domain.entities.route.TrainDataVersion
import com.z_company.domain.entities.setting.ServicePhase
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

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
    val orderIndex: Int = 0,
    // Номер пути — новое поле. Отсутствующее значение в JSON → null (безопасная миграция).
    @SerialName("track_number") val trackNumber: String? = null,
    // Конечная / проходная станция, поля перегона — новые поля, безопасная миграция (default = "не задано").
    val isFinalStation: Boolean = false,
    val isPassingStation: Boolean = false,
    val segmentTrackNumber: String? = null,
    val segmentNotes: String? = null
) {
    fun toStation(): Station {
        val rawName = stationNameGson ?: stationNameNew
        // Сервер (Python) конвертирует null → "None" — убираем
        val cleanName = if (rawName == "None") null else rawName
        val cleanTrack = if (trackNumber == "None") null else trackNumber
        return Station(
            stationId = stationId,
            trainId = trainId,
            stationName = cleanName,
            timeArrival = timeArrival,
            timeDeparture = timeDeparture,
            orderIndex = orderIndex,
            trackNumber = cleanTrack,
            isFinalStation = isFinalStation,
            isPassingStation = isPassingStation,
            segmentTrackNumber = segmentTrackNumber,
            segmentNotes = segmentNotes
        )
    }
}

internal object TrainMapper {

    @Serializable
    private data class StationsStorage(
        val stations: List<Station> = emptyList(),
        val trainDataVersions: List<TrainDataVersion> = emptyList()
    )

    fun encodeStations(stations: List<Station>, trainDataVersions: List<TrainDataVersion>): String {
        val indexed = stations.mapIndexed { index, station ->
            station.copy(orderIndex = index)
        }
        return json.encodeToString(StationsStorage(indexed, trainDataVersions))
    }

    fun encodeServicePhase(phase: ServicePhase?): String? =
        phase?.let { json.encodeToString(it) }

    fun encodeTrainAssist(assist: TrainAssist?): String? =
        assist?.let { json.encodeToString(it) }

    fun encodeCarInspector(carInspector: CarInspector?): String? =
        carInspector?.let { json.encodeToString(it) }

    fun encodeAdditionalNumbers(numbers: List<String>): String? =
        if (numbers.isEmpty()) null else json.encodeToString(numbers)

    private fun decodeStations(value: String): MutableList<Station> =
        runCatching {
            val stationJson = if (value.trimStart().startsWith("{")) {
                json.parseToJsonElement(value).jsonObject["stations"]?.toString() ?: "[]"
            } else value
            json.decodeFromString<List<StationRow>>(stationJson)
                .map { it.toStation() }
                .sortedBy { it.orderIndex }
                .toMutableList()
        }.getOrElse { e ->
            println("TrainMapper: Failed to decode stations: ${e.message}, raw=$value")
            mutableListOf()
        }

    private fun decodeTrainDataVersions(value: String): List<TrainDataVersion> =
        if (!value.trimStart().startsWith("{")) emptyList() else runCatching {
            json.decodeFromString<StationsStorage>(value).trainDataVersions
        }.getOrElse { emptyList() }

    private fun decodeServicePhase(value: String?): ServicePhase? =
        value?.let {
            runCatching { json.decodeFromString<ServicePhase>(it) }.getOrNull()
        }

    private fun decodeTrainAssist(value: String?): TrainAssist? =
        value?.let {
            runCatching { json.decodeFromString<TrainAssist>(it) }.getOrNull()
        }

    private fun decodeCarInspector(value: String?): CarInspector? =
        value?.let {
            runCatching { json.decodeFromString<CarInspector>(it) }.getOrNull()
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
        stations = decodeStations(row.stations),
        servicePhase = decodeServicePhase(row.servicePhase),
        pusher = decodeTrainAssist(row.pusher),
        doubleTraction = decodeTrainAssist(row.doubleTraction),
        doubledTrain = decodeTrainAssist(row.doubledTrain),
        dataVersions = decodeTrainDataVersions(row.stations),
        carInspector = decodeCarInspector(row.carInspector)
    )
}
