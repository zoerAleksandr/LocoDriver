package com.z_company.loco_driver.ui.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import com.z_company.loco_driver.ui.LocoDriverAppState
import com.z_company.login.navigation.AuthFeature
import com.z_company.login.navigation.loginGraph
import com.z_company.route.navigation.HomeFeature
import com.z_company.route.navigation.homeGraph
import com.z_company.settings.navigation.settingsGraph
import androidx.navigation.compose.NavHost
@ExperimentalAnimationApi
@Composable
fun MainNavigation(
    appState: LocoDriverAppState,
    isLoggedIn: Boolean,
    isShowFirstPresentation: Boolean,
    isShowUpdatePresentation: Boolean
) {
    val initialRoute = if (isLoggedIn) HomeFeature.route else AuthFeature.route

    NavHost(
        appState.navHostController,
        startDestination = initialRoute
    ) {
        loginGraph(router = appState.router, isShowFirstPresentation = isShowFirstPresentation)
        homeGraph(router = appState.router, isShowUpdatePresentation = isShowUpdatePresentation)
        settingsGraph(appState.router)
    }
}