package com.z_company.login.navigation

import android.util.Log
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.navigation.NavGraphBuilder
import androidx.navigation.navigation
import com.z_company.domain.navigation.Router
import com.z_company.core.ui.navigation.composableScreen

@ExperimentalAnimationApi
fun NavGraphBuilder.loginGraph(
    router: Router,
    isShowFirstPresentation: Boolean
) {
    val startDestination =
        if (isShowFirstPresentation) {
            FirstPresentationBlockRoute.route
        } else {
            SignInScreenRoute.route
        }

    navigation(
        route = AuthFeature.route,
        startDestination = startDestination,
    ) {
        composableScreen(
            route = SignInScreenRoute.route,
        ) {
            Log.i("NAV", SignInScreenRoute.route)
            SignInDestination(router)
        }
        composableScreen(
            route = RecoveryPasswordRoute.route,
        ) {
            Log.i("NAV", RecoveryPasswordRoute.route)
            PasswordRecoveryDestination(router)
        }
        composableScreen(
            route = LogInScreenRoute.route,
        ) {
            Log.i("NAV", LogInScreenRoute.route)
            LogInDestination(router)
        }
        composableScreen(
            route = FirstPresentationBlockRoute.route
        ){
            FirstPresentationBlockDestination(router = router)
        }
    }
}