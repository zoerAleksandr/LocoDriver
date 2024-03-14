package com.z_company.settings.viewmodel

import com.z_company.core.ResultState
import com.z_company.domain.entities.UserSettings

data class SettingsUiState(
    val settingDetails: ResultState<UserSettings?> = ResultState.Loading,
    val saveSettings: ResultState<Unit>? = null
)