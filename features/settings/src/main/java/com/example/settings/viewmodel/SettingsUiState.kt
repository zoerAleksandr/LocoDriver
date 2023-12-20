package com.example.settings.viewmodel

import com.example.core.ResultState
import com.example.domain.entities.UserSettings

data class SettingsUiState(
    val settingDetails: ResultState<UserSettings?> = ResultState.Loading,
    val saveSettings: ResultState<Unit>? = null
)