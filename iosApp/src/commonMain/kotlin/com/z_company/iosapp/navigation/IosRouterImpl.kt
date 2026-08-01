package com.z_company.iosapp.navigation

import androidx.navigation.NavHostController
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Train
import com.z_company.domain.navigation.Router

/**
 * iOS-реализация Router — обёртка над NavHostController из Compose Navigation KMP.
 *
 * Зеркало RouterImpl из :app модуля (Android).
 * navController задаётся через LaunchedEffect в AppNavHost после создания NavHostController.
 */
internal class IosRouterImpl : Router {

    var navController: NavHostController? = null

    // Auth — на iOS пока нет отдельного флоу авторизации, переходим на Home
    override fun showSignIn() = navigate(HomeRoute.route)
    override fun showLogIn() = navigate(HomeRoute.route)
    override fun showStartScreen() = navigate(HomeRoute.route)
    override fun showHome(startingRoute: String) = navigate(HomeRoute.route)

    override fun showRouteForm(basicId: String?, isMakeCopy: Boolean) =
        navigate(FormRoute.buildRoute(basicId, isMakeCopy))

    override fun showRouteDetails(basicData: BasicData) =
        navigate(FormRoute.buildRoute(basicData.id))

    override fun showSettings() = navigate(SettingsScreenRoute.route)
    override fun showSearch() = navigate(SearchRoute.route)

    override fun back() { navController?.popBackStack() }
    override fun navigationUp(): Boolean = navController?.navigateUp() ?: false

    override fun showChangedLocoForm(locomotive: Locomotive) =
        navigate(FormLoco.buildRoute(locomotive.locoId, locomotive.basicId))

    override fun showEmptyLocoForm(basicId: String) =
        navigate(FormLoco.buildRoute(null, basicId))

    override fun showChangeTrainForm(train: Train) =
        navigate(FormTrain.buildRoute(train.trainId, train.basicId))

    override fun showEmptyTrainForm(basicId: String) =
        navigate(FormTrain.buildRoute(null, basicId))

    override fun showChangePassengerForm(passenger: Passenger) =
        navigate(FormPassenger.buildRoute(passenger.passengerId, passenger.basicId))

    override fun showEmptyPassengerForm(basicId: String) =
        navigate(FormPassenger.buildRoute(null, basicId))

    override fun showSelectReleaseDayScreen() = navigate(SelectReleaseDaysScreenRoute.route)
    override fun showPurchasesScreen() = navigate(PurchasesRoute.route)
    override fun showMoreInfo(monthOfYearId: String) = navigate(MoreInfoRoute.buildRoute(monthOfYearId))
    override fun showSalaryCalculation() = navigate(SalaryCalculationRoute.route)
    override fun showSettingSalary() = navigate(SettingSalaryRoute.route)
    override fun showAllRoute() = navigate(AllRouteScreenRoute.route)
    override fun showWorkScheduleScreen() = navigate(WorkScheduleScreenRoute.route)

    private fun navigate(route: String) {
        navController?.navigate(route)
    }
}
