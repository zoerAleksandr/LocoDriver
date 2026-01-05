package com.z_company.loco_driver.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance // Изменено: Добавлен импорт для luminance() — это метод Color, чтобы вычислить яркость цвета (0..1).
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import com.z_company.route.navigation.login.AuthFeature
import androidx.activity.ComponentActivity // Изменено: Уже был, но подтверждено — нужен для доступа к window.
import androidx.compose.foundation.layout.WindowInsets // Изменено: Уже были, но добавлены комментарии для ясности.
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemBars // Изменено: Добавлен импорт для WindowInsets.systemBars, чтобы использовать в paddings.

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

        val backgroundColor = MaterialTheme.colorScheme.background
        val surfaceColor = MaterialTheme.colorScheme.surface

        // Определяем, нужно ли показывать нижнее меню
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val localContext = LocalContext.current
        val bottomBarRoutes = setOf(
            HomeRoute.route,
            SalaryCalculationRoute.route,
            FormRoute.route,
            SettingsScreenRoute.route,
            ProfileRoute.route
        )

        val showBottomBar = isLoggedIn && currentRoute in bottomBarRoutes

        val navBarColor = if (currentRoute in bottomBarRoutes) {
            surfaceColor
        } else {
            backgroundColor
        }

        // Изменено: Обновлён SideEffect для динамического управления цветом иконок на основе luminance фонов.
        // Зачем: В Android 15+ система не всегда автоматически адаптирует иконки; мы вычисляем, светлый ли фон (luminance > 0.5), и устанавливаем isAppearanceLight... соответственно.
        // Если фон тёмный (luminance < 0.5) — isAppearanceLight = false → светлые иконки (как primary в тёмной теме).
        // Если светлый — true → тёмные иконки. Это обеспечивает контраст, и решает проблему "systemBar светлый с светлыми иконками".
        // Для status bar — на основе backgroundColor (всегда под ним). Для nav bar — на основе navBarColor (может меняться по маршрутам).
        SideEffect {
            val window = (localContext as? ComponentActivity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            val isLightBackground = backgroundColor.luminance() > 0.5f
            val isLightNavBar = navBarColor.luminance() > 0.5f
            insetsController.isAppearanceLightStatusBars = isLightBackground
            insetsController.isAppearanceLightNavigationBars = isLightNavBar
        }

        // Изменено: Добавлен modifier .background(backgroundColor) explicitly на root Box, но уже был; добавлено для ясности.
        // Зачем: Убеждаемся, что фон рисуется под всеми барами (в edge-to-edge прозрачные бары покажут этот цвет).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor) // Фон для всего экрана, включая под статус-баром и навигационным баром.
        ) {
            // Изменено: Уточнён Box для nav bar — добавлен background(navBarColor), но уже был; добавлено условие if (navHeight > 0.dp) для avoidance ошибок на устройствах без nav bar.
            // Зачем: В gesture navigation nav bar может отсутствовать (height=0), так что не рисуем лишний Box; это предотвращает артефакты.
            val density = LocalDensity.current
            val navInsets = WindowInsets.navigationBars.asPaddingValues()
            val navHeight = navInsets.calculateBottomPadding()
            if (navHeight > 0.dp) {
                Box(
                    modifier = Modifier
                        .background(navBarColor)
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(navHeight)
                )
            }

            // Изменено: Surface сделан transparent (color.copy(alpha=0f)), но уже был; paddings заменены на WindowInsets.systemBars для полного покрытия (status + nav + cutouts).
            // Зачем: Content отступает от баров, но фон root виден под ними; это решает "systemBar светлый" — он станет как backgroundColor.
            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0f), // Transparent, чтобы не перекрывать root-фон.
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.systemBars) // Изменено: Используем systemBars вместо отдельных paddings — охватывает все бары и cutouts для лучшей совместимости.
            ) {
                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            BottomNavigationBar(navController = navController)
                        }
                    },
                ) { paddingValues ->
                    NavHost(
                        navController = navController,
                        startDestination = if (isLoggedIn) HomeFeature.route else AuthFeature.route,
                        modifier = Modifier.padding(paddingValues),
                        enterTransition = { fadeIn() },
                        exitTransition = { fadeOut() },
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
            }

            // Презентация обновления — поверх всего
            if (isLoggedIn && isShowUpdatePresentation) {
                UpdatePresentationBlockDestination(router = appState.router)
            }
        }
    }
}