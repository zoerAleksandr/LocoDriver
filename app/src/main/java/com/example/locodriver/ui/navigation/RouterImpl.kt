package com.example.locodriver.ui.navigation

import androidx.navigation.NavHostController
import com.example.domain.entities.Route
import com.example.domain.navigation.Router

class RouterImpl(
    private val navigationController: NavHostController
) : Router {
    override fun showLogin() {
        TODO("Not yet implemented")
    }

    override fun showHome() {
        TODO("Not yet implemented")
    }

    override fun showRouteForm(route: Route?) {
        TODO("Not yet implemented")
    }

    override fun showRouteDetails(route: Route) {
        TODO("Not yet implemented")
    }

    override fun showSettings() {
        TODO("Not yet implemented")
    }

    override fun back() {
        TODO("Not yet implemented")
    }

    override fun navigationUp(): Boolean {
        TODO("Not yet implemented")
    }
}