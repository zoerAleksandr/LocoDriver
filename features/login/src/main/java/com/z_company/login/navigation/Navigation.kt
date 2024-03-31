package com.z_company.login.navigation

import android.util.Log
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.navigation.NavGraphBuilder
import androidx.navigation.navigation
import com.z_company.domain.navigation.Router
import com.z_company.core.ui.navigation.composableScreen

@ExperimentalAnimationApi
fun NavGraphBuilder.loginGraph(
    router: Router
) {

    navigation(
        route = LoginFeature.route,
        startDestination = LoginScreenRoute.route,
    ) {
        composableScreen(LoginScreenRoute.route) {
            Log.i("NAV", LoginScreenRoute.route)
            LoginDestination(router)
        }
        composableScreen(RecoveryPasswordRoute.route) {
            Log.i("NAV", RecoveryPasswordRoute.route)
            PasswordRecoveryDestination(router)
        }
    }
}