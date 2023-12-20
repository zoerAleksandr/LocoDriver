package com.example.locodriver.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import com.example.domain.navigation.Router
import com.example.locodriver.ui.navigation.RouterImpl
import com.google.accompanist.navigation.animation.rememberAnimatedNavController

@ExperimentalAnimationApi
@Composable
fun rememberLocoDriverAppState(
    navController: NavHostController = rememberAnimatedNavController()
): LocoDriverAppState {
    return remember(navController) {
        LocoDriverAppState(RouterImpl(navController), navController)
    }
}

@Stable
data class LocoDriverAppState(
    val router: Router,
    internal val navHostController: NavHostController,
)