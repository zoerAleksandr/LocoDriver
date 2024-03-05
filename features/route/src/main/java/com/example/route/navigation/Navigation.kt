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
    ) {
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
        ) { backStackEntry ->
            DetailsDestination(router = router, backStackEntry = backStackEntry)
        }

        composablePopup(
            route = FormRoute.route,
            arguments = FormRoute.navArguments
        ) { backStackEntry ->
            FormDestination(router = router, backStackEntry = backStackEntry)
        }

        composablePopup(
            route = FormLoco.route,
            arguments = FormLoco.navArguments,
        ) { backStackEntry ->
            FormLocoDestination(router = router, backStackEntry = backStackEntry)
        }

        composablePopup(
            route = FormTrain.route,
            arguments = FormTrain.navArguments,
        ) { backStackEntry ->
            FormTrainDestination(router = router, backStackEntry = backStackEntry)
        }

        composablePopup(
            route = FormPassenger.route,
            arguments = FormPassenger.navArguments
        ) { backStackEntry ->
            FormPassengerDestination(router = router, backStackEntry = backStackEntry)
        }

        composablePopup(
            route = FormNotes.route,
            arguments = FormNotes.navArguments
        ) {backStackEntry ->
            FormNotesDestination(router = router, backStackEntry = backStackEntry)
        }

        composablePopup(
            route = CreatePhotoRoute.route,
            arguments = CreatePhotoRoute.navArguments
        ) {backStackEntry ->
            CreatePhotoDestination(router = router, backStackEntry = backStackEntry)
        }
    }
}