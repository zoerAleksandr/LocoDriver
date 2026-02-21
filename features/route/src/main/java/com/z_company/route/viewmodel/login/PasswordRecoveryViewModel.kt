package com.z_company.route.viewmodel.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.core.util.isEmailValid
import com.z_company.repository.remote_rest.AuthManager
import com.z_company.repository.remote_rest.ResponseState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PasswordRecoveryViewModel : ViewModel(), KoinComponent {
    private val authManager: AuthManager by inject()

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
            authManager.forgotPassword(emailWithoutWhitespace).collect { state ->
                when (state) {
                    is ResponseState.Success -> {
                        _uiState.update { s ->
                            s.copy(
                                resultState = ResultState.Success(Unit),
                                requestHasBeenSend = true
                            )
                        }
                    }
                    is ResponseState.Error -> {
                        _uiState.update { s ->
                            s.copy(
                                resultState = ResultState.Error(
                                    entity = ErrorEntity(
                                        throwable = Throwable(message = state.errorMessage)
                                    )
                                )
                            )
                        }
                    }
                    else -> {}
                }
            }
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
                it.copy(isEnableButton = true)
            }
        } else {
            _uiState.update {
                it.copy(isEnableButton = false)
            }
        }
    }
}
