package com.z_company.domain.entities.route

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
)

data class SectionElectric(
    var sectionId: String = UUID.randomUUID().toString(),
    var locoId: String = "",
    var type: LocoType = LocoType.ELECTRIC,
    var acceptedEnergy: Int? = null,
    var deliveryEnergy: Int? = null,
    var acceptedRecovery: Int? = null,
    var deliveryRecovery: Int? = null
)
data class SectionDiesel(
    var sectionId: String = UUID.randomUUID().toString(),
    var locoId: String = "",
    var type: LocoType = LocoType.DIESEL,
    var acceptedFuel: Double? = null,
    var deliveryFuel: Double? = null,
    var coefficient: Double? = null,
    var fuelSupply: Double? = null,
    var coefficientSupply: Double? = null,
)