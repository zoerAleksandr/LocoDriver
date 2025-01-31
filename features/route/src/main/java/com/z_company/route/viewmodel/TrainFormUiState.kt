package com.z_company.route.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.z_company.core.ResultState
import com.z_company.domain.entities.ServicePhase
import com.z_company.domain.entities.route.Train

data class TrainFormUiState(
    val trainDetailState: ResultState<Train?> = ResultState.Loading,
    val saveTrainState: ResultState<Unit>? = null,
    val stationsListState: SnapshotStateList<StationFormState>? = mutableStateListOf(),
    val errorMessage: String? = null,
    val exitFromScreen: Boolean = false,
    val changesHaveState: Boolean = false,
    val confirmExitDialogShow: Boolean = false,
    val isExpandedDropDownMenuStation: Pair<Int, Boolean>? = null,
    val isShowDialogSelectServicePhase: Boolean = false,
    val servicePhaseList: SnapshotStateList<ServicePhase> = mutableStateListOf(),
    val selectedServicePhase: ServicePhase? = null
)