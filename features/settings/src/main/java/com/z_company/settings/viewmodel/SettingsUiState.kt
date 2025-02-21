package com.z_company.settings.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.ServicePhase
import com.z_company.domain.entities.User
import com.z_company.domain.entities.UserSettings

data class SettingsUiState(
    val settingDetails: ResultState<UserSettings?> = ResultState.Loading,
    val userDetailsState: ResultState<User?> = ResultState.Loading,
    val calendarState: ResultState<MonthOfYear?> = ResultState.Loading,
    val saveSettingsState: ResultState<Unit>? = null,
    val updateRepositoryState: ResultState<Unit>? = null,
    val updateAt: Long? = null,
    val monthList: List<Int> = listOf(),
    val yearList: List<Int> = listOf(),
    val logOutState: ResultState<Unit>? = null,
    val resentVerificationEmailButton: Boolean = true,
    var purchasesEndTime: ResultState<String> = ResultState.Loading,
    val isRefreshing: Boolean = false,
    val showDialogAddServicePhase: Boolean = false,
    val selectedServicePhase: Pair<ServicePhase, Int>? = null,
    val servicePhases: SnapshotStateList<ServicePhase>? = mutableStateListOf(),
)