package com.z_company.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parse.ParseUser
import com.z_company.core.ResultState
import com.z_company.use_case.LoginUseCase
import com.z_company.domain.entities.User
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.use_case.RemoteRouteUseCase
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
    private val loginUseCase: LoginUseCase by inject()
    private val remoteRouteUseCase: RemoteRouteUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()


    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private var loadSettingsJob: Job? = null
    private var saveSettingsJob: Job? = null

    private var loadLoginJob: Job? = null


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
            it.copy(saveSettingsState = null)
        }
    }

    private fun loadSettings() {
        loadSettingsJob?.cancel()
        loadSettingsJob = viewModelScope.launch {
            settingsUseCase.getCurrentSettings().collect { result ->
                _uiState.update {
                    it.copy(
                        settingDetails = result,
                    )
                }
                if (result is ResultState.Success) {
                    result.data?.let { userSettings ->
                        _uiState.update {
                            it.copy(
                                updateAt = ResultState.Success(userSettings.updateAt),
                                minRestTime = ResultState.Success(userSettings.minTimeRest),
                                minHomeRestTime = ResultState.Success(userSettings.minTimeHomeRest),
                                defaultTypeLoco = ResultState.Success(userSettings.defaultLocoType),
                                defaultWorkTime = ResultState.Success(userSettings.defaultWorkTime)
                            )
                        }
                    }
                }
            }
        }
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
        val state = uiState.value.settingDetails
        if (state is ResultState.Success) {
            state.data?.let { settings ->
                saveSettingsJob?.cancel()
                saveSettingsJob = viewModelScope.launch {
                    settingsUseCase.saveSetting(settings).collect { result ->
                        _uiState.update {
                            it.copy(
                                saveSettingsState = result
                            )
                        }
                    }
                }
            }
        }
    }

    fun changedMinTimeRest(time: String) {
        time.toLongOrNull()?.let { hour ->
            currentSettings = currentSettings?.copy(
                minTimeRest = hour.times(3_600_00L)
            )
        }
    }

    fun logOut() {
        // TODO сделать выход из аккаунта
        ParseUser.logOutInBackground()
    }

    fun onSync() {
        viewModelScope.launch {
            remoteRouteUseCase.syncBasicData().collect { result ->
                _uiState.update {
                    it.copy(
                        updateRepositoryState = result
                    )
                }
            }
        }
    }

    fun loadDataFromRemote() {
        viewModelScope.launch {
            remoteRouteUseCase.loadingRoutesFromRemote()
        }
    }

    fun changedDefaultWorkTime(timeInMillis: Long) {
        currentSettings = currentSettings?.copy(
            defaultWorkTime = timeInMillis
        )
//        viewModelScope.launch {
//            settingsUseCase.setWorkTimeDefault(timeInMillis).collect { result ->
//                if (result is ResultState.Success) {
//                    _uiState.update {
//                        it.copy(
//                            defaultWorkTime = ResultState.Success(timeInMillis)
//                        )
//                    }
//                }
//            }
//        }
    }
}