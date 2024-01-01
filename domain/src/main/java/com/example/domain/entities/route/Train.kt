package com.example.domain.entities.route

import java.util.UUID

data class Train(
    var trainId: String = UUID.randomUUID().toString(),
    var routeId: String = "",
    var number: String? = null,
    var weight: Int? = null,
    var axle: Int? = null,
    var conditionalLength: Int? = null,
    var stations: MutableList<Station> = mutableListOf()
)

data class Station(
    var stationId: String = UUID.randomUUID().toString(),
    var trainId: String = "",
    var stationName: String? = null,
    var timeArrival: Long? = null,
    var timeDeparture: Long? = null
)
