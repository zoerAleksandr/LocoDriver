package com.z_company.route.navigation.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.z_company.domain.navigation.Router
import androidx.lifecycle.viewmodel.compose.viewModel
import com.z_company.route.ui.login.PasswordRecoveryScreen
import com.z_company.route.viewmodel.login.PasswordRecoveryViewModel


@Composable
fun PasswordRecoveryDestination(
    router: Router
) {
    val viewModel: PasswordRecoveryViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    PasswordRecoveryScreen(
        resultState = uiState.resultState,
        onBack = router::back,
        requestPasswordReset = viewModel::requestPasswordReset,
        isEnableButton = uiState.isEnableButton,
        requestHasBeenSend = uiState.requestHasBeenSend,
        isEmailValid = viewModel::isEmailValid,
        cancelRequest = viewModel::cancelRequest
    )
}