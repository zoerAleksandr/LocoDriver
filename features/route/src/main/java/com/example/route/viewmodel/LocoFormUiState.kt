package com.example.route.viewmodel

import com.example.core.ResultState
import com.example.domain.entities.route.pre_save.PreLocomotive

data class LocoFormUiState(
    val locoDetailState: ResultState<PreLocomotive?> = ResultState.Loading,
    val saveLocoState: ResultState<Unit>? = null,
    val errorMessage: String? = null
)
