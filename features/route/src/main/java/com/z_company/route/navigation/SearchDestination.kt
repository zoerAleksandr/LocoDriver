package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import com.z_company.domain.navigation.Router
import com.z_company.shared.ui.screen.SearchScreen as SharedSearchScreen

@Composable
fun SearchDestination(router: Router) {
    SharedSearchScreen(
        onBackClick = router::back,
        onRouteClick = { basicData -> router.showRouteForm(basicData.id) },
    )
}
