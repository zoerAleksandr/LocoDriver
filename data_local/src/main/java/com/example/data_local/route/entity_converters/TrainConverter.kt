package com.example.data_local.route.entity_converters

import com.example.domain.entities.route.Train
import com.example.data_local.route.entity.Train as TrainEntity

internal object TrainConverter {
    fun fromData(train: Train) = TrainEntity(
        train.trainId,
        train.baseId,
        train.number,
        train.weight,
        train.axle,
        train.conditionalLength,
        StationConverter.fromDataList(train.stations)
    )

    private fun toData(entity: TrainEntity) = Train(
        trainId = entity.trainId,
        baseId = entity.baseId,
        number = entity.number,
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