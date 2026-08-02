package com.z_company.iosapp.navigation

/**
 * Маршруты iOS-приложения.
 *
 * Строки совпадают со значениями в features/route/navigation/Routes.kt (Android),
 * чтобы при миграции features на KMP можно было использовать общие объекты.
 *
 * Аргументы в URL-паттернах соответствуют features, но без androidx.navigation.NavType —
 * типизация аргументов будет добавлена при полном портировании экранов.
 */

internal object HomeFeature { const val route = "HomeFeature" }
internal object HomeRoute { const val route = "HomeRoute" }

internal object FormRoute {
    private const val paramRouteId = "routeId"
    private const val paramMakeCopy = "makeCopy"
    const val route = "FormRoute?$paramRouteId={$paramRouteId}/?$paramMakeCopy={$paramMakeCopy}"
    const val base = "FormRoute"

    fun buildRoute(basicId: String? = null, isMakeCopy: Boolean = false): String =
        basicId?.let { "$base?$paramRouteId=$it/?$paramMakeCopy=$isMakeCopy" } ?: base
}

internal object FormLoco {
    private const val paramLocoId = "locoId"
    private const val paramBasicId = "basicId"
    const val route = "FormLoco/{$paramBasicId}?{$paramLocoId}"

    fun buildRoute(locoId: String?, basicId: String) = "FormLoco/$basicId?$locoId"
}

internal object FormTrain {
    private const val paramTrainId = "trainId"
    private const val paramBasicId = "basicId"
    const val route = "FormTrain/{$paramBasicId}?{$paramTrainId}"

    fun buildRoute(trainId: String?, basicId: String) = "FormTrain/$basicId?$trainId"
}

internal object FormPassenger {
    private const val paramPassengerId = "passengerId"
    private const val paramBasicId = "basicId"
    const val route = "FormPassenger/{$paramBasicId}?{$paramPassengerId}"

    fun buildRoute(passengerId: String?, basicId: String) = "FormPassenger/$basicId?$passengerId"
}

internal object SalaryCalculationRoute { const val route = "SalaryCalculationRoute" }
internal object SettingSalaryRoute { const val route = "SettingSalaryRoute" }
internal object SearchRoute { const val route = "SearchRoute" }
internal object PurchasesRoute { const val route = "PurchasesRoute" }
internal object SettingsScreenRoute { const val route = "SettingsScreen" }
internal object ProfileRoute { const val route = "ProfileRoute" }
internal object AllRouteScreenRoute { const val route = "AllRouteScreenRoute" }
internal object SelectReleaseDaysScreenRoute { const val route = "SelectReleaseDaysScreen" }
