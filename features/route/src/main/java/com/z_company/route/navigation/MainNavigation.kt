package com.z_company.route.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.z_company.core.ui.navigation.composablePopup
import com.z_company.core.ui.navigation.composableScreen
import com.z_company.domain.navigation.Router

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainNavigation(router: Router, navController: NavHostController) {
    NavHost(navController, startDestination = NavigationItem.Home.route.route) {
        composablePopup (
            route = HomeRoute.route,
        ) {
            HomeDestination(router = router)
        }
        composablePopup(
            route = SalaryCalculationRoute.route
        ) {
            SalaryCalculationDestination(router = router)
        }
        composablePopup(
            route = FormRoute.route,
            arguments = FormRoute.navArguments
        ) { backStackEntry ->
            FormDestination(router = router, backStackEntry = backStackEntry)
        }
        composablePopup(SettingsScreenRoute.route) {
            SettingDestination(router = router)
        }

        composablePopup(ProfileRoute.route) {
//            ProfileScreen()
        }
    }
}