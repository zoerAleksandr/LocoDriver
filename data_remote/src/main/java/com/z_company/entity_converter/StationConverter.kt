package com.z_company.entity_converter

import com.z_company.domain.entities.route.Station
import com.z_company.entity.Station as StationRemote

object StationConverter {
    private fun toRemote(station: Station) = StationRemote(
        stationId = station.stationId,
        trainId = station.trainId,
        stationName = station.stationName,
        timeArrival = station.timeArrival,
        timeDeparture = station.timeDeparture
    )

    private fun fromRemote(stationRemote: StationRemote) = Station(
        stationId = stationRemote.stationId,
        trainId = stationRemote.trainId,
        stationName = stationRemote.stationName,
        timeArrival = stationRemote.timeArrival,
        timeDeparture = stationRemote.timeDeparture
    )

    fun toRemoteList(stationList: List<Station>): MutableList<StationRemote> {
        return stationList.map {
            toRemote(it)
        }.toMutableList()
    }

    fun fromRemoteList(remoteStationList: List<StationRemote>): MutableList<Station> {
        return remoteStationList.map {
            fromRemote(it)
        }.toMutableList()
    }
}