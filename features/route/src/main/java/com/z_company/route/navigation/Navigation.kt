package com.z_company.route.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.z_company.core.ui.navigation.composableScreen
import com.z_company.domain.navigation.Router

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.homeGraph(
    router: Router
) {
    navigation(
        route = HomeFeature.route,
        startDestination = HomeRoute.route
    ) {
        composableScreen(
            route = HomeRoute.route,
        ) {
            HomeDestination(router = router)
        }

        composableScreen(
            route = FormRoute.route,
            arguments = FormRoute.navArguments
        ) { backStackEntry ->
            FormDestination(router = router, backStackEntry = backStackEntry)
        }

        composableScreen(
            route = FormLoco.route,
            arguments = FormLoco.navArguments,
        ) { backStackEntry ->
            FormLocoDestination(router = router, backStackEntry = backStackEntry)
        }

        composableScreen(
            route = FormTrain.route,
            arguments = FormTrain.navArguments,
        ) { backStackEntry ->
            FormTrainDestination(router = router, backStackEntry = backStackEntry)
        }

        composableScreen(
            route = FormPassenger.route,
            arguments = FormPassenger.navArguments
        ) { backStackEntry ->
            FormPassengerDestination(router = router, backStackEntry = backStackEntry)
        }

        composableScreen(
            route = CreatePhotoRoute.route,
            arguments = CreatePhotoRoute.navArguments
        ) { backStackEntry ->
            CreatePhotoDestination(router = router, backStackEntry = backStackEntry)
        }

        composableScreen(
            route = PreviewPhotoRoute.route,
            arguments = PreviewPhotoRoute.navArguments
        ) { backStackEntry ->
            PreviewPhotoDestination(router = router, backStackEntry = backStackEntry)
        }

        composableScreen(
            route = ViewingImageRoute.route,
            arguments = ViewingImageRoute.navArguments
        ) { navBackStackEntry ->
            ViewingImageDestination(router = router, navBackStackEntry = navBackStackEntry)
        }
        composableScreen(
            route = SearchRoute.route
        ) {
            SearchDestination(router = router)
        }
        composableScreen(
            route = PurchasesRoute.route
        ) {
            PurchasesDestination(router = router)
        }
    }
}