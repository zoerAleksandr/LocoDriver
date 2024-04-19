package com.z_company.entity_converter

import com.z_company.domain.entities.route.SectionElectric
import com.z_company.entity.SectionElectric as SectionElectricRemote

internal object ElectricSectionConverter {
    private fun fromData(section: SectionElectric) = SectionElectricRemote(
        sectionId = section.sectionId,
        locoId = section.locoId,
        acceptedEnergy = section.acceptedEnergy,
        deliveryEnergy = section.deliveryEnergy,
        acceptedRecovery = section.acceptedRecovery,
        deliveryRecovery = section.deliveryRecovery
    )

    private fun toData(sectionEntity: SectionElectricRemote) = SectionElectric(
        sectionId = sectionEntity.sectionId,
        locoId = sectionEntity.locoId,
        acceptedEnergy = sectionEntity.acceptedEnergy,
        deliveryEnergy = sectionEntity.deliveryEnergy,
        acceptedRecovery = sectionEntity.acceptedRecovery,
        deliveryRecovery = sectionEntity.deliveryRecovery,
    )

    fun fromDataList(list: List<SectionElectric>): MutableList<SectionElectricRemote> {
        return list.map {
            fromData(it)
        }.toMutableList()
    }

    fun toDataList(entityList: List<SectionElectricRemote>): MutableList<SectionElectric> {
        return entityList.map { section ->
            toData(section)
        }.toMutableList()
    }
}