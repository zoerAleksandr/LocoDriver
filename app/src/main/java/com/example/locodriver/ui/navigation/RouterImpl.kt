package com.example.locodriver.ui.navigation

import androidx.navigation.NavHostController
import com.example.domain.entities.route.BasicData
import com.example.domain.entities.route.Locomotive
import com.example.domain.entities.route.Notes
import com.example.domain.entities.route.Passenger
import com.example.domain.entities.route.Route
import com.example.domain.entities.route.Train
import com.example.domain.navigation.Router
import com.example.login.navigation.LoginFeature
import com.example.login.navigation.LoginScreenRoute
import com.example.route.navigation.DetailsRoute
import com.example.route.navigation.FormLoco
import com.example.route.navigation.FormRoute
import com.example.route.navigation.HomeRoute
import com.example.settings.navigation.SettingsFeature

class RouterImpl(
    private val navController: NavHostController
) : Router {
    override fun showLogin() {
        navController.navigate(LoginScreenRoute.route) {
            popUpTo(0)
        }
    }

    override fun showHome() {
        navController.navigate(HomeRoute.route) {
            popUpTo(LoginFeature.route) {
                inclusive = true
                saveState = false
            }
        }
    }

    override fun showRouteForm(route: Route?) {
        navController.navigate(
            FormRoute.buildDetailsRoute(route?.basicData?.id)
        )
    }

    override fun showRouteDetails(basicData: BasicData) {
        navController.navigate(
            DetailsRoute.buildDetailsRoute(basicData.id)
        )
    }

    override fun showSettings() {
        navController.navigate(SettingsFeature.route)
    }

    override fun showSearch() {
        TODO("Not yet implemented")
    }

    override fun back() {
        navController.popBackStack()
    }

    override fun navigationUp(): Boolean {
        return navController.navigateUp()
    }

    override fun showChangedLocoForm(locomotive: Locomotive) {
        navController.navigate(
            FormLoco.buildDetailsRoute(locomotive.locoId, locomotive.basicId)
        )
    }

    override fun showEmptyLocoForm(basicId: String) {
        navController.navigate(
            FormLoco.buildDetailsRoute(locoId = null, basicId = basicId)
        )
    }

    override fun showLocoDetails(locomotive: Locomotive) {
        TODO("Not yet implemented")
    }

    override fun showTrainForm(train: Train?) {
        TODO("Not yet implemented")
    }

    override fun showTrainDetails(train: Train) {
        TODO("Not yet implemented")
    }

    override fun showPassengerForm(passenger: Passenger?) {
        TODO("Not yet implemented")
    }

    override fun showPassengerDetails(passenger: Passenger) {
        TODO("Not yet implemented")
    }

    override fun showNotesForm(notes: Notes?) {
        TODO("Not yet implemented")
    }

    override fun showNotesDetails(notes: Notes) {
        TODO("Not yet implemented")
    }
}