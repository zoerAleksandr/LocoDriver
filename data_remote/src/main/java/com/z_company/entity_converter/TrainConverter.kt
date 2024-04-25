package com.z_company.entity_converter

import com.z_company.domain.entities.route.Train
import com.z_company.entity.Train as TrainRemote

object TrainConverter {
    fun toRemote(train: Train) = TrainRemote(
        trainId = train.trainId,
        basicId = train.basicId,
        remoteObjectId = train.remoteObjectId ?: "",
        number = train.number,
        weight = train.weight,
        axle = train.axle,
        conditionalLength = train.conditionalLength,
        stations = StationConverter.toRemoteList(train.stations)
    )

    fun fromRemote(trainRemote: TrainRemote) = Train(
        trainId = trainRemote.trainId,
        basicId = trainRemote.basicId,
        remoteObjectId = trainRemote.remoteObjectId,
        number = trainRemote.number,
        weight = trainRemote.weight,
        axle = trainRemote.axle,
        conditionalLength = trainRemote.conditionalLength,
        stations = StationConverter.fromRemoteList(trainRemote.stations)
    )

    fun fromRemoteList(trains: List<TrainRemote>): MutableList<Train> {
        return trains.map { train ->
            fromRemote(train)
        }.toMutableList()
    }
}