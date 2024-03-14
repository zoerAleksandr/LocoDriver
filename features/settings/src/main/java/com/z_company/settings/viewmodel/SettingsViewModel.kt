package com.z_company.settings.viewmodel

import androidx.lifecycle.ViewModel
import com.z_company.core.ResultState
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.use_cases.CalendarUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingsViewModel : ViewModel(), KoinComponent {
    private val calendarUseCase: CalendarUseCase by inject()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private var loadSettingsJob: Job? = null
    private var saveSettingsJob: Job? = null

    var currentSettings: UserSettings?
        get() {
            return _uiState.value.settingDetails.let {
                if (it is ResultState.Success) it.data else null
            }
        }
        private set(value) {
            _uiState.update {
                it.copy(settingDetails = ResultState.Success(value))
            }
        }

    init {
        loadSettings()
    }

    fun resetSaveState() {
        _uiState.update {
            it.copy(saveSettings = null)
        }
    }
    private fun loadSettings() {

    }

    fun saveSettings() {

    }
}