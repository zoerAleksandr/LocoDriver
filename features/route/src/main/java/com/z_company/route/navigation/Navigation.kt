package com.z_company.route.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.navigation
import com.z_company.core.ui.navigation.composableScreen
import com.z_company.domain.navigation.Router

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.homeGraph(
    router: Router,
) {
    navigation(
        route = HomeFeature.route,
        startDestination = HomeRoute.route
    ) {
        // главное меню
        composableScreen(
            route = HomeRoute.route,
        ) {
            HomeDestination(router = router)
        }

        // маршруты
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

        // настройки и профиль
        composableScreen(SettingsScreenRoute.route) {
            SettingDestination(router = router)
        }
        composableScreen(ProfileRoute.route) {
            ProfileDestination(router = router)
        }
        composableScreen(SettingSalaryRoute.route) {
            SettingSalaryDestination(router = router)
        }

        // зарплата и статистика
        composableScreen(SalaryCalculationRoute.route) {
            SalaryCalculationDestination(router = router)
        }
        composableScreen(
            route = MoreInfoRoute.route,
            arguments = MoreInfoRoute.navArguments,
        ) { backStackEntry ->
            MoreInfoDestination(router = router, backStackEntry = backStackEntry)
        }

        // поиск, покупки, расписание
        composableScreen(SearchRoute.route) {
            SearchDestination(router = router)
        }
        composableScreen(PurchasesRoute.route) {
            PurchasesDestination(router = router)
        }
        composableScreen(AllRouteScreenRoute.route) {
            AllRouteScreenDestination(router = router)
        }
        composableScreen(WorkScheduleScreenRoute.route) {
            WorkScheduleDestination(router = router)
        }
        composableScreen(SelectReleaseDaysScreenRoute.route) {
            SelectReleaseDaysDestination(router = router)
        }
        composableScreen(UpdatePresentationBlockRoute.route) {
            UpdatePresentationBlockDestination(router = router)
        }
    }
}