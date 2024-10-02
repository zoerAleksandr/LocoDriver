package com.z_company.data_local.route.entity_converters

import com.z_company.domain.entities.route.Train
import com.z_company.data_local.route.entity.Train as TrainEntity

internal object TrainConverter {
    fun fromData(train: Train) = TrainEntity(
        trainId = train.trainId,
        basicId = train.basicId,
        remoteObjectId = train.remoteObjectId ?: "",
        number = train.number,
        distance = train.distance,
        weight = train.weight,
        axle = train.axle,
        conditionalLength = train.conditionalLength,
        stations = StationConverter.fromDataList(train.stations)
    )

    fun toData(entity: TrainEntity) = Train(
        trainId = entity.trainId,
        basicId = entity.basicId,
        remoteObjectId = entity.remoteObjectId,
        number = entity.number,
        distance = entity.distance,
        weight = entity.weight,
        axle = entity.axle,
        conditionalLength = entity.conditionalLength,
        stations = StationConverter.toDataList(entity.stations),
    )

    fun fromDataList(list: List<Train>): MutableList<TrainEntity> {
        return list.map {
            fromData(it)
        }.toMutableList()
    }

    fun toDataList(entityList: List<TrainEntity>): MutableList<Train> {
        return entityList.map { section ->
            toData(section)
        }.toMutableList()
    }
}