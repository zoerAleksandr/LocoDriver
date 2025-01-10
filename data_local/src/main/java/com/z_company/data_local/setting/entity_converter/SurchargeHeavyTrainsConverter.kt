package com.z_company.data_local.setting.entity_converter

import com.z_company.domain.entities.SurchargeHeavyTrains
import com.z_company.data_local.setting.entity.SurchargeHeavyTrains as HeavyTrainsEntity

internal object SurchargeHeavyTrainsConverter {
    fun fromData(surchargeHeavyTrains: SurchargeHeavyTrains) =
        HeavyTrainsEntity(
            id = surchargeHeavyTrains.id,
            weight = surchargeHeavyTrains.weight,
            percentSurcharge = surchargeHeavyTrains.percentSurcharge
        )

    fun toData(entity: HeavyTrainsEntity) = SurchargeHeavyTrains(
        id = entity.id,
        weight = entity.weight,
        percentSurcharge = entity.percentSurcharge
    )

    fun fromDataList(list: List<SurchargeHeavyTrains>): MutableList<HeavyTrainsEntity>{
        return list.map {
            fromData(it)
        }.toMutableList()
    }

    fun toDataList(listEntity: List<HeavyTrainsEntity>): MutableList<SurchargeHeavyTrains> {
        return listEntity.map{
            toData(it)
        }.toMutableList()
    }
}