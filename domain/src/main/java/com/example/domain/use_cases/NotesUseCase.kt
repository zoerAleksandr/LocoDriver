package com.example.domain.use_cases

import com.example.core.ResultState
import com.example.domain.entities.route.Notes
import com.example.domain.repositories.RouteRepository
import kotlinx.coroutines.flow.Flow

class NotesUseCase(
    private val repository: RouteRepository
) {
    fun saveNotes(notes: Notes): Flow<ResultState<Unit>> {
        return repository.saveNotes(notes)
    }
    fun loadNotes(notesId: String): Flow<ResultState<Notes?>> {
        return repository.loadNotes(notesId)
    }
}