package com.z_company.entity

import java.net.URL


data class Photo(
    var photoId: String = "",
    var basicId: String = "",
    var remoteObjectId: String? = null,
    var url: String,
    var dateOfCreate: Long = 0L
)
