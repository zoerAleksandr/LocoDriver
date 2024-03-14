package com.example.route.viewmodel

import com.example.core.ResultState
import com.example.domain.entities.route.Photo

data class ViewingImageUiState(
    val imageState: ResultState<Photo?> = ResultState.Loading,
    val removeImageState: ResultState<Unit>? = null
)
