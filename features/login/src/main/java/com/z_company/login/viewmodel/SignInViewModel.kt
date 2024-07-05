package com.z_company.login.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.core.util.isEmailValid
import com.z_company.use_case.AuthUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val MIN_LENGTH_PASSWORD = 4
class SignInViewModel : ViewModel(),
    KoinComponent {
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

    private var loginJob: Job? = null

    fun signInUser(username: String, password: String) {
        val usernameWithoutWhitespace = username.filterNot { it.isWhitespace() }
        val passwordWithoutWhitespace = password.filterNot { it.isWhitespace() }
        loginJob?.cancel()
        loginJob = authUseCase.loginWithEmail(usernameWithoutWhitespace, passwordWithoutWhitespace)
            .onEach { result ->
                _uiState.update {
                    it.copy(
                        userState = result
                    )
                }
            }.launchIn(viewModelScope)
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