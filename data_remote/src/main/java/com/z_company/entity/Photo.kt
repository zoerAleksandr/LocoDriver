package com.z_company.entity


data class Photo(
    var photoId: String = "",
    var basicId: String = "",
    var remoteObjectId: String? = null,
    var base64: String = "",
    var dateOfCreate: Long = 0L
)
