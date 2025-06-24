package com.z_company.settings.viewmodel

import com.z_company.core.ResultState
import com.z_company.domain.entities.UserSettings

data class SettingHomeScreenUIState(
    val currentSetting: ResultState<UserSettings?> = ResultState.Loading(),
    val saveSettingState: ResultState<Unit>? = null,
)