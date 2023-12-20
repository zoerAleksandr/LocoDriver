package com.example.login.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.navigation
import com.example.domain.navigation.Router
import com.example.login.ui.LoginScreen
import com.example.login.viewmodel.LoginViewModel
import com.google.accompanist.navigation.animation.composable

@ExperimentalAnimationApi
fun NavGraphBuilder.loginGraph(
    router: Router
) {
    navigation(
        route = LoginFeature.route,
        startDestination = LoginScreenRoute.route,
    ) {
        composable(LoginScreenRoute.route) {
            val viewModel: LoginViewModel = viewModel()
            val loginState by viewModel.loginState.collectAsState()
            LoginScreen(
                loginState = loginState,
                onLoginSuccess = router::showHome,
                resetLoginState = viewModel::resetLoginState,
                onLoginClick = viewModel::login
            )
        }
    }
}