package com.z_company.iosapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.savedstate.read
import com.z_company.iosapp.screen.AllRouteScreen
import com.z_company.iosapp.screen.FormScreen
import com.z_company.iosapp.screen.HomeScreen
import com.z_company.iosapp.screen.ProfileScreen
import com.z_company.iosapp.screen.PurchasesScreen

// Shared screens from :features:shared
import com.z_company.shared.ui.screen.FormLocoScreen as SharedFormLocoScreen
import com.z_company.shared.ui.screen.FormPassengerScreen as SharedFormPassengerScreen
import com.z_company.shared.ui.screen.FormTrainScreen as SharedFormTrainScreen
import com.z_company.shared.ui.screen.MoreInfoScreen as SharedMoreInfoScreen
import com.z_company.shared.ui.screen.SalaryCalculationScreen as SharedSalaryCalculationScreen
import com.z_company.shared.ui.screen.SearchScreen as SharedSearchScreen
import com.z_company.shared.ui.screen.SelectReleaseDaysScreen as SharedSelectReleaseDaysScreen
import com.z_company.shared.ui.screen.SettingSalaryScreen as SharedSettingSalaryScreen
import com.z_company.shared.ui.screen.SettingsScreen as SharedSettingsScreen
import com.z_company.shared.ui.screen.WorkScheduleScreen as SharedWorkScheduleScreen

/**
 * Корневой NavHost iOS-приложения.
 *
 * Маршруты совпадают со строками в features/route/navigation/Routes.kt,
 * чтобы при миграции features на Compose Multiplatform стабы заменялись
 * реальными экранами без изменения навигационного графа.
 *
 * Экраны, мигрированные в :features:shared, теперь используются напрямую.
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
            SharedFormLocoScreen(locoId = locoId, basicId = basicId, onBackClick = { router.back() })
        }
        composable(FormTrain.route) { backStackEntry ->
            val basicId = backStackEntry.arguments?.read {
                if (contains("basicId")) getString("basicId") else null
            } ?: ""
            val trainId = backStackEntry.arguments?.read {
                if (contains("trainId")) getString("trainId") else null
            }
            SharedFormTrainScreen(trainId = trainId, basicId = basicId, onBackClick = { router.back() })
        }
        composable(FormPassenger.route) { backStackEntry ->
            val basicId = backStackEntry.arguments?.read {
                if (contains("basicId")) getString("basicId") else null
            } ?: ""
            val passengerId = backStackEntry.arguments?.read {
                if (contains("passengerId")) getString("passengerId") else null
            }
            SharedFormPassengerScreen(passengerId = passengerId, basicId = basicId, onBackClick = { router.back() })
        }

        // --- Shared screens from :features:shared ---
        composable(SettingsScreenRoute.route) {
            SharedSettingsScreen(
                onBackClick = { router.back() },
                onShowSettingSalary = { router.showSettingSalary() },
            )
        }
        composable(ProfileRoute.route) {
            ProfileScreen(router = router)
        }
        composable(SalaryCalculationRoute.route) {
            SharedSalaryCalculationScreen(
                onBackClick = { router.back() },
                onShowSettingSalary = { router.showSettingSalary() },
            )
        }
        composable(SettingSalaryRoute.route) {
            SharedSettingSalaryScreen(
                onBackClick = { router.back() },
            )
        }
        composable(SearchRoute.route) {
            SharedSearchScreen(
                onBackClick = { router.back() },
                onRouteClick = { basicData -> router.showRouteDetails(basicData) },
            )
        }
        composable(PurchasesRoute.route) {
            PurchasesScreen(router = router)
        }
        composable(AllRouteScreenRoute.route) {
            AllRouteScreen(router = router)
        }
        composable(WorkScheduleScreenRoute.route) {
            SharedWorkScheduleScreen(
                onBackClick = { router.back() },
            )
        }
        composable(SelectReleaseDaysScreenRoute.route) {
            SharedSelectReleaseDaysScreen(
                onBackClick = { router.back() },
            )
        }
        composable(MoreInfoRoute.route) { backStackEntry ->
            val monthId = backStackEntry.arguments?.read {
                if (contains("monthId")) getString("monthId") else null
            }
            SharedMoreInfoScreen(
                monthId = monthId,
                onBackClick = { router.back() },
            )
        }
    }
}
