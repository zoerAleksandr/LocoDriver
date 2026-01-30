package com.z_company.domain.entities.route

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Photo(
    var photoId: String = UUID.randomUUID().toString(),
    var basicId: String = "",
    var remoteObjectId: String? = null,
    var url: String,
    var dateOfCreate: Long
)
