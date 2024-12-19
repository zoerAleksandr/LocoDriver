package com.z_company.login.navigation

import androidx.compose.runtime.Composable
import com.z_company.domain.navigation.Router
import com.z_company.login.ui.FirstPresentationBlockScreen

@Composable
fun FirstPresentationBlockDestination(router: Router){
    FirstPresentationBlockScreen(
        onNextClick = router::showSignIn
    )
}