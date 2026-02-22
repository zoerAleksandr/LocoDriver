package com.z_company.domain.entities.route

import com.z_company.domain.util.generateId
import kotlinx.serialization.Serializable

@Serializable
data class Locomotive(
    var locoId: String = generateId(),
    var basicId: String,
    var remoteObjectId: String? = null,
    var series: String? = null,
    var number: String? = null,
    var type: LocoType = LocoType.ELECTRIC,
    var electricSectionList: MutableList<SectionElectric> = mutableListOf(),
    var dieselSectionList: MutableList<SectionDiesel> = mutableListOf(),
    var timeStartOfAcceptance: Long? = null,
    var timeEndOfAcceptance: Long? = null,
    var timeStartOfDelivery: Long? = null,
    var timeEndOfDelivery: Long? = null,
    var normaElectricCurrent1: Int? = null,
    var normaElectricCurrent2: Int? = null,
    var normaDiesel: String? = null,
    var heatingCounterAccepted: Double? = null,
    var heatingCounterDelivery: Double? = null,
)

@Serializable
data class SectionElectric(
    var sectionId: String = generateId(),
    var locoId: String = "",
    var type: LocoType = LocoType.ELECTRIC,
    var acceptedEnergy: Double? = null,
    var deliveryEnergy: Double? = null,
    var acceptedRecovery: Double? = null,
    var deliveryRecovery: Double? = null,
    var acceptedEnergyOtherCurrent: Double? = null,
    var deliveryEnergyOtherCurrent: Double? = null,
    var acceptedRecoveryOtherCurrent: Double? = null,
    var deliveryRecoveryOtherCurrent: Double? = null
)

@Serializable
data class SectionDiesel(
    var sectionId: String = generateId(),
    var locoId: String = "",
    var type: LocoType = LocoType.DIESEL,
    var acceptedFuel: Double? = null,
    var deliveryFuel: Double? = null,
    var coefficient: Double? = null,
    var fuelSupply: Double? = null,
    var fuelSupplyInKilo: Double? = null,
    var coefficientSupply: Double? = null,
)
