package com.example.domain.entities.route

import java.util.UUID

data class BasicData(
    var id: String = UUID.randomUUID().toString(),
    var number: String? = null,
    var timeStartWork: Long? = null,
    var timeEndWork: Long? = null,
    var restPointOfTurnover: Boolean = false
)
