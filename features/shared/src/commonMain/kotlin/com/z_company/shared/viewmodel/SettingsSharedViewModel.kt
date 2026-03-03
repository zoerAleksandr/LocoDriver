package com.z_company.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shared ViewModel for the settings screen.
 */
class SettingsSharedViewModel(
    private val settingsUseCase: SettingsUseCase,
) : ViewModel() {

    private val _settings = MutableStateFlow<UserSettings?>(null)
    val settings: StateFlow<UserSettings?> = _settings.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            settingsUseCase.getUserSettingFlow().collect { userSettings ->
                _settings.value = userSettings
                _isLoading.value = false
            }
        }
    }

    fun setConsiderFutureRoute(value: Boolean) {
        val current = _settings.value ?: return
        val updated = current.copy(isConsiderFutureRoute = value)
        viewModelScope.launch {
            settingsUseCase.saveSetting(updated).collect { result ->
                if (result is ResultState.Success) {
                    _settings.value = updated
                }
            }
        }
    }

    fun setShowBreak(value: Boolean) {
        viewModelScope.launch {
            settingsUseCase.setShowBreak(value).collect { result ->
                if (result is ResultState.Success) {
                    _settings.value = _settings.value?.copy(isShowBreak = value)
                }
            }
        }
    }
}
