package com.example.domain.entities.route

import java.util.UUID

data class Passenger(
    var passengerid: String = UUID.randomUUID().toString(),
    var baseId: String = "",
    var trainNumber: String? = null,
    var stationDeparture: String? = null,
    var stationArrival: String? = null,
    var timeArrival: Long? = null,
    var timeDeparture: Long? = null,
    var notes: String? = null
)
