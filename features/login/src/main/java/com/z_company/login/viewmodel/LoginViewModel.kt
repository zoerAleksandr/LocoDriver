package com.z_company.login.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parse.ParseException
import com.parse.ParseUser
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
    val uiState = _loginState.asStateFlow()

    private var loginJob: Job? = null
    private var registeredJob: Job? = null

    fun registeredUser(name: String, password: String, email: String) {
        registeredJob?.cancel()
        registeredJob = viewModelScope.launch {
            _loginState.update {
                it.copy(
                    loginState = ResultState.Loading
                )
            }
            val user = ParseUser()
            user.username = name
            user.setPassword(password)
            user.email = email
            user.signUpInBackground { parseException ->
                if (parseException == null) {
                    _loginState.update {
                        it.copy(
                            loginState = ResultState.Success(true)
                        )
                    }
                } else {
                    _loginState.update {
                        it.copy(
                            loginState = ResultState.Success(false),
                            errorMessage = parseException.message
                        )
                    }
                    ParseUser.logOut()
                }
            }
        }
    }

    fun loginUser(username: String, password: String) {
        loginJob?.cancel()
        loginJob = viewModelScope.launch {
            _loginState.update {
                it.copy(
                    loginState = ResultState.Loading
                )
            }
            ParseUser.logInInBackground(
                username,
                password
            ) { parseUser: ParseUser?, parseException: ParseException? ->
                if (parseUser != null) {
                    _loginState.update {
                        it.copy(
                            loginState = ResultState.Success(true)
                        )
                    }
                } else {
                    ParseUser.logOut()
                    _loginState.update {
                        it.copy(
                            loginState = ResultState.Success(false),
                            errorMessage = parseException?.message
                        )
                    }
                }
            }
        }
    }

    fun resetErrorState(){
        _loginState.update {
            it.copy(
                errorMessage = null
            )
        }
    }
}