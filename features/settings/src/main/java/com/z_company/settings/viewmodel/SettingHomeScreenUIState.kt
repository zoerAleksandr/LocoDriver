package com.z_company.settings.viewmodel

import com.z_company.core.ResultState

data class SettingHomeScreenUIState(
    val saveSettingState: ResultState<Unit>? = null,
    val isVisibleNightTime: Boolean = false,
    val isVisiblePassengerTime: Boolean = false,
    val isVisibleRelationTime: Boolean = false,
)