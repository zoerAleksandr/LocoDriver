package com.z_company.data_local.route.entity_converters

import com.z_company.domain.entities.route.SectionElectric
import com.z_company.data_local.route.entity.SectionElectric as SectionElectricEntity
import java.math.BigDecimal

internal object ElectricSectionConverter {
    private fun fromData(section: SectionElectric) = SectionElectricEntity(
        sectionId = section.sectionId,
        locoId = section.locoId,
        acceptedEnergy = section.acceptedEnergy?.let { BigDecimal(it) },
        deliveryEnergy = section.deliveryEnergy?.let { BigDecimal(it) },
        acceptedRecovery = section.acceptedRecovery?.let { BigDecimal(it) },
        deliveryRecovery = section.deliveryRecovery?.let { BigDecimal(it) },
        acceptedEnergyOtherCurrent = section.acceptedEnergyOtherCurrent?.let { BigDecimal(it) },
        deliveryEnergyOtherCurrent = section.deliveryEnergyOtherCurrent?.let { BigDecimal(it) },
        acceptedRecoveryOtherCurrent = section.acceptedRecoveryOtherCurrent?.let { BigDecimal(it) },
        deliveryRecoveryOtherCurrent = section.deliveryRecoveryOtherCurrent?.let { BigDecimal(it) },
    )

    private fun toData(sectionEntity: SectionElectricEntity) = SectionElectric(
        sectionId = sectionEntity.sectionId,
        locoId = sectionEntity.locoId,
        acceptedEnergy = sectionEntity.acceptedEnergy?.toDouble(),
        deliveryEnergy = sectionEntity.deliveryEnergy?.toDouble(),
        acceptedRecovery = sectionEntity.acceptedRecovery?.toDouble(),
        deliveryRecovery = sectionEntity.deliveryRecovery?.toDouble(),
        acceptedEnergyOtherCurrent = sectionEntity.acceptedEnergyOtherCurrent?.toDouble(),
        deliveryEnergyOtherCurrent = sectionEntity.deliveryEnergyOtherCurrent?.toDouble(),
        acceptedRecoveryOtherCurrent = sectionEntity.acceptedRecoveryOtherCurrent?.toDouble(),
        deliveryRecoveryOtherCurrent = sectionEntity.deliveryRecoveryOtherCurrent?.toDouble(),
    )

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
