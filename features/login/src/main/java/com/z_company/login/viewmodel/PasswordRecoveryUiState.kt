package com.z_company.login.viewmodel

data class PasswordRecoveryUiState(
    val isEnableButton: Boolean = false,
    val isLoading: Boolean = false,
    val requestHasBeenSend: Boolean = false
)