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

object FormRoute : AppRoutes("AddingRoute") {
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