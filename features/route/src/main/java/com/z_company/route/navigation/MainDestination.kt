package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import com.z_company.domain.navigation.Router
import com.z_company.route.ui.MainScreen

@Composable
fun MainDestination(router: Router){
    MainScreen(router)
}