package com.z_company.settings.viewmodel

import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.User
import com.z_company.domain.entities.UserSettings

data class SettingsUiState(
    val settingDetails: ResultState<UserSettings?> = ResultState.Loading,
    val userDetailsState: ResultState<User?> = ResultState.Loading,
    val calendarState: ResultState<MonthOfYear?> = ResultState.Loading,
    val saveSettingsState: ResultState<Unit>? = null,
    val updateRepositoryState: ResultState<Unit> = ResultState.Success(Unit),
    val updateAt: ResultState<Long> = ResultState.Loading,
    val monthList: List<Int> = listOf(),
    val yearList: List<Int> = listOf(),
    val logOutState: ResultState<Unit>? = null,
    val resentVerificationEmailButton: Boolean = true
)