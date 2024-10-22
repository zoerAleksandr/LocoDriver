package com.z_company.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingHomeScreenViewModel : ViewModel(), KoinComponent {
    private val settingsUseCase: SettingsUseCase by inject()

    private val _uiState = MutableStateFlow(SettingHomeScreenUIState())
    val uiState = _uiState.asStateFlow()
    private var currentSettings: UserSettings?
        get() {
            return _uiState.value.currentSetting.let {
                if (it is ResultState.Success) it.data else null
            }
        }
        private set(value) {
            _uiState.update {
                it.copy(currentSetting = ResultState.Success(value))
            }
        }


    fun changeIsVisibleNightTime(b: Boolean) {
        currentSettings = currentSettings?.copy(
            // todo
        )
    }

    fun resetSaveState() {
        _uiState.update {
            it.copy(saveSettingState = null)
        }
    }

    fun saveSetting() {
        viewModelScope.launch(Dispatchers.IO) {
            currentSettings?.let {
                settingsUseCase.saveSetting(it).collect { result ->
                    _uiState.update { state ->
                        state.copy(saveSettingState = result)
                    }
                }
            }
        }
    }

    private fun loadSetting(){
        viewModelScope.launch {
            settingsUseCase.getCurrentSettings().collect{ result ->
                _uiState.update {
                    it.copy(
                        currentSetting = result,
                    )
                }
            }
        }
    }

    init {
        loadSetting()
    }
}