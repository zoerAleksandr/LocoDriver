package com.example.domain.entities

import java.util.UUID

data class Notes(
    val id: String = UUID.randomUUID().toString(),
    var text: String? = null,
    var photos: MutableList<String> = mutableListOf()
)
