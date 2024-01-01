package com.example.data_local.route.entity_converters

import com.example.domain.entities.route.SectionElectric
import com.example.data_local.route.entity.SectionElectric as SectionElectricEntity

internal object ElectricSectionConverter {
    private fun fromData(section: SectionElectric) = SectionElectricEntity(
        sectionId = section.sectionId,
        locoId = section.locoId,
        acceptedEnergy = section.acceptedEnergy,
        deliveryEnergy = section.deliveryEnergy,
        acceptedRecovery = section.acceptedRecovery,
        deliveryRecovery = section.deliveryRecovery
    )

    private fun toData(sectionEntity: SectionElectricEntity) = SectionElectric().apply {
        sectionId = sectionEntity.sectionId
        locoId = sectionEntity.locoId
        acceptedEnergy = sectionEntity.acceptedEnergy
        deliveryEnergy = sectionEntity.deliveryEnergy
        acceptedRecovery = sectionEntity.acceptedRecovery
        deliveryRecovery = sectionEntity.deliveryRecovery
    }

    fun fromDataList(list: List<SectionElectric>): MutableList<SectionElectricEntity> {
        return list.map {
            fromData(it)
        }.toMutableList()
    }

    fun toDataList(entityList: List<SectionElectricEntity>): MutableList<SectionElectric> {
        return entityList.map { section ->
            toData(section)
        }.toMutableList()
    }
}