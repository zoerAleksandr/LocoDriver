package com.z_company.entity_converter

import com.z_company.domain.entities.route.Passenger
import com.z_company.entity.Passenger as PassengerRemote

object PassengerConverter {
    fun toRemote(passenger: Passenger) = PassengerRemote(
        passengerId = passenger.passengerId,
        basicId = passenger.basicId,
        remoteObjectId = passenger.remoteObjectId,
        trainNumber = passenger.trainNumber,
        stationDeparture = passenger.stationDeparture,
        stationArrival = passenger.stationArrival,
        timeArrival = passenger.timeArrival,
        timeDeparture = passenger.timeDeparture,
        notes = passenger.notes
    )

    private fun fromRemote(passengerRemote: PassengerRemote) = Passenger(
        passengerId = passengerRemote.passengerId,
        basicId = passengerRemote.basicId,
        remoteObjectId = passengerRemote.remoteObjectId,
        trainNumber = passengerRemote.trainNumber,
        stationDeparture = passengerRemote.stationDeparture,
        stationArrival = passengerRemote.stationArrival,
        timeArrival = passengerRemote.timeArrival,
        timeDeparture = passengerRemote.timeDeparture,
        notes = passengerRemote.notes
    )

    fun toRemoteList(passengerList: List<Passenger>): MutableList<PassengerRemote> {
        return passengerList.map {
            toRemote(it)
        }.toMutableList()
    }

    fun fromRemoteList(remotePassengerList: List<PassengerRemote>): MutableList<Passenger> {
        return remotePassengerList.map {
            fromRemote(it)
        }.toMutableList()
    }
}