package com.example.locodriver.ui.navigation

import androidx.navigation.NavHostController
import com.example.domain.entities.route.Route
import com.example.domain.navigation.Router
import com.example.login.navigation.LoginFeature
import com.example.login.navigation.LoginScreenRoute
import com.example.route.navigation.DetailsRoute
import com.example.route.navigation.FormRoute
import com.example.route.navigation.HomeRoute
import com.example.settings.navigation.SettingsFeature

class RouterImpl(
    private val navController: NavHostController
) : Router {
    override fun showLogin() {
        navController.navigate(LoginScreenRoute.route) {
            popUpTo(0)
        }
    }

    override fun showHome() {
        navController.navigate(HomeRoute.route) {
            popUpTo(LoginFeature.route) {
                inclusive = true
                saveState = false
            }
        }
    }

    override fun showRouteForm(route: Route?) {
        navController.navigate(
            FormRoute.buildDetailsRoute(route?.id)
        )
    }

    override fun showRouteDetails(route: Route) {
        navController.navigate(
            DetailsRoute.buildDetailsRoute(route.id)
        )
    }

    override fun showSettings() {
        navController.navigate(SettingsFeature.route)
    }

    override fun back() {
        navController.popBackStack()
    }

    override fun navigationUp(): Boolean {
        return navController.navigateUp()
    }
}