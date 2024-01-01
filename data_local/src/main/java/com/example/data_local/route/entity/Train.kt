package com.example.data_local.route.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.data_local.route.type_converters.PhotosConverter
import com.example.data_local.route.type_converters.StationConverter

@Entity
@TypeConverters(StationConverter::class)
internal data class Train(
    @PrimaryKey
    var trainId: String,
    @ColumnInfo(index = true)
    var routeId: String,
    var number: String?,
    var weight: Int?,
    var axle: Int?,
    var conditionalLength: Int?,
    var stations: List<Station> = listOf()
)

@Entity
internal data class Station(
    @PrimaryKey
    var stationId: String,
    @ColumnInfo(index = true)
    var trainId: String,
    var stationName: String?,
    var timeArrival: Long?,
    var timeDeparture: Long?
)
