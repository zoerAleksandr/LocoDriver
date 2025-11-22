package com.z_company.route.navigation.login

import androidx.compose.runtime.Composable
import com.z_company.domain.navigation.Router
import com.z_company.route.ui.login.FirstPresentationBlockScreen

@Composable
fun FirstPresentationBlockDestination(router: Router){
    FirstPresentationBlockScreen(
        onNextClick = router::showSignIn
    )
}