package com.z_company.login.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.z_company.domain.navigation.Router
import com.z_company.login.ui.LoginScreen
import com.z_company.login.viewmodel.LoginViewModel
import org.koin.androidx.compose.getViewModel

@Composable
fun LoginDestination(
    router: Router
){
    val viewModel = getViewModel<LoginViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    LoginScreen(
        userState = uiState.userState,
        onLoginSuccess = router::showHome,
        registeredUser = viewModel::registeredUser,
        logInUser = viewModel::loginUser,
        onPasswordRecovery = router::showRecoveryPassword,
        resetErrorState = viewModel::resetErrorState
    )
}