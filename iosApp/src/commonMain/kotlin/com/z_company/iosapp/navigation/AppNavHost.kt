package com.z_company.iosapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.savedstate.read
import com.z_company.iosapp.screen.AllRouteScreen
import com.z_company.iosapp.screen.FormLocoScreen
import com.z_company.iosapp.screen.FormPassengerScreen
import com.z_company.iosapp.screen.FormScreen
import com.z_company.iosapp.screen.FormTrainScreen
import com.z_company.iosapp.screen.HomeScreen
import com.z_company.iosapp.screen.MoreInfoScreen
import com.z_company.iosapp.screen.ProfileScreen
import com.z_company.iosapp.screen.SalaryCalculationScreen
import com.z_company.iosapp.screen.SearchScreen
import com.z_company.iosapp.screen.SelectReleaseDaysScreen
import com.z_company.iosapp.screen.SettingSalaryScreen
import com.z_company.iosapp.screen.SettingsScreen
import com.z_company.iosapp.screen.PurchasesScreen
import com.z_company.iosapp.screen.WorkScheduleScreen

/**
 * Корневой NavHost iOS-приложения.
 *
 * Маршруты совпадают со строками в features/route/navigation/Routes.kt,
 * чтобы при миграции features на Compose Multiplatform стабы заменялись
 * реальными экранами без изменения навигационного графа.
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
        // FormRoute: "FormRoute?routeId={routeId}/?makeCopy={makeCopy}"
        composable(FormRoute.route) { backStackEntry ->
            val routeId = backStackEntry.arguments?.read {
                if (contains("routeId")) getString("routeId") else null
            }
            FormScreen(router = router, routeId = routeId)
        }
        composable(FormLoco.route) { backStackEntry ->
            val basicId = backStackEntry.arguments?.read {
                if (contains("basicId")) getString("basicId") else null
            } ?: ""
            val locoId = backStackEntry.arguments?.read {
                if (contains("locoId")) getString("locoId") else null
            }
            FormLocoScreen(router = router, basicId = basicId, locoId = locoId)
        }
        composable(FormTrain.route) { backStackEntry ->
            val basicId = backStackEntry.arguments?.read {
                if (contains("basicId")) getString("basicId") else null
            } ?: ""
            val trainId = backStackEntry.arguments?.read {
                if (contains("trainId")) getString("trainId") else null
            }
            FormTrainScreen(router = router, basicId = basicId, trainId = trainId)
        }
        composable(FormPassenger.route) { backStackEntry ->
            val basicId = backStackEntry.arguments?.read {
                if (contains("basicId")) getString("basicId") else null
            } ?: ""
            val passengerId = backStackEntry.arguments?.read {
                if (contains("passengerId")) getString("passengerId") else null
            }
            FormPassengerScreen(router = router, basicId = basicId, passengerId = passengerId)
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
            SettingSalaryScreen(router = router)
        }
        composable(SearchRoute.route) {
            SearchScreen(router = router)
        }
        composable(PurchasesRoute.route) {
            PurchasesScreen(router = router)
        }
        composable(AllRouteScreenRoute.route) {
            AllRouteScreen(router = router)
        }
        composable(WorkScheduleScreenRoute.route) {
            WorkScheduleScreen(router = router)
        }
        composable(SelectReleaseDaysScreenRoute.route) {
            SelectReleaseDaysScreen(router = router)
        }
        composable(MoreInfoRoute.route) { backStackEntry ->
            val monthId = backStackEntry.arguments?.read {
                if (contains("monthId")) getString("monthId") else null
            }
            MoreInfoScreen(router = router, monthId = monthId)
        }
    }
}
