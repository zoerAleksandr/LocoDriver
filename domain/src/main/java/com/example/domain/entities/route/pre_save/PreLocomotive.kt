package com.example.domain.entities.route.pre_save

import com.example.domain.entities.route.LocoType
import com.example.domain.entities.route.SectionDiesel
import com.example.domain.entities.route.SectionElectric
import java.util.UUID

data class PreLocomotive(
    var locoId: String = UUID.randomUUID().toString(),
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
