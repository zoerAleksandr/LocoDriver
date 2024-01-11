package com.example.domain.navigation

import com.example.domain.entities.route.BasicData
import com.example.domain.entities.route.Route

interface Router {
    fun showLogin()
    fun showHome()
    fun showRouteForm(route: Route? = null)
    fun showRouteDetails(basicData: BasicData)
    fun showSettings()
    fun showSearch()
    fun back()
    fun navigationUp(): Boolean
}
