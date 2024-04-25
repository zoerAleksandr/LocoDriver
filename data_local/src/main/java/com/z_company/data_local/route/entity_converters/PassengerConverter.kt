package com.z_company.data_local.route.entity_converters

import com.z_company.domain.entities.route.Passenger
import com.z_company.data_local.route.entity.Passenger as PassengerEntity

internal object PassengerConverter {
    fun fromData(passenger: Passenger) = PassengerEntity(
        passengerId = passenger.passengerId,
        basicId = passenger.basicId,
        remoteObjectId = passenger.remoteObjectId,
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
        remoteObjectId = entity.remoteObjectId,
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