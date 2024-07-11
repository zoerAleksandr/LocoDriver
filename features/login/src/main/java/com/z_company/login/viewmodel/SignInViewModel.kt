package com.z_company.login.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.core.util.isEmailValid
import com.z_company.login.ui.getMessageThrowable
import com.z_company.use_case.AuthUseCase
import com.z_company.use_case.RemoteRouteUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

const val MIN_LENGTH_PASSWORD = 4
const val TIME_OUT: Long = 15_000L

class SignInViewModel : ViewModel(), KoinComponent {
    private val remoteRouteUseCase: RemoteRouteUseCase by inject()
    private val authUseCase: AuthUseCase by inject()
    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState = _uiState.asStateFlow()

    var email by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set

    fun setEmailValue(value: String) {
        email = value
        checkField()
    }

    fun setPasswordValue(value: String) {
        password = value
        checkField()
    }

    private var parentLoginJob: Job? = null
    private var loginWithEmailJob: Job? = null

    fun signInUser(username: String, password: String) {
        parentLoginJob = viewModelScope.launch {
            val usernameWithoutWhitespace = username.filterNot { it.isWhitespace() }
            val passwordWithoutWhitespace = password.filterNot { it.isWhitespace() }
            loginWithEmailJob?.cancel()
            loginWithEmailJob =
                authUseCase.loginWithEmail(usernameWithoutWhitespace, passwordWithoutWhitespace)
                    .onEach { result ->
                        if (result is ResultState.Error) {
                            val messageThrowable = getMessageThrowable(result.entity.throwable)
                            _uiState.update {
                                it.copy(
                                    userState = result.copy(
                                        entity = ErrorEntity(
                                            Throwable(
                                                message = messageThrowable
                                            ),
                                        )
                                    )
                                )
                            }
                        } else {
                            if (result is ResultState.Success) {
                                loadDataFromRemote()
                            }
                            _uiState.update {
                                it.copy(
                                    userState = result
                                )
                            }
                        }
                    }.launchIn(viewModelScope)
            delay(TIME_OUT)
            _uiState.update {
                it.copy(
                    userState = ResultState.Error(entity = ErrorEntity(Throwable(message = "Слабый сигнал! Проверьте интернет соединение.")))
                )
            }
            loginWithEmailJob?.cancel()
        }
    }

    private fun loadDataFromRemote() {
        viewModelScope.launch {
            remoteRouteUseCase.loadingRoutesFromRemote()
        }
    }

    fun cancelRegistered() {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(ResultState.Success(null))
            }
            parentLoginJob?.cancel()
        }
    }

    fun showErrorConfirmed() {
        _uiState.update {
            it.copy(
                userState = ResultState.Success(null)
            )
        }
    }

    private fun isEmailValid(): Boolean {
        return email.isEmailValid()
    }

    private fun isPasswordValid(): Boolean {
        return password.length >= MIN_LENGTH_PASSWORD
    }

    private fun checkField() {
        if (isEmailValid() && isPasswordValid()) {
            _uiState.update {
                it.copy(isEnableButton = true)
            }
        } else {
            _uiState.update {
                it.copy(isEnableButton = false)
            }
        }
    }
}