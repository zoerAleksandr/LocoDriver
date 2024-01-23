package com.example.domain.entities.route

import java.util.UUID

data class Locomotive(
    var locoId: String = UUID.randomUUID().toString(),
    var basicId: String,
    var series: String? = null,
    var number: String? = null,
    var type: LocoType = LocoType.ELECTRIC,
    var electricSectionList: MutableList<SectionElectric> = mutableListOf(),
    var dieselSectionList: MutableList<SectionDiesel> = mutableListOf(SectionDiesel()),
    var timeStartOfAcceptance: Long? = null,
    var timeEndOfAcceptance: Long? = null,
    var timeStartOfDelivery: Long? = null,
    var timeEndOfDelivery: Long? = null
)

data class SectionElectric(
    var sectionId: String = UUID.randomUUID().toString(),
    var locoId: String = "",
    var type: LocoType = LocoType.ELECTRIC,
    var acceptedEnergy: Double? = null,
    var deliveryEnergy: Double? = null,
    var acceptedRecovery: Double? = null,
    var deliveryRecovery: Double? = null
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