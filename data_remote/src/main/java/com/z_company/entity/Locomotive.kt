package com.z_company.entity

import com.z_company.domain.entities.route.LocoType
import java.util.UUID

data class Locomotive(
    var locoId: String = UUID.randomUUID().toString(),
    var basicId: String,
    var removeObjectId: String = "",
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
