package com.z_company.domain.entities.route

import java.util.Date
import java.util.UUID

data class BasicData(
    var id: String = UUID.randomUUID().toString(),
    var isSynchronizedRoute: Boolean = false,
    var remoteRouteId: String? = null,
    var isSynchronized: Boolean = false,
    var remoteObjectId: String? = null,
    var isDeleted: Boolean = false,
    var updatedAt: Date = Date(),
    var number: String? = null,
    var timeStartWork: Long? = null,
    var timeEndWork: Long? = null,
    var restPointOfTurnover: Boolean = false,
    var notes: String? = null
)