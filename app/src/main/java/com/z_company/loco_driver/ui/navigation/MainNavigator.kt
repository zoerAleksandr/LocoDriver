package com.z_company.loco_driver.ui.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import com.z_company.loco_driver.ui.LocoDriverAppState
import com.z_company.login.navigation.AuthFeature
import com.z_company.login.navigation.loginGraph
import com.z_company.route.navigation.HomeFeature
import com.z_company.route.navigation.homeGraph
import com.z_company.settings.navigation.settingsGraph
import com.google.accompanist.navigation.animation.AnimatedNavHost

@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@ExperimentalAnimationApi
@Composable
fun MainNavigation(
    appState: LocoDriverAppState,
    isLoggedIn: Boolean,
) {
    val initialRoute = if (isLoggedIn) HomeFeature.route else AuthFeature.route

    AnimatedNavHost(
        appState.navHostController,
        startDestination = initialRoute
    ) {
        loginGraph(appState.router)
        homeGraph(appState.router)
        settingsGraph(appState.router)
    }
}