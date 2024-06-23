package com.z_company.settings.navigation

import androidx.compose.runtime.Composable
import com.z_company.domain.navigation.Router
import com.z_company.settings.ui.SelectReleaseDaysScreen

@Composable
fun SelectReleaseDaysDestination(router: Router) {
    SelectReleaseDaysScreen(
        onBack = router::back,
        onSaveClick = {}
    )
}
