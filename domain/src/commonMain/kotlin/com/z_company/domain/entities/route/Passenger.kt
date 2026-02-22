package com.z_company.domain.entities.route

import com.z_company.domain.util.generateId
import kotlinx.serialization.Serializable

@Serializable
data class Passenger(
    var passengerId: String = generateId(),
    var basicId: String = "",
    var remoteObjectId: String? = null,
    var trainNumber: String? = null,
    var stationDeparture: String? = null,
    var stationArrival: String? = null,
    var timeArrival: Long? = null,
    var timeDeparture: Long? = null,
    var notes: String? = null
)
