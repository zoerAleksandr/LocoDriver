package com.example.domain.entities.route

import java.util.UUID

data class Locomotive(
    val id: String = UUID.randomUUID().toString(),
    var series: String? = null,
    var number: String? = null,
    var type: LocoType = LocoType.ELECTRIC,
    var electricSectionList: List<SectionElectric>? = null,
    var dieselSectionList: List<SectionDiesel>? = null,
    var timeStartOfAcceptance: Long? = null,
    var timeEndOfAcceptance: Long? = null,
    var timeStartOfDelivery: Long? = null,
    var timeEndOfDelivery: Long? = null
)

data class SectionElectric(
    val id: String = UUID.randomUUID().toString(),
    val type: LocoType = LocoType.ELECTRIC,
    var acceptedEnergy: Double? = null,
    var deliveryEnergy: Double? = null,
    var acceptedRecovery: Double? = null,
    var deliveryRecovery: Double? = null
)
data class SectionDiesel(
    val id: String = UUID.randomUUID().toString(),
    val type: LocoType = LocoType.DIESEL,
    val acceptedEnergy: Double? = null,
    val deliveryEnergy: Double? = null,
    var coefficient: Double? = null,
    var fuelSupply: Double? = null,
    var coefficientSupply: Double? = null,
)