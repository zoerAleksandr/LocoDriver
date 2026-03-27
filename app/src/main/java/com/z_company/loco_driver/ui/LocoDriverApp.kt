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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.domain.entities.route.Route
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
import androidx.activity.ComponentActivity // Изменено: Уже был, но подтверждено — нужен для доступа к window.
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LocoDriverApp(
    appState: LocoDriverAppState,
    isShowUpdatePresentation: Boolean,
    pendingImportRoute: Route? = null,
    onConfirmImport: () -> Unit = {},
    onDismissImport: () -> Unit = {},
    pendingFormOpen: Boolean = false,
    onFormOpened: () -> Unit = {},
    pendingNavigateHome: Boolean = false,
    onNavigatedHome: () -> Unit = {}
) {
    LocoDriverTheme {
        val navController = rememberNavController()

        LaunchedEffect(navController) {
            (appState.router as? RouterImpl)?.updateNavController(navController)
        }

        // Навигация из виджета → FormScreen (добавить маршрут)
        LaunchedEffect(pendingFormOpen) {
            if (pendingFormOpen) {
                navController.navigate(FormRoute.buildDetailsRoute(null, false)) {
                    launchSingleTop = true
                }
                onFormOpened()
            }
        }

        // Навигация из виджета → HomeScreen (по тапу на тело виджета)
        LaunchedEffect(pendingNavigateHome) {
            if (pendingNavigateHome) {
                navController.popBackStack(HomeRoute.route, inclusive = false)
                onNavigatedHome()
            }
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

        val showBottomBar = currentRoute in bottomBarRoutes

        val navBarColor = if (currentRoute in bottomBarRoutes) {
            surfaceColor
        } else {
            backgroundColor
        }

        SideEffect {
            val window = (localContext as? ComponentActivity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            val isLightBackground = backgroundColor.luminance() > 0.5f
            val isLightNavBar = navBarColor.luminance() > 0.5f
            insetsController.isAppearanceLightStatusBars = isLightBackground
            insetsController.isAppearanceLightNavigationBars = isLightNavBar
        }

        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    })
                }
        ) {
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
                        startDestination = HomeFeature.route,
                        modifier = Modifier.padding(paddingValues),
                        enterTransition = { fadeIn() },
                        exitTransition = { fadeOut() },
                    ) {
                        homeGraph(router = appState.router)
                    }
                }
            }

            // Презентация обновления — поверх всего
            if (isShowUpdatePresentation) {
                UpdatePresentationBlockDestination(router = appState.router)
            }

            // Диалог импорта маршрута из .zroute файла
            if (pendingImportRoute != null) {
                AlertDialog(
                    onDismissRequest = onDismissImport,
                    shape = Shapes.medium,
                    title = {
                        Text(text = "Импорт маршрута", style = AppTypography.getType().headlineSmall)
                    },
                    text = {
                        Text(
                            text = "Импортировать маршрут от ${pendingImportRoute.basicData.timeStartWork?.let {
                                java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()).format(java.util.Date(it))
                            } ?: "неизвестной даты"}?",
                            style = AppTypography.getType().bodyLarge
                        )
                    },
                    confirmButton = {
                        Button(shape = Shapes.medium, onClick = onConfirmImport) {
                            Text(text = "Импортировать", style = AppTypography.getType().titleMedium)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = onDismissImport) {
                            Text(
                                text = "Отмена",
                                style = AppTypography.getType().titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
            }
        }
    }
}