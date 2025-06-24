package com.z_company.login.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parse.ParseUser
import com.parse.RequestPasswordResetCallback
import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.core.util.isEmailValid
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PasswordRecoveryViewModel : ViewModel() {
    private var _uiState = MutableStateFlow(PasswordRecoveryUiState())
    val uiState = _uiState.asStateFlow()

    private var requestJob: Job? = null
    fun requestPasswordReset(email: String) {
        val emailWithoutWhitespace = email.filterNot { it.isWhitespace() }
        _uiState.update {
            it.copy(resultState = ResultState.Loading())
        }
        requestJob?.cancel()
        requestJob = viewModelScope.launch {
            ParseUser.requestPasswordResetInBackground(emailWithoutWhitespace) { e ->
                if (e == null) {
                    _uiState.update { state ->
                        state.copy(
                            resultState = ResultState.Success(Unit),
                            requestHasBeenSend = true
                        )
                    }
                } else {
                    _uiState.update { state ->
                        state.copy(
                            resultState = ResultState.Error(
                                entity = ErrorEntity(
                                    throwable = Throwable(
                                        message = e.message
                                    )
                                )
                            )
                        )
                    }
                }
            }

            delay(TIME_OUT)
            _uiState.update {
                it.copy(
                    resultState = ResultState.Error(entity = ErrorEntity(Throwable(message = "Слабый сигнал! Проверьте интернет соединение.")))
                )
            }
            requestJob?.cancelAndJoin()
        }
    }

    fun cancelRequest() {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(resultState = ResultState.Success(Unit))
            }
            requestJob?.cancel()
        }
    }

    fun isEmailValid(email: String) {
        if (email.isEmailValid()) {
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
    }
}