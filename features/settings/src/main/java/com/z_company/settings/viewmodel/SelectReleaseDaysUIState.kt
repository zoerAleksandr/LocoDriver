package com.z_company.settings.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.ReleasePeriod
import com.z_company.domain.entities.UserSettings

data class SelectReleaseDaysUIState(
    val releaseDaysPeriodState: SnapshotStateList<ReleasePeriod>? = mutableStateListOf(),
    val saveReleaseDaysState: ResultState<Unit>? = null,
    val currentMonthOfYearState: ResultState<MonthOfYear?> = ResultState.Loading(),
    val userSettingsState: ResultState<UserSettings?> = ResultState.Loading(),
    val monthList: List<Int> = listOf(),
    val yearList: List<Int> = listOf(),
)
