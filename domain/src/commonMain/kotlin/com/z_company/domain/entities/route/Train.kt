package com.z_company.domain.entities.route

import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.util.generateId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrainDataVersion(
    val stationId: String? = null,
    val stationName: String? = null,
    val weight: String? = null,
    val axle: String? = null,
    val conditionalLength: String? = null,
    val changedAt: Long = 0L
)

/**
 * Вспомогательные данные для толкача / двойной тяги / сдвоенного поезда.
 */
@Serializable
data class TrainAssist(
    var locomotiveNumber: String? = null,   // номер локомотива
    var locomotiveSeries: String? = null,   // серия локомотива
    var driverName: String? = null,         // машинист
    var notes: String? = null,              // примечание
    var isFirst: Boolean? = null            // null = не задано, true = "Я первый", false = "Я второй"
)

@Serializable
data class Train(
    var trainId: String = generateId(),
    var basicId: String = "",
    var number: String? = null,
    var additionalNumbers: MutableList<String> = mutableListOf(),
    var distance: String? = null,
    var weight: String? = null,
    var axle: String? = null,
    var conditionalLength: String? = null,
    var stations: MutableList<Station> = mutableListOf(),
    var servicePhase: ServicePhase? = null,
    var pusher: TrainAssist? = null,            // Толкач
    var doubleTraction: TrainAssist? = null,    // Двойная тяга
    var doubledTrain: TrainAssist? = null,      // Сдвоенный поезд
    /** История изменения состава; опциональное поле обратносуместимого API. */
    var dataVersions: List<TrainDataVersion> = emptyList()
)

@Serializable
data class Station(
    var stationId: String = generateId(),
    var trainId: String = "",
    @SerialName("name")
    var stationName: String? = null,
    var timeArrival: Long? = null,
    var timeDeparture: Long? = null,
    var orderIndex: Int = 0,
    @SerialName("track_number")
    var trackNumber: String? = null
)
