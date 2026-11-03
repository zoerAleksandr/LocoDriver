package com.z_company.domain.entities.norma_time

import kotlinx.serialization.Serializable

/**
 * «Надгробие» удалённой станции справочника: чтобы удаление дошло до других устройств
 * при синхронизации, а не откатилось возвратом станции с сервера.
 */
@Serializable
data class StationTombstone(
    val stationId: String,
    val name: String,
    /** Момент удаления, мс (UTC) — сравнивается с `StationNorm.updatedAt` (кто новее). */
    val deletedAt: Long,
)
