package com.example.route.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.core.ResultState
import com.example.domain.entities.route.Train

data class TrainFormUiState(
    val trainDetailState: ResultState<Train?> = ResultState.Loading,
    val saveTrainState: ResultState<Unit>? = null,
    val stationsListState: SnapshotStateList<StationFormState>? = mutableStateListOf(),
    val errorMessage: String? = null
)