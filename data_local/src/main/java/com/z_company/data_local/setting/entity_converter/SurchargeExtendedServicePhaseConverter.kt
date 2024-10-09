package com.z_company.data_local.setting.entity_converter

import com.z_company.domain.entities.SurchargeExtendedServicePhase
import com.z_company.data_local.setting.entity.SurchargeExtendedServicePhase as SurchargeEntity

internal object SurchargeExtendedServicePhaseConverter {
    fun fromData(surchargeExtendedServicePhase: SurchargeExtendedServicePhase) = SurchargeEntity(
        id = surchargeExtendedServicePhase.id,
        distance = surchargeExtendedServicePhase.distance,
        percentSurcharge = surchargeExtendedServicePhase.percentSurcharge
    )

    fun toData(entity: SurchargeEntity) = SurchargeExtendedServicePhase(
        id = entity.id,
        distance = entity.distance,
        percentSurcharge = entity.percentSurcharge
    )

    fun fromDataList(list: List<SurchargeExtendedServicePhase>): MutableList<SurchargeEntity>{
        return list.map {
            fromData(it)
        }.toMutableList()
    }

    fun toDataList(listEntity: List<SurchargeEntity>): MutableList<SurchargeExtendedServicePhase> {
        return listEntity.map{
            toData(it)
        }.toMutableList()
    }
}