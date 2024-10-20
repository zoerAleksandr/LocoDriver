package com.z_company.settings.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingHomeScreenViewModel: ViewModel() {

    private val _uiState = MutableStateFlow(SettingHomeScreenUIState())
    val uiState = _uiState.asStateFlow()

    fun changeIsVisibleNightTime(b: Boolean) {

    }

    fun saveSetting() {

    }
}