@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.domain.entities.norma_time

import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.util.generateId
import kotlin.time.Clock
import kotlinx.serialization.Serializable

enum class SectionNumberingType {
    NUMERIC,
    LETTERS,
}

@Serializable
data class LocomotiveSeries(
    val seriesId: String = generateId(),
    val name: String,
    val type: LocoType,
    // Нормы «После отстоя без бригады» (вариант по умолчанию в шторке времени).
    val acceptanceDurationMin: Int? = null,
    val deliveryDurationMin: Int? = null,
    // Нормы «Из рук в руки» (передача локомотива без отстоя).
    val acceptanceHandToHandMin: Int? = null,
    val deliveryHandToHandMin: Int? = null,
    val sectionNumberingType: SectionNumberingType = SectionNumberingType.NUMERIC,
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds()
)
