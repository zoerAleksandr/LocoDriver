package com.example.data_local.route.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
internal data class Passenger(
    @PrimaryKey
    var passengerId: String = UUID.randomUUID().toString(),
    @ColumnInfo(index = true)
    var routeId: String = "",
    var trainNumber: String? = null,
    var stationDeparture: String? = null,
    var stationArrival: String? = null,
    var timeArrival: Long? = null,
    var timeDeparture: Long? = null,
    var notes: String? = null
)
