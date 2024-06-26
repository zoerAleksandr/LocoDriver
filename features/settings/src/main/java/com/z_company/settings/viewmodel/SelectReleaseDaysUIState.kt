package com.z_company.settings.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.ReleasePeriod

data class SelectReleaseDaysUIState(
    val releaseDaysPeriodState: SnapshotStateList<ReleasePeriod>? = mutableStateListOf(),
    val saveReleaseDaysState: ResultState<Unit>? = null,
    val calendarState: ResultState<MonthOfYear?> = ResultState.Loading,
)
