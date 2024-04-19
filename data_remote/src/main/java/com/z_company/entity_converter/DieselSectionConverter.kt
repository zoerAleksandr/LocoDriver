package com.z_company.entity_converter

import com.z_company.domain.entities.route.SectionDiesel
import com.z_company.entity.SectionDiesel as SectionDieselRemote

internal object DieselSectionConverter {
    private fun fromData(section: SectionDiesel) = SectionDieselRemote(
        sectionId = section.sectionId,
        locoId = section.locoId,
        acceptedFuel = section.acceptedFuel,
        deliveryFuel = section.deliveryFuel,
        coefficient = section.coefficient,
        fuelSupply = section.fuelSupply,
        coefficientSupply = section.coefficientSupply
    )

    private fun toData(sectionEntity: SectionDieselRemote) = SectionDiesel(
        sectionId = sectionEntity.sectionId,
        locoId = sectionEntity.locoId,
        acceptedFuel = sectionEntity.acceptedFuel,
        deliveryFuel = sectionEntity.deliveryFuel,
        coefficient = sectionEntity.coefficient,
        fuelSupply = sectionEntity.fuelSupply,
        coefficientSupply = sectionEntity.coefficientSupply,
    )

    fun fromDataList(list: List<SectionDiesel>): MutableList<SectionDieselRemote> {
        return list.map {
            fromData(it)
        }.toMutableList()
    }

    fun toDataList(entityList: List<SectionDieselRemote>): MutableList<SectionDiesel> {
        return entityList.map { section ->
            toData(section)
        }.toMutableList()
    }
}