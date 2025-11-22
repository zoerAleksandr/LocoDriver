package com.z_company.route.viewmodel.login

import com.z_company.core.ResultState

data class PasswordRecoveryUiState(
    val resultState: ResultState<Unit> = ResultState.Success(Unit),
    val isEnableButton: Boolean = false,
    val isLoading: Boolean = false,
    val requestHasBeenSend: Boolean = false
)