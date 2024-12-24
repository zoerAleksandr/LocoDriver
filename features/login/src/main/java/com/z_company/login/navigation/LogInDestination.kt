package com.z_company.login.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.z_company.domain.navigation.Router
import com.z_company.login.ui.LogInScreen
import com.z_company.login.viewmodel.LogInViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun LogInDestination(
    router: Router
) {
    val viewModel = koinViewModel<LogInViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    LogInScreen(
        userState = uiState.userState,
        isEnableButton = uiState.isEnableButton,
        onLogInSuccess = router::showStartScreen,
        onRegisteredClick = viewModel::registeredUser,
        onBack = router::back,
        email = viewModel.email,
        setEmail = viewModel::setEmailData,
        password = viewModel.password,
        setPassword = viewModel::setPasswordData,
        confirm = viewModel.confirm,
        setConfirm = viewModel::setConfirmData,
        cancelRegistered = viewModel::cancelRegistered
    )

}