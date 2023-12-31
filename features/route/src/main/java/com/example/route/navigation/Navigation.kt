package com.example.route.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.navigation
import com.example.core.ui.navigation.composablePopup
import com.example.core.ui.navigation.composableScreen
import com.example.domain.navigation.Router

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.homeGraph(
    router: Router
) {
    val popupScreen = { route: String? ->
        route == FormRoute.route
    }

    navigation(
        route = HomeFeature.route,
        startDestination = HomeRoute.route
    ){
        composableScreen(
            route = HomeRoute.route,
            targetIsPopup = popupScreen
        ) {
            HomeDestination(router = router)
        }

        composableScreen(
            route = DetailsRoute.route,
            arguments = DetailsRoute.navArguments,
            targetIsPopup = popupScreen
        ){backStackEntry ->
            DetailsDestination(router = router, backStackEntry = backStackEntry)
        }

        composablePopup(
            route = FormRoute.route,
            arguments = FormRoute.navArguments
        ){backStackEntry ->
            FormDestination(router = router, backStackEntry = backStackEntry)
        }
    }
}