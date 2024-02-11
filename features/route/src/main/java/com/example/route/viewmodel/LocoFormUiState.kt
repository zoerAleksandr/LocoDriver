package com.example.route.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.core.ResultState
import com.example.domain.entities.route.Locomotive

data class LocoFormUiState(
    val locoDetailState: ResultState<Locomotive?> = ResultState.Loading,
    val dieselSectionList: SnapshotStateList<DieselSectionFormState>? = mutableStateListOf(),
    val electricSectionList: SnapshotStateList<ElectricSectionFormState>? = mutableStateListOf(),
    val saveLocoState: ResultState<Unit>? = null,
    val errorMessage: String? = null
)
