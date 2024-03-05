package com.example.route.viewmodel

import com.example.core.ResultState
import com.example.domain.entities.route.Photo

data class NotesFormUiState(
    val notesText: String? = null,
    val photosListState: ResultState<List<Photo>> = ResultState.Loading
)