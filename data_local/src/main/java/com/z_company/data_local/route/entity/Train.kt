package com.z_company.data_local.route.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.z_company.data_local.route.type_converters.StationConverter

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = BasicData::class,
            parentColumns = ["id"],
            childColumns = ["basicId"],
            onDelete = CASCADE,
            onUpdate = CASCADE
        )
    ]
)
@TypeConverters(StationConverter::class)
internal data class Train(
    @PrimaryKey
    var trainId: String,
    @ColumnInfo(index = true)
    var basicId: String,
    var remoteObjectId: String = "",
    var number: String?,
    @ColumnInfo(defaultValue = "")
    var distance: String?,
    var weight: String?,
    var axle: String?,
    var conditionalLength: String?,
    var stations: List<Station> = listOf()
)

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Train::class,
            parentColumns = ["trainId"],
            childColumns = ["trainId"],
            onDelete = CASCADE,
            onUpdate = CASCADE
        )
    ]
)
internal data class Station(
    @PrimaryKey
    var stationId: String,
    @ColumnInfo(index = true)
    var trainId: String,
    var stationName: String?,
    var timeArrival: Long?,
    var timeDeparture: Long?
)
