package com.z_company.login.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parse.ParseUser
import com.z_company.core.ResultState
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

class LoginViewModel : ViewModel(),
    KoinComponent {
    private val authUseCase: AuthUseCase by inject()
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    private var loginJob: Job? = null
    private var registeredJob: Job? = null

    fun registeredUser(name: String, password: String, email: String) {
        registeredJob?.cancel()
        registeredJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    userState = ResultState.Loading
                )
            }
            val user = ParseUser()
            user.username = name
            user.setPassword(password)
            user.email = email
            user.signUpInBackground { parseException ->

            }
        }
    }

    fun loginUser(username: String, password: String) {
        loginJob?.cancel()
        loginJob = authUseCase.loginWithEmail(username, password).onEach { result ->
            _uiState.update {
                it.copy(
                    userState = result
                )
            }
        }.launchIn(viewModelScope)
    }

    fun resetErrorState() {
        _uiState.update {
            it.copy(
                errorMessage = null
            )
        }
    }
}