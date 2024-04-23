package com.z_company.entity

data class Passenger(
    var passengerId: String,
    var basicId: String,
    var remoteObjectId: String? = null,
    var trainNumber: String? = null,
    var stationDeparture: String? = null,
    var stationArrival: String? = null,
    var timeArrival: Long? = null,
    var timeDeparture: Long? = null,
    var notes: String? = null
)