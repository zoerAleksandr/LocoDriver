package com.z_company.login.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.z_company.domain.navigation.Router
import com.z_company.login.ui.SignInScreen
import com.z_company.login.viewmodel.SignInViewModel
import org.koin.androidx.compose.getViewModel

@Composable
fun SignInDestination(
    router: Router
){
    val viewModel = getViewModel<SignInViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    SignInScreen(
        userState = uiState.userState,
        email = viewModel.email,
        password = viewModel.password,
        setEmail = viewModel::setEmailValue,
        setPassword = viewModel::setPasswordValue,
        onSignInSuccess = router::showHome,
        onRegisteredClick = router::showLogIn,
        logInUser = viewModel::signInUser,
        onPasswordRecovery = router::showRecoveryPassword,
        showErrorConfirmed = viewModel::showErrorConfirmed,
        isEnableButtonSignIn = uiState.isEnableButton
    )
}