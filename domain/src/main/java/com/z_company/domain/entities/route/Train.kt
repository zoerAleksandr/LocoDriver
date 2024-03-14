package com.z_company.domain.entities.route

import java.util.UUID

data class Train(
    var trainId: String = UUID.randomUUID().toString(),
    var basicId: String = "",
    var number: String? = null,
    var weight: String? = null,
    var axle: String? = null,
    var conditionalLength: String? = null,
    var stations: MutableList<Station> = mutableListOf()
)

data class Station(
    var stationId: String = UUID.randomUUID().toString(),
    var trainId: String = "",
    var stationName: String? = null,
    var timeArrival: Long? = null,
    var timeDeparture: Long? = null
)
