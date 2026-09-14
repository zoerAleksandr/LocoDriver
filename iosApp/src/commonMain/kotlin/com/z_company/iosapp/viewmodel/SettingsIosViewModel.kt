@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock

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
            settingsUseCase.saveSetting(
                settings.copy(updateAt = Clock.System.now().toEpochMilliseconds())
            ).collect {}
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

    fun setConsiderLocoHeatingInTotal(value: Boolean) {
        val current = _settings.value ?: return
        saveSetting(current.copy(isConsiderLocoHeatingInTotal = value))
    }

    fun setConsiderLocoAuxiliaryInTotal(value: Boolean) {
        val current = _settings.value ?: return
        saveSetting(current.copy(isConsiderLocoAuxiliaryInTotal = value))
    }

    fun setShowLocoStatistics(value: Boolean) {
        val current = _settings.value ?: return
        saveSetting(current.copy(isShowLocoStatistics = value))
    }

    fun setShowLocoNorma(value: Boolean) {
        val current = _settings.value ?: return
        saveSetting(current.copy(isShowLocoNorma = value))
    }

    fun setShowOtherCurrent(value: Boolean) {
        val current = _settings.value ?: return
        saveSetting(current.copy(isShowOtherCurrent = value))
    }

    fun setShowBreak(value: Boolean) {
        val current = _settings.value ?: return
        saveSetting(current.copy(isShowBreak = value))
    }

    /** nightStartHour and nightStartMinute come from the iOS time picker */
    fun setNightStartTime(hour: Int, minute: Int) {
        val current = _settings.value ?: return
        saveSetting(current.copy(nightTime = current.nightTime.copy(
            startNightHour = hour,
            startNightMinute = minute
        )))
    }

    /** nightEndHour and nightEndMinute come from the iOS time picker */
    fun setNightEndTime(hour: Int, minute: Int) {
        val current = _settings.value ?: return
        saveSetting(current.copy(nightTime = current.nightTime.copy(
            endNightHour = hour,
            endNightMinute = minute
        )))
    }

    /** minTimeRestPointOfTurnover in milliseconds */
    fun setMinTimeRest(millis: Long) {
        val current = _settings.value ?: return
        saveSetting(current.copy(minTimeRestPointOfTurnover = millis))
    }

    fun setMinTimeRestSecond(millis: Long) {
        val current = _settings.value ?: return
        saveSetting(current.copy(minTimeRestPointOfTurnoverSecond = millis))
    }

    /** minTimeHomeRest in milliseconds */
    fun setMinTimeHomeRest(millis: Long) {
        val current = _settings.value ?: return
        saveSetting(current.copy(minTimeHomeRest = millis))
    }

    /** defaultWorkTime in milliseconds (used when usingDefaultWorkTime is true) */
    fun setDefaultWorkTime(millis: Long) {
        val current = _settings.value ?: return
        saveSetting(current.copy(defaultWorkTime = millis))
    }
}
