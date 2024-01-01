package com.example.data_local.route.entity_converters

import com.example.domain.entities.route.Passenger
import com.example.data_local.route.entity.Passenger as PassengerEntity

internal object PassengerConverter {
    private fun fromData(passenger: Passenger) = PassengerEntity(
        passenger.passengerid,
        passenger.routeId,
        passenger.trainNumber,
        passenger.stationDeparture,
        passenger.stationArrival,
        passenger.timeDeparture,
        passenger.timeArrival
    )

    private fun toData(entity: PassengerEntity) = Passenger().apply {
        passengerid = entity.passengerId
        routeId = entity.routeId
        trainNumber = entity.trainNumber
        stationDeparture = entity.stationDeparture
        stationArrival = entity.stationArrival
        timeDeparture = entity.timeDeparture
        timeArrival = entity.timeArrival
    }

    fun fromDataList(list: List<Passenger>): MutableList<PassengerEntity> {
        return list.map {
            fromData(it)
        }.toMutableList()
    }

    fun toDataList(entityList: List<PassengerEntity>): MutableList<Passenger> {
        return entityList.map {
            toData(it)
        }.toMutableList()
    }
}