package com.z_company.login.viewmodel

import com.z_company.core.ResultState

data class LoginUiState(
    val loginState: ResultState<Boolean> = ResultState.Success(false),
    val errorMessage: String? = null,
)
