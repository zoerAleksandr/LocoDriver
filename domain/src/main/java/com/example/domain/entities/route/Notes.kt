package com.example.domain.entities.route

import java.util.UUID

data class Notes(
    var notesId: String = UUID.randomUUID().toString(),
    var baseId: String = "",
    var text: String? = null,
    var photos: MutableList<String> = mutableListOf()
)
