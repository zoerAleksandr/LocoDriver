package com.z_company.loco_driver.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import com.z_company.domain.navigation.Router
import com.z_company.loco_driver.ui.navigation.RouterImpl

@ExperimentalAnimationApi
@Composable
fun rememberLocoDriverAppState(): LocoDriverAppState {
    val navController = rememberNavController()
    val router = remember(navController) { RouterImpl() }
    return remember { LocoDriverAppState(router) }
}

@Stable
data class LocoDriverAppState(
    val router: Router,
)