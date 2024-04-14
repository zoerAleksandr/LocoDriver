package com.z_company.entity

import java.util.Date

data class BasicData(
    val id: String = "",
    var synch: Boolean = false,
    var objectId: String = "",
    var updatedAt: Date = Date(),
    var number: String? = null,
    var timeStartWork: Long? = null,
    var timeEndWork: Long? = null,
    var restPointOfTurnover: Boolean = false,
    var notes: String? = null
)
