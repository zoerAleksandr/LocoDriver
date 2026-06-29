@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.domain.entities.norma_time

import com.z_company.domain.util.generateId
import kotlin.time.Clock
import kotlinx.serialization.Serializable

@Serializable
data class StationNorm(
    val stationId: String = generateId(),
    val name: String,
    val appearanceToStartMin: Int? = null,
    val endToBarrierMin: Int? = null,
    val barrierToStartMin: Int? = null,
    val endToWorkEndMin: Int? = null,
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds()
)
