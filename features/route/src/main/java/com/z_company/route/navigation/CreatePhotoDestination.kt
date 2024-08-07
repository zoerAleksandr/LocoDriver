package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import com.z_company.domain.navigation.Router
import com.z_company.route.ui.CameraXDemo

@Composable
fun CreatePhotoDestination(
    router: Router,
    backStackEntry: NavBackStackEntry
) {
    CameraXDemo()
}