package com.example.data_local.route.entity_converters

import com.example.domain.entities.route.Notes
import com.example.data_local.route.entity.Notes as NotesEntity

internal object NotesConverter {
    fun fromData(notes: Notes) = NotesEntity(
        notes.notesId,
        notes.routeId,
        notes.text,
        notes.photos
    )

    fun toData(entity: NotesEntity) = Notes().apply {
        notesId = entity.notesId
        routeId = entity.routeId
        text = entity.text
        photos = entity.photos
    }
}