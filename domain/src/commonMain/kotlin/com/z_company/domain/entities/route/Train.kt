package com.z_company.domain.entities.route

import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.util.generateId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Вспомогательные данные для толкача / двойной тяги / сдвоенного поезда.
 */
@Serializable
data class TrainAssist(
    var locomotiveNumber: String? = null,   // номер локомотива
    var locomotiveSeries: String? = null,   // серия локомотива
    var driverName: String? = null,         // машинист
    var notes: String? = null               // примечание
)

@Serializable
data class Train(
    var trainId: String = generateId(),
    var basicId: String = "",
    var remoteObjectId: String? = null,
    var number: String? = null,
    var additionalNumbers: MutableList<String> = mutableListOf(),
    var distance: String? = null,
    var weight: String? = null,
    var axle: String? = null,
    var conditionalLength: String? = null,
    var isHeavyLongDistance: Boolean = false,
    var stations: MutableList<Station> = mutableListOf(),
    var servicePhase: ServicePhase? = null,
    var pusher: TrainAssist? = null,            // Толкач
    var doubleTraction: TrainAssist? = null,    // Двойная тяга
    var doubledTrain: TrainAssist? = null       // Сдвоенный поезд
)

@Serializable
data class Station(
    var stationId: String = generateId(),
    var trainId: String = "",
    @SerialName("name")
    var stationName: String? = null,
    var timeArrival: Long? = null,
    var timeDeparture: Long? = null
)
