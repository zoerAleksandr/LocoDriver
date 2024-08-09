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

class LogInViewModel : ViewModel(), KoinComponent {
    private val authUseCase: AuthUseCase by inject()
    private val _uiState = MutableStateFlow(LogInUiState())
    val uiState = _uiState.asStateFlow()
    private var parentRegisteredJob: Job? = null
    private var registeredUserByEmailJob: Job? = null
    fun registeredUser(name: String, password: String, email: String) {
        parentRegisteredJob = viewModelScope.launch {
            registeredUserByEmailJob?.cancel()
            registeredUserByEmailJob =
                authUseCase.registeredUserByEmail(name, password, email).onEach { resultState ->
                    if (resultState is ResultState.Error) {
                        val messageThrowable = getMessageThrowable(resultState.entity.throwable)
                        _uiState.update {
                            it.copy(
                                userState = resultState.copy(
                                    entity = ErrorEntity(
                                        Throwable(
                                            message = messageThrowable
                                        ),
                                    )
                                )
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                userState = resultState
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
            registeredUserByEmailJob?.cancel()
        }
    }

    fun cancelRegistered() {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(ResultState.Success(null))
            }
            parentRegisteredJob?.cancel()
        }
    }

    var email: String by mutableStateOf("")
    var password: String by mutableStateOf("")
    var confirm: String by mutableStateOf("")

    fun setEmailData(value: String) {
        val valueWithoutWhitespace = value.filterNot { it.isWhitespace() }
        email = valueWithoutWhitespace
        checkingCorrectPassword()
    }

    fun setPasswordData(value: String) {
        val valueWithoutWhitespace = value.filterNot { it.isWhitespace() }
        password = valueWithoutWhitespace
        checkingCorrectPassword()
    }

    fun setConfirmData(value: String) {
        val valueWithoutWhitespace = value.filterNot { it.isWhitespace() }
        confirm = valueWithoutWhitespace
        checkingCorrectPassword()
    }


    private fun checkingCorrectPassword() {
        if (email.isEmailValid()) {
            if (password.length >= MIN_LENGTH_PASSWORD && password == confirm) {
                _uiState.update {
                    it.copy(
                        isEnableButton = true
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isEnableButton = false
                    )
                }
            }
        } else {
            _uiState.update {
                it.copy(
                    isEnableButton = false
                )
            }
        }
    }
}