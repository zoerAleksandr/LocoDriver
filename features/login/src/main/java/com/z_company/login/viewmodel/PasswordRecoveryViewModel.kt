package com.z_company.login.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parse.ParseUser
import com.z_company.core.util.isEmailValid
import com.z_company.login.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PasswordRecoveryViewModel : ViewModel() {
    private val timeout = R.dimen.timeout_connect.toLong()
    private var _uiState = MutableStateFlow(PasswordRecoveryUiState())
    val uiState = _uiState.asStateFlow()

    private var requestJob: Job? = null
    fun requestPasswordReset(email: String) {
        _uiState.update {
            it.copy(isLoading = true)
        }
        requestJob?.cancel()
        requestJob = viewModelScope.launch {
            ParseUser.requestPasswordResetInBackground(email)
                .continueWith {
                    if (it.isCompleted) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                requestHasBeenSend = true
                            )
                        }
                    }
                }
            delay(timeout)
            cancel()
            _uiState.update {
                it.copy(
                    isLoading = false
                )
            }
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