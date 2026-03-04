package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import com.z_company.core.navigation.AppRoutes
import com.z_company.domain.navigation.Router
import com.z_company.shared.ui.screen.HomeScreen as SharedHomeScreen
import com.z_company.route.R

@Composable
fun HomeDestination(
    router: Router
) {
    SharedHomeScreen(
        onRouteClick = { routeId ->
            router.showRouteForm(routeId)
        },
        onNewRouteClick = { router.showRouteForm() },
        onSettingsClick = { router.showSettings() },
        onSalaryClick = { router.showSalaryCalculation() },
        onAllRouteClick = { router.showAllRoute() },
        onWorkScheduleClick = { router.showWorkScheduleScreen() },
        onSearchClick = { router.showSearch() },
        onMoreInfoClick = { router.showMoreInfo(it) },
    )
}


sealed class NavigationItem(var route: AppRoutes, var icon: Int, var title: String) {
    data object Home : NavigationItem(HomeRoute, R.drawable.home_24px, "Главная")
    data object Money : NavigationItem(SalaryCalculationRoute, R.drawable.wallet_24px, "Зарплата")
    data object Add : NavigationItem(FormRoute, R.drawable.add_circle_24px, "Добавить")
    data object Setting : NavigationItem(SettingsScreenRoute, R.drawable.settings_24px, "Настройки")
    data object Profile : NavigationItem(ProfileRoute, R.drawable.account_circle_24px, "Профиль")
}
