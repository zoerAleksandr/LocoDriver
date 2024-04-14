package com.z_company.domain.entities.route

import java.util.Date
import java.util.UUID

data class BasicData(
    var id: String = UUID.randomUUID().toString(),
    var synch: Boolean = false,
    var updatedAt: Date = Date(),
    var number: String? = null,
    var timeStartWork: Long? = null,
    var timeEndWork: Long? = null,
    var restPointOfTurnover: Boolean = false,
    var notes: String? = null
)
