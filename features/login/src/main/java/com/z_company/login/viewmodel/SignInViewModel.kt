package com.z_company.login.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.data_remote.AuthUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SignInViewModel : ViewModel(),
    KoinComponent {
    private val authUseCase: AuthUseCase by inject()
    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState = _uiState.asStateFlow()

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
}