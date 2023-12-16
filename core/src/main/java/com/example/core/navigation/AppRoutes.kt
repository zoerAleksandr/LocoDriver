package com.example.core.navigation

abstract class AppRoutes(val baseRoute: String) {
    open val route: String = baseRoute
}