package com.z_company.login.viewmodel

import com.z_company.core.ResultState
import com.z_company.domain.entities.User

data class SignInUiState(
    val userState: ResultState<User?> = ResultState.Success(null),
    val errorMessage: String? = null,
)
