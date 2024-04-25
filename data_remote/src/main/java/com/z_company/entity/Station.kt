package com.z_company.entity

data class Station(
    var stationId: String,
    var trainId: String,
    var stationName: String?,
    var timeArrival: Long?,
    var timeDeparture: Long?
)
