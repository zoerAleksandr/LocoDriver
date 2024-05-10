package com.z_company.entity

import com.z_company.domain.entities.route.LocoType

data class SectionElectric(
    var sectionId: String = "",
    var locoId: String = "",
    val type: LocoType = LocoType.ELECTRIC,
    var acceptedEnergy: Int? = null,
    var deliveryEnergy: Int? = null,
    var acceptedRecovery: Int? = null,
    var deliveryRecovery: Int? = null
)

