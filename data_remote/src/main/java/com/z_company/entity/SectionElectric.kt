package com.z_company.entity

import com.z_company.domain.entities.route.LocoType

data class SectionElectric(
    var sectionId: String = "",
    var locoId: String = "",
    val type: LocoType = LocoType.ELECTRIC,
    var acceptedEnergy: Double? = null,
    var deliveryEnergy: Double? = null,
    var acceptedRecovery: Double? = null,
    var deliveryRecovery: Double? = null
)

