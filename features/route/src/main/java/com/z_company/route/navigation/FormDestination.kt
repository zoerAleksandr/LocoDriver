package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import com.z_company.domain.navigation.Router
import com.z_company.route.Const.NULLABLE_ID
import com.z_company.shared.ui.screen.FormRouteScreen as SharedFormRouteScreen

@Composable
fun FormDestination(
    router: Router,
    backStackEntry: NavBackStackEntry
) {
    val routeId = FormRoute.getRouteId(backStackEntry)?.takeIf { it != NULLABLE_ID }

    SharedFormRouteScreen(
        routeId = routeId,
        onBackClick = { router.showHome(HomeRoute.route) },
        onLocoClick = { locoId, basicId ->
            router.showChangedLocoForm(
                com.z_company.domain.entities.route.Locomotive(locoId = locoId, basicId = basicId)
            )
        },
        onNewLocoClick = { basicId -> router.showEmptyLocoForm(basicId) },
        onTrainClick = { trainId, basicId ->
            router.showChangeTrainForm(
                com.z_company.domain.entities.route.Train(trainId = trainId, basicId = basicId)
            )
        },
        onNewTrainClick = { basicId -> router.showEmptyTrainForm(basicId) },
        onPassengerClick = { passengerId, basicId ->
            router.showChangePassengerForm(
                com.z_company.domain.entities.route.Passenger(passengerId = passengerId, basicId = basicId)
            )
        },
        onNewPassengerClick = { basicId -> router.showEmptyPassengerForm(basicId) },
    )
}
