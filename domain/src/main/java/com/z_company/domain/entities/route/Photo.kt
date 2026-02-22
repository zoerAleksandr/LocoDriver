package com.z_company.domain.entities.route

import com.z_company.domain.util.generateId
import kotlinx.serialization.Serializable

@Serializable
data class Photo(
    var photoId: String = generateId(),
    var basicId: String = "",
    var remoteObjectId: String? = null,
    var url: String,
    var dateOfCreate: Long
)
