package com.z_company.iosapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.z_company.iosapp.screen.FormScreen
import com.z_company.iosapp.screen.HomeScreen
import com.z_company.iosapp.screen.ProfileScreen
import com.z_company.iosapp.screen.SalaryCalculationScreen
import com.z_company.iosapp.screen.SettingsScreen
import com.z_company.iosapp.screen.StubScreen

/**
 * Корневой NavHost iOS-приложения.
 *
 * Маршруты совпадают со строками в features/route/navigation/Routes.kt,
 * чтобы при миграции features на Compose Multiplatform стабы заменялись
 * реальными экранами без изменения навигационного графа.
 *
 * Текущий статус экранов:
 *   HomeScreen              — стаб (список маршрутов)
 *   FormScreen              — стаб (форма маршрута)
 *   FormLocoScreen          — стаб
 *   FormTrainScreen         — стаб
 *   FormPassengerScreen     — стаб
 *   SettingsScreen          — стаб
 *   ProfileScreen           — стаб
 *   SalaryCalculationScreen — стаб
 *   остальные               — StubScreen (generic placeholder)
 */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val router = remember { IosRouterImpl() }

    LaunchedEffect(navController) {
        router.navController = navController
    }

    NavHost(
        navController = navController,
        startDestination = HomeRoute.route,
    ) {
        composable(HomeRoute.route) {
            HomeScreen(router = router)
        }
        composable(FormRoute.route) {
            FormScreen(router = router)
        }
        composable(FormLoco.route) {
            StubScreen(title = "Форма локомотива", router = router)
        }
        composable(FormTrain.route) {
            StubScreen(title = "Форма поезда", router = router)
        }
        composable(FormPassenger.route) {
            StubScreen(title = "Форма пассажирского", router = router)
        }
        composable(SettingsScreenRoute.route) {
            SettingsScreen(router = router)
        }
        composable(ProfileRoute.route) {
            ProfileScreen(router = router)
        }
        composable(SalaryCalculationRoute.route) {
            SalaryCalculationScreen(router = router)
        }
        composable(SettingSalaryRoute.route) {
            StubScreen(title = "Настройка зарплаты", router = router)
        }
        composable(SearchRoute.route) {
            StubScreen(title = "Поиск", router = router)
        }
        composable(PurchasesRoute.route) {
            StubScreen(title = "Покупки", router = router)
        }
        composable(AllRouteScreenRoute.route) {
            StubScreen(title = "Все маршруты", router = router)
        }
        composable(WorkScheduleScreenRoute.route) {
            StubScreen(title = "График работы", router = router)
        }
        composable(SelectReleaseDaysScreenRoute.route) {
            StubScreen(title = "Дни отдыха", router = router)
        }
        composable(MoreInfoRoute.route) {
            StubScreen(title = "Подробнее", router = router)
        }
    }
}
