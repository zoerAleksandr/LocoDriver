package com.z_company.core.navigation

abstract class AppRoutes(val basicRoute: String) {
    open val route: String = basicRoute
}