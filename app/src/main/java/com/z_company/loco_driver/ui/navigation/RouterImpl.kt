package com.z_company.loco_driver.ui.navigation

import androidx.navigation.NavHostController
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Train
import com.z_company.domain.navigation.Router
import com.z_company.route.navigation.AllRouteScreenRoute
import com.z_company.route.navigation.CreatePhotoRoute
import com.z_company.route.navigation.DetailsRoute
import com.z_company.route.navigation.FormLoco
import com.z_company.route.navigation.FormPassenger
import com.z_company.route.navigation.FormRoute
import com.z_company.route.navigation.FormTrain
import com.z_company.route.navigation.HomeFeature
import com.z_company.route.navigation.HomeRoute
import com.z_company.route.navigation.MoreInfoRoute
import com.z_company.route.navigation.PreviewPhotoRoute
import com.z_company.route.navigation.PurchasesRoute
import com.z_company.route.navigation.SalaryCalculationRoute
import com.z_company.route.navigation.SearchRoute
import com.z_company.route.navigation.SelectReleaseDaysScreenRoute
import com.z_company.route.navigation.SettingSalaryRoute
import com.z_company.route.navigation.SettingsScreenRoute
import com.z_company.route.navigation.ViewingImageRoute
import com.z_company.route.navigation.WorkScheduleScreenRoute
import com.z_company.route.navigation.login.AuthFeature
import com.z_company.route.navigation.login.LogInScreenRoute
import com.z_company.route.navigation.login.RecoveryPasswordRoute
import com.z_company.route.navigation.login.SignInScreenRoute

class RouterImpl(
) : Router {
    private var navController: NavHostController? = null
    fun updateNavController(controller: NavHostController) {
        this.navController = controller
    }
    private fun requireNavController() = navController ?: throw IllegalStateException("NavController not set")

    override fun showSignIn() {
        requireNavController().navigate(SignInScreenRoute.route) {
            popUpTo(0)
        }
    }

    override fun showLogIn() {
        requireNavController().navigate(LogInScreenRoute.route)
    }

    override fun showRecoveryPassword() {
        requireNavController().navigate(RecoveryPasswordRoute.route)
    }

    override fun showStartScreen() {
        requireNavController().navigate(HomeFeature.route)
//        {
//            popUpTo(AuthFeature.route) {
//                inclusive = true
//                saveState = false
//            }
//        }
    }

    override fun showHome(startingRoute: String) {
        requireNavController().navigate(HomeRoute.route)
//        {
//            popUpTo(requireNavController().graph.startDestinationId) {  // или popUpTo(HomeFeature.route)
//                inclusive = false
//                saveState = true
//            }
//            launchSingleTop = true
//            restoreState = true
//        }
    }

    override fun showRouteForm(basicId: String?, isMakeCopy: Boolean) {
        requireNavController().navigate(
            FormRoute.buildDetailsRoute(basicId, isMakeCopy)
        )
    }

    override fun showRouteDetails(basicData: BasicData) {
        requireNavController().navigate(
            DetailsRoute.buildDetailsRoute(basicData.id)
        )
    }

    override fun showSearch() {
        requireNavController().navigate(
            SearchRoute.route
        )
    }

    override fun back() {
        requireNavController().popBackStack()
    }

    override fun navigationUp(): Boolean {
        return requireNavController().navigateUp()
    }

    override fun showChangedLocoForm(locomotive: Locomotive) {
        requireNavController().navigate(
            FormLoco.buildDetailsRoute(locomotive.locoId, locomotive.basicId)
        )
    }

    override fun showEmptyLocoForm(basicId: String) {
        requireNavController().navigate(
            FormLoco.buildDetailsRoute(locoId = null, basicId = basicId)
        )
    }

    override fun showChangeTrainForm(train: Train) {
        requireNavController().navigate(
            FormTrain.buildDetailsRoute(train.trainId, train.basicId)
        )
    }

    override fun showEmptyTrainForm(basicId: String) {
        requireNavController().navigate(
            FormTrain.buildDetailsRoute(trainId = null, basicId = basicId)
        )
    }

    override fun showChangePassengerForm(passenger: Passenger) {
        requireNavController().navigate(
            FormPassenger.buildDetailsRoute(passenger.passengerId, passenger.basicId)
        )
    }

    override fun showEmptyPassengerForm(basicId: String) {
        requireNavController().navigate(
            FormPassenger.buildDetailsRoute(passengerId = null, basicId = basicId)
        )
    }

    override fun showCameraScreen(basicId: String) {
        requireNavController().navigate(
            CreatePhotoRoute.buildRoute(basicId)
        )
    }

    override fun showPreviewPhotoScreen(photo: String, basicId: String) {
        requireNavController().navigate(
            PreviewPhotoRoute.buildRoute(photo, basicId)
        )
    }

    override fun showViewingImageScreen(imageId: String) {
        requireNavController().navigate(
            ViewingImageRoute.buildRoute(imageId)
        )
    }

    override fun showSelectReleaseDayScreen() {
        requireNavController().navigate(
            SelectReleaseDaysScreenRoute.route
        )
    }

    override fun showPurchasesScreen() {
        requireNavController().navigate(
            PurchasesRoute.route
        )
    }

    override fun showMoreInfo(monthOfYearId: String) {
        requireNavController().navigate(
            MoreInfoRoute.buildRoute(monthOfYearId)
        )
    }

    override fun showSalaryCalculation() {
        requireNavController().navigate(
            SalaryCalculationRoute.route
        )
    }

    override fun showSettingSalary() {
        requireNavController().navigate(
            SettingSalaryRoute.route
        )
    }

    override fun showAllRoute() {
        requireNavController().navigate(
            AllRouteScreenRoute.route
        )
    }

    override fun showWorkScheduleScreen() {
        requireNavController().navigate(
            WorkScheduleScreenRoute.route
        )
    }

    override fun showSettings() {
        requireNavController().navigate(SettingsScreenRoute.route)
    }
}