package com.z_company.route.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.z_company.core.ResultState
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.entities.route.Locomotive

data class LocoFormUiState(
    val locoDetailState: ResultState<Locomotive?> = ResultState.Loading,
    val dieselSectionList: SnapshotStateList<DieselSectionFormState>? = mutableStateListOf(),
    val electricSectionList: SnapshotStateList<ElectricSectionFormState>? = mutableStateListOf(),
    val saveLocoState: ResultState<Unit>? = null,
    val errorMessage: String? = null,
    val refuelDialogShow: Pair<Boolean, Int> = Pair(false, 0),
    val coefficientDialogShow: Pair<Boolean, Int> = Pair(false, 0),
    val settingsState: ResultState<UserSettings?> = ResultState.Loading,
    val exitFromScreen: Boolean = false,
    val changesHaveState: Boolean = false,
    val confirmExitDialogShow: Boolean = false
    )
