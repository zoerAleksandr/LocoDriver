package com.z_company.domain.entities.route

import java.math.BigDecimal
import java.util.UUID

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
    var timeEndOfDelivery: Long? = null
): java.io.Serializable

data class SectionElectric(
    var sectionId: String = UUID.randomUUID().toString(),
    var locoId: String = "",
    var type: LocoType = LocoType.ELECTRIC,
    var acceptedEnergy: BigDecimal? = null,
    var deliveryEnergy: BigDecimal? = null,
    var acceptedRecovery: BigDecimal? = null,
    var deliveryRecovery: BigDecimal? = null
): java.io.Serializable
data class SectionDiesel(
    var sectionId: String = UUID.randomUUID().toString(),
    var locoId: String = "",
    var type: LocoType = LocoType.DIESEL,
    var acceptedFuel: Double? = null,
    var deliveryFuel: Double? = null,
    var coefficient: Double? = null,
    var fuelSupply: Double? = null,
    var coefficientSupply: Double? = null,
): java.io.Serializable