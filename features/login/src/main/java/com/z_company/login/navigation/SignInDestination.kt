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
        onSignInSuccess = router::showHome,
        onRegisteredClick = router::showLogIn,
        logInUser = viewModel::loginUser,
        onPasswordRecovery = router::showRecoveryPassword,
    )
}