package com.z_company.login.viewmodel

import com.z_company.core.ResultState
import io.appwrite.models.Session

data class LoginUiState(
    val loginState: ResultState<Boolean> = ResultState.Success(false),
    val errorMessage: String? = null,
    val session: Session? = null
)
