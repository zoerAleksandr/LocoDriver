package com.z_company.loco_driver.ui.navigation

import androidx.navigation.NavHostController
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Train
import com.z_company.domain.navigation.Router
import com.z_company.route.navigation.AllRouteScreenRoute
import com.z_company.route.navigation.DetailsRoute
import com.z_company.route.navigation.FormLoco
import com.z_company.route.navigation.FormPassenger
import com.z_company.route.navigation.FormRoute
import com.z_company.route.navigation.FormTrain
import com.z_company.route.navigation.HomeFeature
import com.z_company.route.navigation.HomeRoute
import com.z_company.route.navigation.MoreInfoRoute
import com.z_company.route.navigation.PurchasesRoute
import com.z_company.route.navigation.SalaryCalculationRoute
import com.z_company.route.navigation.SearchRoute
import com.z_company.route.navigation.SelectReleaseDaysScreenRoute
import com.z_company.route.navigation.SettingSalaryRoute
import com.z_company.route.navigation.SettingsScreenRoute
import com.z_company.route.navigation.WorkScheduleScreenRoute
import com.z_company.route.navigation.CalendarRoute
import com.z_company.route.navigation.ScheduleWizardRoute
import com.z_company.route.navigation.AbsenceRoute
import com.z_company.route.navigation.StatisticsRoute
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
    }

    override fun showHome(startingRoute: String) {
        requireNavController().navigate(HomeRoute.route)
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

    override fun showCalendar() {
        requireNavController().navigate(CalendarRoute.route)
    }

    override fun showScheduleWizard() {
        requireNavController().navigate(ScheduleWizardRoute.route)
    }

    override fun showAbsence() {
        requireNavController().navigate(AbsenceRoute.route)
    }

    override fun showStatistics() {
        requireNavController().navigate(StatisticsRoute.route)
    }

    override fun showSettings() {
        requireNavController().navigate(SettingsScreenRoute.buildRoute())
    }

    override fun showSettingsRoute() {
        requireNavController().navigate(SettingsScreenRoute.buildRoute("ROUTE"))
    }

    override fun showSettingsRouteForm() {
        requireNavController().navigate(SettingsScreenRoute.buildRoute("ROUTE_FORM"))
    }

    override fun showSettingsLoco() {
        requireNavController().navigate(SettingsScreenRoute.buildRoute("LOCOMOTIVE"))
    }

    override fun showSettingsRest() {
        requireNavController().navigate(SettingsScreenRoute.buildRoute("REST"))
    }

    override fun showSettingsSeriesList() {
        requireNavController().navigate(SettingsScreenRoute.buildRoute("SERIES_LIST"))
    }

    override fun showSettingsSeriesEditor(seriesId: String) {
        requireNavController().navigate(SettingsScreenRoute.buildRoute("SERIES_EDITOR_$seriesId"))
    }

    override fun showSettingsStationList() {
        requireNavController().navigate(SettingsScreenRoute.buildRoute("STATION_LIST"))
    }

    override fun showSettingsStationEditor(stationId: String) {
        requireNavController().navigate(SettingsScreenRoute.buildRoute("STATION_EDITOR_$stationId"))
    }
}