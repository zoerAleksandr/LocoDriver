package com.example.route.viewmodel

import com.example.core.ResultState
import com.example.domain.entities.route.Locomotive

data class LocoFormUiState(
    val locoDetailState: ResultState<Locomotive?> = ResultState.Loading,
    val saveLocoState: ResultState<Unit>? = null,
    val errorMessage: String? = null
)
