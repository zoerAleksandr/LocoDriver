package com.z_company.settings.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable
import androidx.navigation.compose.navigation
import com.z_company.core.ui.navigation.composablePopup
import com.z_company.domain.navigation.Router
import com.z_company.settings.ui.SelectReleaseDaysScreen


@ExperimentalAnimationApi
fun NavGraphBuilder.settingsGraph(
    router: Router
) {
    navigation(
        route = SettingsFeature.route,
        startDestination = SettingsScreenRoute.route,
    ) {
        composable(SettingsScreenRoute.route) {
            SettingDestination(router = router)
        }
        composable(SelectReleaseDaysScreenRoute.route) {
            SelectReleaseDaysDestination(router = router)
        }
    }
}