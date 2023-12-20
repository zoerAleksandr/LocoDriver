package com.example.login.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ResultState
import com.example.domain.use_cases.AuthUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class LoginViewModel: ViewModel(), KoinComponent {
    private val authUseCase: AuthUseCase by inject()
    private val _loginState = MutableStateFlow<ResultState<Unit>?>(null)
    val loginState: StateFlow<ResultState<Unit>?> = _loginState

    private var loginJob: Job? = null

    fun login() {
        loginJob?.cancel()
        loginJob = authUseCase.login().onEach {
            _loginState.value = it
        }.launchIn(viewModelScope)
    }

    fun resetLoginState() {
        _loginState.value = null
    }
}