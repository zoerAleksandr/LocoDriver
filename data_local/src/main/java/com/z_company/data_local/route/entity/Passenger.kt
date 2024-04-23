package com.z_company.data_local.route.entity

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
            childColumns = ["basicId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
internal data class Passenger(
    @PrimaryKey
    var passengerId: String = UUID.randomUUID().toString(),
    @ColumnInfo(index = true)
    var basicId: String = "",
    var remoteObjectId: String? = null,
    var trainNumber: String? = null,
    var stationDeparture: String? = null,
    var stationArrival: String? = null,
    var timeArrival: Long? = null,
    var timeDeparture: Long? = null,
    var notes: String? = null
)
