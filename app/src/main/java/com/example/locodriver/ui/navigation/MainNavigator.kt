package com.example.locodriver.ui.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import com.example.locodriver.ui.LocoDriverAppState
import com.example.login.navigation.LoginFeature
import com.example.login.navigation.loginGraph
import com.example.route.navigation.HomeFeature
import com.example.route.navigation.homeGraph
import com.example.settings.navigation.settingsGraph
import com.google.accompanist.navigation.animation.AnimatedNavHost

@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@ExperimentalAnimationApi
@Composable
fun MainNavigation(
    appState: LocoDriverAppState,
    isLoggedIn: Boolean,
) {
    val initialRoute =
        if (isLoggedIn) HomeFeature.route else LoginFeature.route

    AnimatedNavHost(
        appState.navHostController,
        startDestination = initialRoute
    ) {
        loginGraph(appState.router)
        homeGraph(appState.router)
        settingsGraph(appState.router)
    }
}