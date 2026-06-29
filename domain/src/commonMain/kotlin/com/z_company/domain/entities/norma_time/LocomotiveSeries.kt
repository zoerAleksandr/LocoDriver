@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.domain.entities.norma_time

import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.util.generateId
import kotlin.time.Clock
import kotlinx.serialization.Serializable

@Serializable
data class LocomotiveSeries(
    val seriesId: String = generateId(),
    val name: String,
    val type: LocoType,
    val acceptanceDurationMin: Int? = null,
    val deliveryDurationMin: Int? = null,
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds()
)
