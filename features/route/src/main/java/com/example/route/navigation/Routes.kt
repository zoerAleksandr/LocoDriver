package com.example.route.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.core.navigation.AppRoutes

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
    override val route: String = "$basicRoute?$paramRouteId={$paramRouteId}"
    val navArguments = listOf(
        navArgument(paramRouteId) {
            type = NavType.StringType
            nullable = true
        }
    )

    fun getRouteId(backStackEntry: NavBackStackEntry): String? =
        backStackEntry.arguments?.getString(paramRouteId)

    fun buildDetailsRoute(id: String?) =
        id?.let { "$basicRoute?$paramRouteId=$id" } ?: basicRoute
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

object CreatePhotoRoute: AppRoutes("CreatePhoto") {
    private const val paramBasicId = "basicId"
    override val route: String = "$basicRoute/{$paramBasicId}"
    val navArguments = listOf(
        navArgument(paramBasicId){
            type = NavType.StringType
            nullable = false
        }
    )
    fun getBasicId(backStackEntry: NavBackStackEntry): String? =
        backStackEntry.arguments?.getString(paramBasicId)
    fun buildRoute(basicId: String) =
        "$basicRoute/$basicId"
}

object PreviewPhotoRoute: AppRoutes("PreviewPhoto") {
    private const val paramPhotoUrl = "paramPhotoUrl"
    private const val paramBasicId = "paramBasicId"

    override val route: String = "$basicRoute/{$paramBasicId}/{$paramPhotoUrl}"
    val navArguments = listOf(
        navArgument(paramPhotoUrl) {
            type = NavType.StringType
            nullable = false
        }
    )

    fun getPhotoUrl(backStackEntry: NavBackStackEntry): String? =
        backStackEntry.arguments?.getString(paramPhotoUrl)
    fun getBasicId(backStackEntry: NavBackStackEntry): String? =
        backStackEntry.arguments?.getString(paramBasicId)
    fun buildRoute(photoUrl: String, basicId: String) =
        "$basicRoute/$basicId/$photoUrl"
}

object ViewingImageRoute: AppRoutes("ViewingImage") {
    private const val paramImageId = "paramImageId"

    override val route: String = "$basicRoute/{$paramImageId}"
    val navArguments = listOf(
        navArgument(paramImageId) {
            type = NavType.StringType
            nullable = false
        }
    )
    fun getImageId(backStackEntry: NavBackStackEntry): String? =
        backStackEntry.arguments?.getString(paramImageId)
    fun buildRoute(imageId: String) =
        "$basicRoute/$imageId"
}