package com.z_company.entity

import com.z_company.domain.entities.route.LocoType

data class SectionDiesel(
    var sectionId: String = "",
    var locoId: String = "",
    var type: LocoType = LocoType.DIESEL,
    var acceptedFuel: Double? = null,
    var deliveryFuel: Double? = null,
    var coefficient: Double? = null,
    var fuelSupply: Double? = null,
    var coefficientSupply: Double? = null,
)
