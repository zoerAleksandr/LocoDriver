package com.example.data_local.route.entity_converters

import com.example.domain.entities.route.Locomotive
import com.example.data_local.route.entity.Locomotive as LocomotiveEntity

internal object LocomotiveConverter {
    fun fromData(locomotive: Locomotive) = LocomotiveEntity(
        locoId = locomotive.locoId,
        baseId = locomotive.basicId,
        series = locomotive.series,
        number = locomotive.number,
        type = locomotive.type,
        electricSectionList = ElectricSectionConverter.fromDataList(locomotive.electricSectionList),
        dieselSectionList = DieselSectionConverter.fromDataList(locomotive.dieselSectionList),
        timeStartOfAcceptance = locomotive.timeStartOfAcceptance,
        timeEndOfAcceptance = locomotive.timeEndOfAcceptance,
        timeStartOfDelivery = locomotive.timeStartOfDelivery,
        timeEndOfDelivery = locomotive.timeEndOfDelivery
    )


    fun toData(entity: LocomotiveEntity) = Locomotive().apply {
        locoId = entity.locoId
        basicId = entity.baseId
        series = entity.series
        number = entity.number
        type = entity.type
        electricSectionList = ElectricSectionConverter.toDataList(entity.electricSectionList)
        dieselSectionList = DieselSectionConverter.toDataList(entity.dieselSectionList)
        timeStartOfAcceptance = entity.timeStartOfAcceptance
        timeEndOfAcceptance = entity.timeEndOfAcceptance
        timeStartOfDelivery = entity.timeStartOfDelivery
        timeEndOfDelivery = entity.timeEndOfDelivery
    }

    fun fromDataList(list: List<Locomotive>): MutableList<LocomotiveEntity> {
        return list.map {
            fromData(it)
        }.toMutableList()
    }

    fun toDataList(entityList: List<LocomotiveEntity>): MutableList<Locomotive> {
        return entityList.map { section ->
            toData(section)
        }.toMutableList()
    }
}