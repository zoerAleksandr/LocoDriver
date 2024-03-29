package com.example.data_local.route.entity_converters

import com.example.domain.entities.route.Station
import com.example.data_local.route.entity.Station as StationEntity

internal object StationConverter {
    private fun fromData(station: Station) = StationEntity(
       stationId = station.stationId,
       trainId = station.trainId,
       stationName = station.stationName,
       timeDeparture = station.timeDeparture,
       timeArrival = station.timeArrival
    )

    private fun toData(entity: StationEntity) = Station(
        stationId = entity.stationId,
        trainId = entity.trainId,
        stationName = entity.stationName,
        timeDeparture = entity.timeDeparture,
        timeArrival = entity.timeArrival
    )

    fun fromDataList(list: List<Station>): MutableList<StationEntity> {
        return list.map {
            fromData(it)
        }.toMutableList()
    }

    fun toDataList(entityList: List<StationEntity>): MutableList<Station> {
        return entityList.map {
            toData(it)
        }.toMutableList()
    }
}