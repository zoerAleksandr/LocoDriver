package com.example.domain.entities

import java.util.UUID

data class Train(
    val id: String = UUID.randomUUID().toString(),
    var number: String? = null,
    var weight: Int? = null,
    var axle: Int? = null,
    var conditionalLength: Int? = null,
    var locomotive: Locomotive? = null,
    var stations: MutableList<Station> = mutableListOf()
)

data class Station(
    val id: String = UUID.randomUUID().toString(),
    var stationName: String? = null,
    var timeArrival: Long? = null,
    var timeDeparture: Long? = null
)
