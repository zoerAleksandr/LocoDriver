package com.z_company.settings.viewmodel

import com.z_company.core.ResultState
import com.z_company.domain.entities.User
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.entities.route.LocoType

data class SettingsUiState(
    val settingDetails: ResultState<UserSettings?> = ResultState.Loading,
    val userDetailsState: ResultState<User?> = ResultState.Loading,
    val saveSettingsState: ResultState<Unit>? = null,
    val updateRepositoryState: ResultState<Unit> = ResultState.Success(Unit),
    val updateAt: ResultState<Long> = ResultState.Loading,
)