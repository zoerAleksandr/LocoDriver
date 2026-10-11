package com.z_company.domain.entities.norma_time

import com.z_company.domain.entities.route.LocoType
import kotlinx.serialization.Serializable

/** «Надгробие» удалённой серии локомотива — см. [StationTombstone]. */
@Serializable
data class LocomotiveSeriesTombstone(
    val seriesId: String,
    val name: String,
    val type: LocoType,
    /** Момент удаления, мс (UTC) — сравнивается с `LocomotiveSeries.updatedAt`. */
    val deletedAt: Long,
)
