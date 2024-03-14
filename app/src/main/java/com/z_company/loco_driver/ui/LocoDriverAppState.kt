package com.z_company.loco_driver.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import com.z_company.domain.navigation.Router
import com.z_company.loco_driver.ui.navigation.RouterImpl
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