package com.z_company.loco_driver.ui.navigation

import androidx.navigation.NavHostController
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Train
import com.z_company.domain.navigation.Router
import com.z_company.login.navigation.LoginFeature
import com.z_company.login.navigation.LoginScreenRoute
import com.z_company.route.navigation.CreatePhotoRoute
import com.z_company.route.navigation.DetailsRoute
import com.z_company.route.navigation.FormLoco
import com.z_company.route.navigation.FormPassenger
import com.z_company.route.navigation.FormRoute
import com.z_company.route.navigation.FormTrain
import com.z_company.route.navigation.HomeRoute
import com.z_company.route.navigation.PreviewPhotoRoute
import com.z_company.route.navigation.ViewingImageRoute
import com.z_company.settings.navigation.SettingsFeature

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

    override fun showRouteForm(basicId: String?) {
        navController.navigate(
            FormRoute.buildDetailsRoute(basicId)
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

    override fun showChangeTrainForm(train: Train) {
        navController.navigate(
            FormTrain.buildDetailsRoute(train.trainId, train.basicId)
        )
    }

    override fun showEmptyTrainForm(basicId: String) {
        navController.navigate(
            FormTrain.buildDetailsRoute(trainId = null, basicId = basicId)
        )
    }

    override fun showTrainDetails(train: Train) {
        TODO("Not yet implemented")
    }

    override fun showChangePassengerForm(passenger: Passenger) {
        navController.navigate(
            FormPassenger.buildDetailsRoute(passenger.passengerId, passenger.basicId)
        )
    }

    override fun showEmptyPassengerForm(basicId: String) {
        navController.navigate(
            FormPassenger.buildDetailsRoute(passengerId = null, basicId = basicId)
        )
    }

    override fun showPassengerDetails(passenger: Passenger) {
        TODO("Not yet implemented")
    }

    override fun showCameraScreen(basicId: String) {
        navController.navigate(
            CreatePhotoRoute.buildRoute(basicId)
        )
    }

    override fun showPreviewPhotoScreen(photo: String, basicId: String) {
        navController.navigate(
            PreviewPhotoRoute.buildRoute(photo, basicId)
        )
    }

    override fun showViewingImageScreen(imageId: String) {
        navController.navigate(
            ViewingImageRoute.buildRoute(imageId)
        )
    }
}