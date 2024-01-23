package com.example.route.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.core.navigation.AppRoutes

object HomeFeature : AppRoutes("HomeFeature")
object HomeRoute : AppRoutes("HomeRoute")
object DetailsRoute : AppRoutes("RouteDetailsRoute") {
    private const val paramRouteId = "routeId"
    override val route: String = "$baseRoute/{$paramRouteId}"
    val navArguments = listOf(
        navArgument(paramRouteId) {
            type = NavType.StringType
        }
    )

    fun getRouteId(backStackEntry: NavBackStackEntry): String? =
        backStackEntry.arguments?.getString(paramRouteId)

    fun buildDetailsRoute(id: String) = "$baseRoute/$id"
}

object FormRoute : AppRoutes("FormRoute") {
    private const val paramRouteId = "routeId"
    override val route: String = "$baseRoute?$paramRouteId={$paramRouteId}"
    val navArguments = listOf(
        navArgument(paramRouteId) {
            type = NavType.StringType
            nullable = true
        }
    )

    fun getRouteId(backStackEntry: NavBackStackEntry): String? =
        backStackEntry.arguments?.getString(paramRouteId)

    fun buildDetailsRoute(id: String?) =
        id?.let { "$baseRoute?$paramRouteId=$id" } ?: baseRoute
}

object FormLoco : AppRoutes("FormLoco") {
    private const val paramLocoId = "locoId"
    private const val paramBasicId = "basicId"
    override val route: String = "$baseRoute/{$paramBasicId}?{$paramLocoId}"
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
        "$baseRoute/$basicId?$locoId"
}