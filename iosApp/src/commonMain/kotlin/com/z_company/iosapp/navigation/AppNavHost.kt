package com.z_company.iosapp.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.savedstate.read
import com.z_company.shared.ui.component.BottomTab
import com.z_company.shared.ui.component.SharedBottomNavigationBar

// All screens from :features:shared
import com.z_company.shared.ui.screen.AllRouteScreen as SharedAllRouteScreen
import com.z_company.shared.ui.screen.FormLocoScreen as SharedFormLocoScreen
import com.z_company.shared.ui.screen.FormPassengerScreen as SharedFormPassengerScreen
import com.z_company.shared.ui.screen.FormRouteScreen as SharedFormRouteScreen
import com.z_company.shared.ui.screen.FormTrainScreen as SharedFormTrainScreen
import com.z_company.shared.ui.screen.HomeScreen as SharedHomeScreen
import com.z_company.shared.ui.screen.MoreInfoScreen as SharedMoreInfoScreen
import com.z_company.shared.ui.screen.ProfileScreen as SharedProfileScreen
import com.z_company.shared.ui.screen.PurchasesScreen as SharedPurchasesScreen
import com.z_company.shared.ui.screen.SalaryCalculationScreen as SharedSalaryCalculationScreen
import com.z_company.shared.ui.screen.SearchScreen as SharedSearchScreen
import com.z_company.shared.ui.screen.SelectReleaseDaysScreen as SharedSelectReleaseDaysScreen
import com.z_company.shared.ui.screen.SettingSalaryScreen as SharedSettingSalaryScreen
import com.z_company.shared.ui.screen.SettingsScreen as SharedSettingsScreen
import com.z_company.shared.ui.screen.WorkScheduleScreen as SharedWorkScheduleScreen

/**
 * Корневой NavHost iOS-приложения.
 *
 * Все экраны теперь используются из :features:shared.
 * Scaffold с нижней навигацией показывается только на основных экранах.
 */

// Маршруты, на которых показывается нижняя навигация
private val bottomBarRoutes = setOf(
    HomeRoute.route,
    SalaryCalculationRoute.route,
    SettingsScreenRoute.route,
    ProfileRoute.route,
)

// Маппинг маршрута навигации → таб
private fun routeToTab(route: String?): BottomTab = when (route) {
    SalaryCalculationRoute.route -> BottomTab.Salary
    SettingsScreenRoute.route -> BottomTab.Settings
    ProfileRoute.route -> BottomTab.Profile
    else -> BottomTab.Home
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val router = remember { IosRouterImpl() }

    LaunchedEffect(navController) {
        router.navController = navController
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                SharedBottomNavigationBar(
                    currentTab = routeToTab(currentRoute),
                    onTabSelected = { tab ->
                        val targetRoute = when (tab) {
                            BottomTab.Home -> HomeRoute.route
                            BottomTab.Salary -> SalaryCalculationRoute.route
                            BottomTab.Add -> {
                                router.showRouteForm()
                                return@SharedBottomNavigationBar
                            }
                            BottomTab.Settings -> SettingsScreenRoute.route
                            BottomTab.Profile -> ProfileRoute.route
                        }
                        if (targetRoute != currentRoute) {
                            navController.navigate(targetRoute) {
                                popUpTo(HomeRoute.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(HomeRoute.route) {
                SharedHomeScreen(
                    onRouteClick = { routeId -> router.showRouteForm(routeId) },
                    onNewRouteClick = { router.showRouteForm() },
                    onSettingsClick = { router.showSettings() },
                    onSalaryClick = { router.showSalaryCalculation() },
                    onAllRouteClick = { router.showAllRoute() },
                    onWorkScheduleClick = { router.showWorkScheduleScreen() },
                    onSearchClick = { router.showSearch() },
                    onMoreInfoClick = { monthId -> router.showMoreInfo(monthId) },
                )
            }
            composable(FormRoute.route) { backStackEntry ->
                val routeId = backStackEntry.arguments?.read {
                    if (contains("routeId")) getString("routeId") else null
                }
                SharedFormRouteScreen(
                    routeId = routeId,
                    onBackClick = { router.back() },
                    onLocoClick = { locoId, basicId ->
                        navController.navigate(FormLoco.buildRoute(locoId, basicId))
                    },
                    onNewLocoClick = { basicId -> router.showEmptyLocoForm(basicId) },
                    onTrainClick = { trainId, basicId ->
                        navController.navigate(FormTrain.buildRoute(trainId, basicId))
                    },
                    onNewTrainClick = { basicId -> router.showEmptyTrainForm(basicId) },
                    onPassengerClick = { passengerId, basicId ->
                        navController.navigate(FormPassenger.buildRoute(passengerId, basicId))
                    },
                    onNewPassengerClick = { basicId -> router.showEmptyPassengerForm(basicId) },
                )
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

            composable(SettingsScreenRoute.route) {
                SharedSettingsScreen(
                    onBackClick = { router.back() },
                    onShowSettingSalary = { router.showSettingSalary() },
                )
            }
            composable(ProfileRoute.route) {
                SharedProfileScreen(
                    onBackClick = { router.back() },
                    onPurchasesClick = { router.showPurchasesScreen() },
                )
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
                SharedPurchasesScreen(
                    onBackClick = { router.back() },
                )
            }
            composable(AllRouteScreenRoute.route) {
                SharedAllRouteScreen(
                    onBackClick = { router.back() },
                    onRouteClick = { basicData -> router.showRouteDetails(basicData) },
                )
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
}
