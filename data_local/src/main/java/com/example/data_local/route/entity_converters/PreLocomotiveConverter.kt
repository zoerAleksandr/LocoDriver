package com.example.data_local.route.entity_converters

import com.example.domain.entities.route.Locomotive
import com.example.domain.entities.route.pre_save.PreLocomotive
import com.example.data_local.route.entity.pre_save.PreLocomotive as PreLocomotiveEntity

internal object PreLocomotiveConverter {
    fun fromPreSave(preLocomotive: PreLocomotive, basicId: String) = Locomotive(
        locoId = preLocomotive.locoId,
        basicId = basicId,
        series = preLocomotive.series,
        number = preLocomotive.number,
        type = preLocomotive.type,
        electricSectionList = preLocomotive.electricSectionList,
        dieselSectionList = preLocomotive.dieselSectionList,
        timeStartOfAcceptance = preLocomotive.timeStartOfAcceptance,
        timeEndOfAcceptance = preLocomotive.timeEndOfAcceptance,
        timeStartOfDelivery = preLocomotive.timeStartOfDelivery,
        timeEndOfDelivery = preLocomotive.timeEndOfDelivery
    )

    fun toPreSave(locomotive: Locomotive) = PreLocomotive(
        locoId = locomotive.locoId,
        series = locomotive.series,
        number = locomotive.number,
        type = locomotive.type,
        electricSectionList = locomotive.electricSectionList,
        dieselSectionList = locomotive.dieselSectionList,
        timeStartOfAcceptance = locomotive.timeStartOfAcceptance,
        timeEndOfAcceptance = locomotive.timeEndOfAcceptance,
        timeStartOfDelivery = locomotive.timeStartOfDelivery,
        timeEndOfDelivery = locomotive.timeEndOfDelivery
    )

    fun fromPreSaveList(list: List<PreLocomotive>, basicId: String): MutableList<Locomotive> {
        return list.map {
            fromPreSave(it, basicId)
        }.toMutableList()
    }

    fun fromData(preLocomotive: PreLocomotive) = PreLocomotiveEntity(
        locoId = preLocomotive.locoId,
        series = preLocomotive.series,
        number = preLocomotive.number,
        type = preLocomotive.type,
        electricSectionList = ElectricSectionConverter.fromDataList(preLocomotive.electricSectionList),
        dieselSectionList = DieselSectionConverter.fromDataList(preLocomotive.dieselSectionList),
        timeStartOfAcceptance = preLocomotive.timeStartOfAcceptance,
        timeEndOfAcceptance = preLocomotive.timeEndOfAcceptance,
        timeStartOfDelivery = preLocomotive.timeStartOfDelivery,
        timeEndOfDelivery = preLocomotive.timeEndOfDelivery
    )


    fun toData(entity: PreLocomotiveEntity) = PreLocomotive(
        locoId = entity.locoId,
        series = entity.series,
        number = entity.number,
        type = entity.type,
        electricSectionList = ElectricSectionConverter.toDataList(entity.electricSectionList),
        dieselSectionList = DieselSectionConverter.toDataList(entity.dieselSectionList),
        timeStartOfAcceptance = entity.timeStartOfAcceptance,
        timeEndOfAcceptance = entity.timeEndOfAcceptance,
        timeStartOfDelivery = entity.timeStartOfDelivery,
        timeEndOfDelivery = entity.timeEndOfDelivery,
    )

    fun fromDataList(list: List<PreLocomotive>): MutableList<PreLocomotiveEntity> {
        return list.map {
            fromData(it)
        }.toMutableList()
    }

    fun toDataList(entityList: List<PreLocomotiveEntity>): MutableList<PreLocomotive> {
        return entityList.map { section ->
            toData(section)
        }.toMutableList()
    }
}