package com.z_company.route.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.z_company.core.navigation.AppRoutes

object HomeFeature : AppRoutes("HomeFeature")
object HomeRoute : AppRoutes("HomeRoute")
object DetailsRoute : AppRoutes("RouteDetailsRoute") {
    private const val paramRouteId = "routeId"
    override val route: String = "$basicRoute/{$paramRouteId}"
    val navArguments = listOf(
        navArgument(paramRouteId) {
            type = NavType.StringType
        }
    )

    fun getRouteId(backStackEntry: NavBackStackEntry): String? =
        backStackEntry.arguments?.getString(paramRouteId)

    fun buildDetailsRoute(id: String) = "$basicRoute/$id"
}

object FormRoute : AppRoutes("FormRoute") {
    private const val paramRouteId = "routeId"
    private const val paramMakeCopy = "makeCopy"
    override val route: String = "$basicRoute?$paramRouteId={$paramRouteId}/?$paramMakeCopy={$paramMakeCopy}"
    val navArguments = listOf(
        navArgument(paramRouteId) {
            type = NavType.StringType
            nullable = true
        },
        navArgument(paramMakeCopy) {
            type = NavType.BoolType
            nullable = false
            defaultValue = false
        }
    )

    fun getRouteId(backStackEntry: NavBackStackEntry): String? =
        backStackEntry.arguments?.getString(paramRouteId)

    fun isMakeCopy(backStackEntry: NavBackStackEntry): Boolean =
        backStackEntry.arguments?.getBoolean(paramMakeCopy) ?: false

    fun buildDetailsRoute(id: String?, isMakeCopy: Boolean) =
        id?.let { "$basicRoute?$paramRouteId=$id/?$paramMakeCopy=$isMakeCopy" } ?: basicRoute
}

object FormLoco : AppRoutes("FormLoco") {
    private const val paramLocoId = "locoId"
    private const val paramBasicId = "basicId"
    override val route: String = "$basicRoute/{$paramBasicId}?{$paramLocoId}"
    val navArguments = listOf(
        navArgument(paramLocoId) {
            type = NavType.StringType
            nullable = true
        },
        navArgument(paramBasicId) {
            type = NavType.StringType
            nullable = true
        }
    )

    fun getLocoId(backStackEntry: NavBackStackEntry): String? =
        backStackEntry.arguments?.getString(paramLocoId)

    fun getBasicId(backStackEntry: NavBackStackEntry): String? =
        backStackEntry.arguments?.getString(paramBasicId)

    fun buildDetailsRoute(locoId: String?, basicId: String) =
        "$basicRoute/$basicId?$locoId"
}

object FormTrain: AppRoutes("FormTrain") {
    private const val paramTrainId = "trainId"
    private const val paramBasicId = "basicId"
    override val route: String = "$basicRoute/{$paramBasicId}?{$paramTrainId}"
    val navArguments = listOf(
        navArgument(paramTrainId) {
            type = NavType.StringType
            nullable = true
        },
        navArgument(paramBasicId) {
            type = NavType.StringType
            nullable = true
        }
    )
    fun getTrainId(backStackEntry: NavBackStackEntry): String? =
        backStackEntry.arguments?.getString(paramTrainId)
    fun getBasicId(backStackEntry: NavBackStackEntry): String? =
        backStackEntry.arguments?.getString(paramBasicId)
    fun buildDetailsRoute(trainId: String?, basicId: String) =
        "$basicRoute/$basicId?$trainId"
}

object FormPassenger: AppRoutes("FormPassenger") {
    private const val paramPassengerId = "passengerId"
    private const val paramBasicId = "basicId"
    override val route: String = "$basicRoute/{$paramBasicId}?{$paramPassengerId}"
    val navArguments = listOf(
        navArgument(paramPassengerId) {
            type = NavType.StringType
            nullable = true
        },
        navArgument(paramBasicId) {
            type = NavType.StringType
            nullable = true
        }
    )
    fun getPassengerId(backStackEntry: NavBackStackEntry): String? =
        backStackEntry.arguments?.getString(paramPassengerId)
    fun getBasicId(backStackEntry: NavBackStackEntry): String? =
        backStackEntry.arguments?.getString(paramBasicId)
    fun buildDetailsRoute(passengerId: String?, basicId: String) =
        "$basicRoute/$basicId?$passengerId"
}

object SearchRoute: AppRoutes("SearchRoute")
object PurchasesRoute: AppRoutes("PurchasesRoute")

object MoreInfoRoute: AppRoutes("MoreInfoRoute") {
    private const val paramMonthOfYearId = "paramMonthOfYearId"
    override val route: String = "$basicRoute/{$paramMonthOfYearId}"
    val navArguments = listOf(
        navArgument(paramMonthOfYearId) {
            type = NavType.StringType
            nullable = false
        }
    )
    fun getMonthOfYearId(backStackEntry: NavBackStackEntry): String? =
        backStackEntry.arguments?.getString(paramMonthOfYearId)
    fun buildRoute(monthId: String) =
        "$basicRoute/$monthId"
}

object SalaryCalculationRoute: AppRoutes("SalaryCalculationRoute")
object SettingSalaryRoute: AppRoutes("SettingSalaryRoute")
object UpdatePresentationBlockRoute: AppRoutes("UpdatePresentationBlockRoute")
object AllRouteScreenRoute: AppRoutes("AllRouteScreenRoute")
object WorkScheduleScreenRoute: AppRoutes("WorkScheduleScreenRoute")
object SettingsScreenRoute : AppRoutes("SettingsScreen") {
    private const val paramSubScreen = "subScreen"
    override val route: String = "$basicRoute?$paramSubScreen={$paramSubScreen}"
    val navArguments = listOf(
        navArgument(paramSubScreen) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        }
    )

    fun getSubScreen(backStackEntry: NavBackStackEntry): String? =
        backStackEntry.arguments?.getString(paramSubScreen)

    fun buildRoute(subScreen: String? = null): String =
        if (subScreen != null) "$basicRoute?$paramSubScreen=$subScreen" else basicRoute
}
object SelectReleaseDaysScreenRoute: AppRoutes("SelectReleaseDaysScreen")
object ProfileRoute: AppRoutes("ProfileRoute")