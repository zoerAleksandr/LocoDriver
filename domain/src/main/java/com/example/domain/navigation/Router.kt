package com.example.domain.navigation

import com.example.domain.entities.Route

interface Router {
    fun showLogin()
    fun showHome()
    fun showRouteForm(route: Route? = null)
    fun showRouteDetails(route: Route)
    fun showSettings()
    fun back()
    fun navigationUp(): Boolean
}
