package com.z_company.domain.entities.route

import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.util.generateId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Train(
    var trainId: String = generateId(),
    var basicId: String = "",
    var remoteObjectId: String? = null,
    var number: String? = null,
    var distance: String? = null,
    var weight: String? = null,
    var axle: String? = null,
    var conditionalLength: String? = null,
    var isHeavyLongDistance: Boolean = false,
    var stations: MutableList<Station> = mutableListOf(),
    var servicePhase: ServicePhase? = null
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
