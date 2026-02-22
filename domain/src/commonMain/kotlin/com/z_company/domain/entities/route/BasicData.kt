package com.z_company.domain.entities.route

import com.z_company.domain.util.generateId
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Serializable
data class BasicData(
    var id: String = generateId(),
    var isSynchronizedRoute: Boolean = false,
    var remoteRouteId: String? = null,
    var isSynchronized: Boolean = false,
    var remoteObjectId: String? = null,
    var isOnePersonOperation: Boolean = false,
    var isDeleted: Boolean = false,
    var updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
    var number: String? = null,
    var timeStartWork: Long? = null,
    var timeEndWork: Long? = null,
    var restPointOfTurnover: Boolean = false,
    var notes: String? = null,
    var isFavorite: Boolean = false
)
