package com.z_company.loco_driver.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.z_company.loco_driver.ui.navigation.RouterImpl
import com.z_company.loco_driver.ui.navigation.loginGraph
import com.z_company.loco_driver.ui.theme.LocoDriverTheme
import com.z_company.route.component.BottomNavigationBar
import com.z_company.route.navigation.FormRoute
import com.z_company.route.navigation.HomeFeature
import com.z_company.route.navigation.HomeRoute
import com.z_company.route.navigation.ProfileRoute
import com.z_company.route.navigation.SalaryCalculationRoute
import com.z_company.route.navigation.SettingsScreenRoute
import com.z_company.route.navigation.UpdatePresentationBlockDestination
import com.z_company.route.navigation.homeGraph
import androidx.compose.runtime.getValue
import com.z_company.route.navigation.login.AuthFeature

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LocoDriverApp(
    appState: LocoDriverAppState,
    isLoggedIn: Boolean,
    isShowFirstPresentation: Boolean,
    isShowUpdatePresentation: Boolean
) {
    LocoDriverTheme {
        val navController = rememberNavController()

        LaunchedEffect(navController) {
            (appState.router as? RouterImpl)?.updateNavController(navController)
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = Color.Transparent
        ) {
            // Определяем, нужно ли показывать нижнее меню
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            val bottomBarRoutes = setOf(
                HomeRoute.route,
                SalaryCalculationRoute.route,
                FormRoute.route,
                SettingsScreenRoute.route,
                ProfileRoute.route
            )

            val showBottomBar = true
//                isLoggedIn && currentRoute in bottomBarRoutes

            Scaffold(
                bottomBar = {
                    if (showBottomBar) {
                        BottomNavigationBar(navController = navController)
                    }
                },
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground
            ) { paddingValues ->
                NavHost(
                    navController = navController,
                    startDestination = HomeFeature.route,
//                        if (isLoggedIn) HomeFeature.route else AuthFeature.route,
                    modifier = Modifier.padding(paddingValues)
                ) {
                    // Граф авторизации
                    loginGraph(
                        router = appState.router,
                        isShowFirstPresentation = isShowFirstPresentation
                    )

                    // Всё основное приложение
                    homeGraph(router = appState.router)
                }
            }

            // Презентация обновления — поверх всего
            if (isLoggedIn && isShowUpdatePresentation) {
                UpdatePresentationBlockDestination(router = appState.router)
            }
        }
    }
}