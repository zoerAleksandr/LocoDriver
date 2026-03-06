package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import com.z_company.domain.navigation.Router
import com.z_company.route.Const.NULLABLE_ID
import com.z_company.shared.ui.screen.FormTrainScreen as SharedFormTrainScreen

@Composable
fun FormTrainDestination(
    router: Router,
    backStackEntry: NavBackStackEntry
) {
    val trainId = FormTrain.getTrainId(backStackEntry)?.takeIf { it != NULLABLE_ID }
    val basicId = FormTrain.getBasicId(backStackEntry) ?: ""

    SharedFormTrainScreen(
        trainId = trainId,
        basicId = basicId,
        onBackClick = router::back,
    )
}
