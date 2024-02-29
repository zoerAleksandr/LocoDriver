package com.example.route.viewmodel

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.core.ResultState
import com.example.domain.entities.route.Notes

data class NotesFormUiState(
    val notesDetailState: ResultState<Notes?> = ResultState.Loading,
    val saveNotesState: ResultState<Unit>? = null,
    val photosListState: SnapshotStateList<String>? = null
)