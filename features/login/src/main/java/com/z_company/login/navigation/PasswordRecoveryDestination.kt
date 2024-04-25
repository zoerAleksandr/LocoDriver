package com.z_company.login.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.z_company.domain.navigation.Router
import com.z_company.login.ui.PasswordRecoveryScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.z_company.login.viewmodel.PasswordRecoveryViewModel


@Composable
fun PasswordRecoveryDestination(
    router: Router
) {
    val viewModel: PasswordRecoveryViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    PasswordRecoveryScreen(
        onBack = router::back,
        requestPasswordReset = viewModel::requestPasswordReset,
        isLoading = uiState.isLoading,
        isEnableButton = uiState.isEnableButton,
        requestHasBeenSend = uiState.requestHasBeenSend,
        isEmailValid = viewModel::isEmailValid
    )
}