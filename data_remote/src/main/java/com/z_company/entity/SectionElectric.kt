package com.z_company.entity

import com.z_company.domain.entities.route.LocoType
import java.math.BigDecimal

data class SectionElectric(
    var sectionId: String = "",
    var locoId: String = "",
    val type: LocoType = LocoType.ELECTRIC,
    var acceptedEnergy: BigDecimal? = null,
    var deliveryEnergy: BigDecimal? = null,
    var acceptedRecovery: BigDecimal? = null,
    var deliveryRecovery: BigDecimal? = null
)

