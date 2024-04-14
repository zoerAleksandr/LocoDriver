package com.z_company.settings.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parse.ParseUser
import com.z_company.core.ResultState
import com.z_company.data_local.setting.DataStoreRepository
import com.z_company.use_case.LoginUseCase
import com.z_company.domain.entities.User
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.data_remote.RemoteRouteUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingsViewModel : ViewModel(), KoinComponent {
    private val calendarUseCase: CalendarUseCase by inject()
    private val loginUseCase: LoginUseCase by inject()
    private val remoteRouteUseCase: RemoteRouteUseCase by inject()
    private val dataStoreRepository: DataStoreRepository by inject()


    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private var loadSettingsJob: Job? = null
    private var saveSettingsJob: Job? = null

    private var loadLoginJob: Job? = null
    private var saveLoginJob: Job? = null


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

    var currentUser: User?
        get() {
            return _uiState.value.userDetailsState.let {
                if (it is ResultState.Success) it.data else null
            }
        }
        private set(value) {
            _uiState.update {
                it.copy(userDetailsState = ResultState.Success(value))
            }
        }

    init {
        loadSettings()
        loadLogin()
    }

    fun resetSaveState() {
        _uiState.update {
            it.copy(saveSettings = null)
        }
    }

    private fun loadSettings() {
        loadSettingsJob?.cancel()
        loadSettingsJob = dataStoreRepository.getMinTimeRest().onEach { time ->
            time?.let {
                currentSettings = currentSettings?.copy(
                    minTimeRest = it
                ) ?: UserSettings(minTimeRest = time)
            }
        }.launchIn(viewModelScope)
    }

    private fun loadLogin() {
        loadLoginJob?.cancel()
        loadLoginJob = loginUseCase.getUser().onEach { resultState ->
            _uiState.update {
                it.copy(userDetailsState = resultState)
            }
            if (resultState is ResultState.Success) {
                currentUser = resultState.data
            }
        }.launchIn(viewModelScope)

    }

    fun saveSettings() {
        currentSettings?.let { setting ->
            Log.d("ZZZ", "setting.minTimeRest = ${setting.minTimeRest}")
            saveSettingsJob?.cancel()
            saveSettingsJob = dataStoreRepository.setMinTimeRest(
                setting.minTimeRest
            ).onEach { resultState ->
                if (resultState is ResultState.Success) {
                    _uiState.update {
                        it.copy(saveSettings = resultState)
                    }
                }
            }.launchIn(viewModelScope)
        }
    }

    fun changedMinTimeRest(time: String) {
        currentSettings = currentSettings?.copy(
            minTimeRest = time.toLongOrNull()
        )
    }

    fun logOut() {
        ParseUser.logOutInBackground()
    }

    fun onSync() {
        viewModelScope.launch {
            remoteRouteUseCase.syncData()
        }
    }
}