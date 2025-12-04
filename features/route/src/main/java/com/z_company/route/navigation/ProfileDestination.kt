package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.z_company.domain.navigation.Router
import com.z_company.route.ui.ProfileScreen
import com.z_company.route.viewmodel.ProfileViewModel

@Composable
fun ProfileDestination(
    router: Router
){
    val viewModel: ProfileViewModel = viewModel()
    ProfileScreen(
        viewModel = viewModel,
        onBillingClick = router::showPurchasesScreen,
        onLogOut = router::showSignIn
    )
}