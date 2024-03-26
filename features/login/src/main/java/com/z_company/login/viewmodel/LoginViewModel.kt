package com.z_company.login.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.data_remote.AuthUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class LoginViewModel : ViewModel(),
    KoinComponent {
    private val authUseCase: AuthUseCase by inject()
    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState = _loginState.asStateFlow()

    private var loginJob: Job? = null
    private var userIdPhone: String = ""

    fun requestingSMSCode(number: String) {
        loginJob?.cancel()
        loginJob = viewModelScope.launch {
            try {
                userIdPhone = authUseCase.requestingSMSCode(number)
            } catch (e: Exception) {
                _loginState.update {
                    it.copy(
                        loginState = ResultState.Success(false),
                        errorMessage = e.message
                    )
                }
                e.printStackTrace()
            }
        }
    }

    fun loginWithPhone(secret: String) {
        loginJob?.cancel()
        loginJob = viewModelScope.launch {
            try {
                val session = authUseCase.loginWithPhone(userIdPhone, secret)
                if (session.current) {
                    _loginState.update {
                        it.copy(
                            loginState = ResultState.Success(true),
                            session = session
                        )
                    }
                }
            } catch (e: Exception) {
                _loginState.update {
                    it.copy(
                        loginState = ResultState.Success(false),
                        errorMessage = e.message
                    )
                }
                e.printStackTrace()
            }
        }
    }
}