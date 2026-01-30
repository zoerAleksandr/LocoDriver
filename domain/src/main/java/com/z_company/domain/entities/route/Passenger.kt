package com.z_company.domain.entities.route

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Passenger(
    var passengerId: String = UUID.randomUUID().toString(),
    var basicId: String = "",
    var remoteObjectId: String? = null,
    var trainNumber: String? = null,
    var stationDeparture: String? = null,
    var stationArrival: String? = null,
    var timeArrival: Long? = null,
    var timeDeparture: Long? = null,
    var notes: String? = null
)
