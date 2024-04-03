package com.z_company.login.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.util.isEmailValid
import com.z_company.data_remote.AuthUseCase
import kotlinx.coroutines.Job
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
    private var registeredJob: Job? = null
    fun registeredUser(name: String, password: String, email: String) {
        viewModelScope.launch {
            registeredJob?.cancel()
            registeredJob =
                authUseCase.registeredUserByEmail(name, password, email).onEach { resultState ->
                    _uiState.update {
                        it.copy(
                            userState = resultState
                        )
                    }
                }.launchIn(viewModelScope)
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
            if (password.length >= 3 && password == confirm) {
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