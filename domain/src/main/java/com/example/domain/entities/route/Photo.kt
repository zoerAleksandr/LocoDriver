package com.example.domain.entities.route

import java.util.UUID

data class Photo(
    var photoId: String = UUID.randomUUID().toString(),
    var basicId: String = "",
    var base64: String,
    var dateOfCreate: Long
)
