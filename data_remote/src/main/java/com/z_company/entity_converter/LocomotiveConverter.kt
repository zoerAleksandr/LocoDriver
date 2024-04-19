package com.z_company.entity_converter

import com.z_company.domain.entities.route.Locomotive
import com.z_company.entity.Locomotive as LocomotiveRemote

internal object LocomotiveConverter {
    fun fromData(locomotive: Locomotive) = LocomotiveRemote(
        locoId = locomotive.locoId,
        basicId = locomotive.basicId,
        removeObjectId = locomotive.remoteObjectId ?: "",
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


    fun toData(entity: LocomotiveRemote) = Locomotive(
        locoId = entity.locoId,
        basicId = entity.basicId,
        remoteObjectId = entity.removeObjectId,
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
}