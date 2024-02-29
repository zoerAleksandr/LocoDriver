package com.example.core.navigation

abstract class AppRoutes(val basicRoute: String) {
    open val route: String = basicRoute
}