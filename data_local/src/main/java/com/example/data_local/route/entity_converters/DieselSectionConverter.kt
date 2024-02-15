package com.example.data_local.route.entity_converters

import com.example.domain.entities.route.SectionDiesel
import com.example.data_local.route.entity.SectionDiesel as SectionDieselEntity

internal object DieselSectionConverter {
    private fun fromData(section: SectionDiesel) = SectionDieselEntity(
        sectionId = section.sectionId,
        locoId = section.locoId,
        acceptedFuel = section.acceptedFuel,
        deliveryFuel = section.deliveryFuel,
        coefficient = section.coefficient,
        fuelSupply = section.fuelSupply,
        coefficientSupply = section.coefficientSupply
    )

    private fun toData(sectionEntity: SectionDieselEntity) = SectionDiesel(
        sectionId = sectionEntity.sectionId,
        locoId = sectionEntity.locoId,
        acceptedFuel = sectionEntity.acceptedFuel,
        deliveryFuel = sectionEntity.deliveryFuel,
        coefficient = sectionEntity.coefficient,
        fuelSupply = sectionEntity.fuelSupply,
        coefficientSupply = sectionEntity.coefficientSupply,
    )

    fun fromDataList(list: List<SectionDiesel>): MutableList<SectionDieselEntity> {
        return list.map {
            fromData(it)
        }.toMutableList()
    }

    fun toDataList(entityList: List<SectionDieselEntity>): MutableList<SectionDiesel> {
        return entityList.map { section ->
            toData(section)
        }.toMutableList()
    }
}