package com.z_company.route.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parse.ParseUser
import com.z_company.SessionManager
import com.z_company.core.ResultState
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.core.util.isEmailValid
import com.z_company.domain.entities.User
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.repository.Back4AppManager
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.use_case.AuthUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.z_company.use_case.LoginUseCase
import com.z_company.use_case.RemoteRouteUseCase

data class ProfileUiState(
    val userDetailsState: ResultState<User?> = ResultState.Loading(),
    val purchasesEndTime: ResultState<String> = ResultState.Loading(),
    val uploadState: ResultState<Int>? = null,
    val downloadState: ResultState<Int>? = null,
    val logOutState: ResultState<Unit>? = null,
    val updateAt: Long? = null,
    val dateAndTimeConverter: DateAndTimeConverter? = null,
    val isRefreshing: Boolean = false,
    val resentVerificationEmailButton: Boolean = false,
)

class ProfileViewModel : ViewModel(), KoinComponent {
    private val authUseCase: AuthUseCase by inject()
    private val loginUseCase: LoginUseCase by inject()
    private val back4AppManager: Back4AppManager by inject()
    private val sharedPrefs: SharedPreferencesRepositories by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val remoteRouteUseCase: RemoteRouteUseCase by inject()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    var currentEmail by mutableStateOf("")
        private set

    private var loadUserJob: Job? = null
    private var loadSettingsJob: Job? = null

    init {
        loadUser()
        loadSettingsForSyncInfo()
        loadPurchasesInfo()
    }

    private fun loadUser() {
        loadUserJob?.cancel()
        loadUserJob = viewModelScope.launch(Dispatchers.IO) {
            loginUseCase.getUser().collect { result ->
                _uiState.update { it.copy(userDetailsState = result) }
                if (result is ResultState.Success) {
                    currentEmail = result.data?.email ?: ""
                }
            }
        }
    }

    private fun loadSettingsForSyncInfo() {
        loadSettingsJob = settingsUseCase.getFlowCurrentSettingsState().onEach { result ->
            if (result is ResultState.Success) {
                result.data?.let { settings ->
                    _uiState.update {
                        it.copy(
                            updateAt = settings.updateAt,
                            dateAndTimeConverter = DateAndTimeConverter(settings)
                        )
                    }
                    // Обновляем подписку, т.к. нужен converter
                    loadPurchasesInfo()
                }
            }
        }.launchIn(viewModelScope)
    }

    fun loadPurchasesInfo() {
        viewModelScope.launch {
            val expiration = sharedPrefs.getSubscriptionExpiration()
            val text = if (expiration == 0L) {
                ""
            } else {
                _uiState.value.dateAndTimeConverter?.getDateMiniAndTime(expiration) ?: ""
            }
            _uiState.update { it.copy(purchasesEndTime = ResultState.Success(text)) }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch(Dispatchers.IO) {
            loadPurchasesInfo()
            // Перезагружаем пользователя (flow уже обновится)
            kotlinx.coroutines.delay(500)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun setEmail(value: String) {
        currentEmail = value
        _uiState.update { it.copy(resentVerificationEmailButton = value.isEmailValid()) }
    }

    fun emailConfirmation() {
        viewModelScope.launch {
            val parseUser = ParseUser.getCurrentUser()
            parseUser.email = currentEmail
            parseUser.username = currentEmail
            parseUser.saveInBackground()
        }
    }

    fun onUploadToRemote() {
        viewModelScope.launch {
            back4AppManager.synchronizedStorage().collect { result ->
                _uiState.update { it.copy(uploadState = result) }
            }
        }
    }

    fun onDownloadFromRemote() {
        viewModelScope.launch {
            back4AppManager.loadRouteListFromRemote().collect { result ->
                _uiState.update { it.copy(downloadState = result) }
            }
        }
    }

    fun resetUploadState() = _uiState.update { it.copy(uploadState = null) }
    fun resetDownloadState() = _uiState.update { it.copy(downloadState = null) }

    fun logOut() {
        viewModelScope.launch {
            authUseCase.logout().collect { result ->
                if (result is ResultState.Success){
                    remoteRouteUseCase.cancelingSync()
                    // Принудительно обновляем статус логина в MainViewModel
                    getKoin().get<SessionManager>().updateLoggedIn()
                }
                _uiState.update { it.copy(logOutState = result) }
            }
        }
    }

    fun cleanRepo() {
        viewModelScope.launch(Dispatchers.IO) {
            back4AppManager.cleanUpRoutesForAllUsers()
        }
    }
}