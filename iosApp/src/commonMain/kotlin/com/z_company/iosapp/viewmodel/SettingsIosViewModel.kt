package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * KMP ViewModel для экрана настроек.
 *
 * Упрощённая версия SettingsViewModel из features/route (Android), без:
 *   - KoinComponent (конструкторная инжекция)
 *   - androidx.compose.runtime.snapshots (SnapshotStateList)
 *   - Android-зависимостей
 *
 * Шаг 17: добавить save/update-методы для редактирования настроек.
 */
class SettingsIosViewModel(
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

    // ── watchState helpers ────────────────────────────────────────────────────

    fun watchSettings(callback: (UserSettings?) -> Unit) {
        viewModelScope.launch { settings.collect { callback(it) } }
    }

    fun watchIsLoading(callback: (Boolean) -> Unit) {
        viewModelScope.launch { isLoading.collect { callback(it) } }
    }

    fun saveSetting(settings: UserSettings) {
        viewModelScope.launch {
            settingsUseCase.saveSetting(settings).collect {}
        }
    }

    fun setNormaHours(hours: Long) {
        val current = _settings.value ?: return
        saveSetting(current.copy(defaultWorkTime = hours * 3_600_000L))
    }

    fun setUsingDefaultWorkTime(value: Boolean) {
        val current = _settings.value ?: return
        saveSetting(current.copy(usingDefaultWorkTime = value))
    }

    fun setConsiderFutureRoute(value: Boolean) {
        val current = _settings.value ?: return
        saveSetting(current.copy(isConsiderFutureRoute = value))
    }

    fun setShowLocoHeating(value: Boolean) {
        val current = _settings.value ?: return
        saveSetting(current.copy(isShowLocoHeating = value))
    }

    fun setShowLocoAuxiliary(value: Boolean) {
        val current = _settings.value ?: return
        saveSetting(current.copy(isShowLocoAuxiliary = value))
    }

    fun setShowLocoStatistics(value: Boolean) {
        val current = _settings.value ?: return
        saveSetting(current.copy(isShowLocoStatistics = value))
    }

    fun setShowLocoNorma(value: Boolean) {
        val current = _settings.value ?: return
        saveSetting(current.copy(isShowLocoNorma = value))
    }
}
