package com.example.data_local.route.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = BasicData::class,
            parentColumns = ["id"],
            childColumns = ["baseId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
internal data class Passenger(
    @PrimaryKey
    var passengerId: String = UUID.randomUUID().toString(),
    @ColumnInfo(index = true)
    var baseId: String = "",
    var trainNumber: String? = null,
    var stationDeparture: String? = null,
    var stationArrival: String? = null,
    var timeArrival: Long? = null,
    var timeDeparture: Long? = null,
    var notes: String? = null
)
