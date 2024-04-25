package com.z_company.route.viewmodel

import com.z_company.core.ResultState

data class CreatePhotoScreenUiState(
    val savePhotoState : ResultState<Unit>? = null
)
