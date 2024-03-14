package com.example.data_local.route.entity_converters

import com.example.domain.entities.route.Passenger
import com.example.data_local.route.entity.Passenger as PassengerEntity

internal object PassengerConverter {
    fun fromData(passenger: Passenger) = PassengerEntity(
        passengerId = passenger.passengerId,
        basicId = passenger.basicId,
        trainNumber = passenger.trainNumber,
        stationDeparture = passenger.stationDeparture,
        stationArrival = passenger.stationArrival,
        timeDeparture = passenger.timeDeparture,
        timeArrival = passenger.timeArrival,
        notes = passenger.notes
    )

    fun toData(entity: PassengerEntity) = Passenger(
        passengerId = entity.passengerId,
        basicId = entity.basicId,
        trainNumber = entity.trainNumber,
        stationDeparture = entity.stationDeparture,
        stationArrival = entity.stationArrival,
        timeDeparture = entity.timeDeparture,
        timeArrival = entity.timeArrival,
        notes = entity.notes
    )

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