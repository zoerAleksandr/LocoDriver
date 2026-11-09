package com.z_company.route.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.navigation
import com.z_company.core.ui.navigation.composableScreen
import com.z_company.domain.navigation.Router

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.homeGraph(
    router: Router,
) {
    navigation(
        route = HomeFeature.route,
        startDestination = HomeRoute.route
    ) {
        // главное меню
        composableScreen (
            route = HomeRoute.route,
        ) {
            HomeDestination(router = router)
        }
        composableScreen(
            route = SalaryCalculationRoute.route
        ) {
            SalaryCalculationDestination(router = router)
        }
        composableScreen(
            route = FormRoute.route,
            arguments = FormRoute.navArguments
        ) { backStackEntry ->
            FormDestination(router = router, backStackEntry = backStackEntry)
        }
        composableScreen(
            route = SettingsScreenRoute.route,
            arguments = SettingsScreenRoute.navArguments
        ) { backStackEntry ->
            SettingDestination(router = router, backStackEntry = backStackEntry)
        }

        composableScreen(ProfileRoute.route) {
            ProfileDestination(router = router)
        }

        composableScreen(DataPrivacyRoute.route) {
            com.z_company.route.ui.DataPrivacyScreen(onBack = { router.back() })
        }

        composableScreen(MyCertificatesRoute.route) {
            com.z_company.route.ui.MyCertificatesScreen(
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
                onBack = { router.back() },
                onOpen = { item ->
                    router.showBrakeCertificate(com.z_company.domain.brake.BrakePrefill(routeId = item.routeId, trainId = item.trainId))
                },
            )
        }

        composableScreen(OfflineSectionsRoute.route) {
            com.z_company.route.ui.OfflineSectionsScreen(
                viewModel = org.koin.androidx.compose.koinViewModel(),
                onBack = { router.back() },
            )
        }

        composableScreen(TrackHistoryRoute.route) {
            val showPurchases = rememberShowPurchasesScreen(router)
            com.z_company.route.ui.TrackHistoryScreen(
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
                onBack = { router.back() },
                onOpenDataPrivacy = router::showDataPrivacy,
                onOpenPurchases = showPurchases,
            )
        }

        composableScreen(
            route = FormLoco.route,
            arguments = FormLoco.navArguments,
        ) { backStackEntry ->
            FormLocoDestination(router = router, backStackEntry = backStackEntry)
        }

        composableScreen(
            route = FormTrain.route,
            arguments = FormTrain.navArguments,
        ) { backStackEntry ->
            FormTrainDestination(router = router, backStackEntry = backStackEntry)
        }

        composableScreen(
            route = FormPassenger.route,
            arguments = FormPassenger.navArguments
        ) { backStackEntry ->
            FormPassengerDestination(router = router, backStackEntry = backStackEntry)
        }
        composableScreen(
            route = FormOtherWork.route,
            arguments = FormOtherWork.navArguments
        ) { backStackEntry ->
            FormOtherWorkDestination(router = router, backStackEntry = backStackEntry)
        }
        composableScreen(
            route = PartnersManageRoute.route
        ) {
            PartnersManageDestination(router = router)
        }
        composableScreen(
            route = PartnerPickerRoute.route,
            arguments = PartnerPickerRoute.navArguments
        ) { backStackEntry ->
            PartnerPickerDestination(router = router, backStackEntry = backStackEntry)
        }
        composableScreen(
            route = PartnerEditRoute.route,
            arguments = PartnerEditRoute.navArguments
        ) { backStackEntry ->
            PartnerEditDestination(router = router, backStackEntry = backStackEntry)
        }
        composableScreen(
            route = SearchRoute.route
        ) {
            SearchDestination(router = router)
        }
        composableScreen(
            route = PurchasesRoute.route
        ) {
            PurchasesDestination(router = router)
        }
        composableScreen(
            route = SettingSalaryRoute.route
        ) {
            SettingSalaryDestination(router = router)
        }
        composableScreen(
            route = UpdatePresentationBlockRoute.route
        ){
            UpdatePresentationBlockDestination(router = router)
        }
        composableScreen(
            route = AllRouteScreenRoute.route
        ){
            AllRouteScreenDestination(router = router)
        }
        composableScreen(SelectReleaseDaysScreenRoute.route) {
            SelectReleaseDaysDestination(router = router)
        }
        composableScreen(StatisticsRoute.route) {
            StatisticsDestination(router = router)
        }
        composableScreen(
            route = BrakeCertificateRoute.route,
            arguments = BrakeCertificateRoute.navArguments,
        ) { backStackEntry ->
            BrakeCertificateDestination(router = router, backStackEntry = backStackEntry)
        }
        composableScreen(CalendarRoute.route) {
            CalendarDestination(router = router)
        }
        composableScreen(ScheduleWizardRoute.route) {
            ScheduleWizardDestination(router = router)
        }
        composableScreen(AbsenceRoute.route) {
            AbsenceDestination(router = router)
        }
    }
}