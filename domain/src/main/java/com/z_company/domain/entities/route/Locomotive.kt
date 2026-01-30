package com.z_company.domain.entities.route

import kotlinx.serialization.Contextual
import java.math.BigDecimal
import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class Locomotive(
    var locoId: String = UUID.randomUUID().toString(),
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
    @Contextual
    var heatingCounterAccepted: BigDecimal? = null,
    @Contextual
    var heatingCounterDelivery: BigDecimal? = null,
)

@Serializable
data class SectionElectric(
    var sectionId: String = UUID.randomUUID().toString(),
    var locoId: String = "",
    var type: LocoType = LocoType.ELECTRIC,
    @Contextual
    var acceptedEnergy: BigDecimal? = null,
    @Contextual
    var deliveryEnergy: BigDecimal? = null,
    @Contextual
    var acceptedRecovery: BigDecimal? = null,
    @Contextual
    var deliveryRecovery: BigDecimal? = null,
    @Contextual
    var acceptedEnergyOtherCurrent: BigDecimal? = null,
    @Contextual
    var deliveryEnergyOtherCurrent: BigDecimal? = null,
    @Contextual
    var acceptedRecoveryOtherCurrent: BigDecimal? = null,
    @Contextual
    var deliveryRecoveryOtherCurrent: BigDecimal? = null
)

@Serializable
data class SectionDiesel(
    var sectionId: String = UUID.randomUUID().toString(),
    var locoId: String = "",
    var type: LocoType = LocoType.DIESEL,
    var acceptedFuel: Double? = null,
    var deliveryFuel: Double? = null,
    var coefficient: Double? = null,
    var fuelSupply: Double? = null,
    var fuelSupplyInKilo: Double? = null,
    var coefficientSupply: Double? = null,
)