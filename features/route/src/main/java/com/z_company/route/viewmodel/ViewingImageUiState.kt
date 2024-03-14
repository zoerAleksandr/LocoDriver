package com.z_company.route.viewmodel

import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Photo

data class ViewingImageUiState(
    val imageState: ResultState<Photo?> = ResultState.Loading,
    val removeImageState: ResultState<Unit>? = null
)
