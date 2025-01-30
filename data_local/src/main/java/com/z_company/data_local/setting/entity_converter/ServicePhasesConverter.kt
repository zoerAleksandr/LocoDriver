package com.z_company.data_local.setting.entity_converter

import com.z_company.domain.entities.ServicePhase
import com.z_company.data_local.setting.entity.ServicePhase as ServicePhaseEntity

internal object ServicePhasesConverter {

    fun fromData(servicePhase: ServicePhase) = ServicePhaseEntity(
        departureStation = servicePhase.departureStation,
        arrivalStation = servicePhase.arrivalStation,
        distance = servicePhase.distance
    )
    fun toData(entity: ServicePhaseEntity) = ServicePhase(
        departureStation = entity.departureStation,
        arrivalStation = entity.arrivalStation,
        distance = entity.distance
    )

    fun fromDataList(list: List<ServicePhase>): MutableList<ServicePhaseEntity>{
        return list.map {
            fromData(it)
        }.toMutableList()
    }

    fun toDataList(listEntity: List<ServicePhaseEntity>): MutableList<ServicePhase> {
        return listEntity.map{
            toData(it)
        }.toMutableList()
    }
}