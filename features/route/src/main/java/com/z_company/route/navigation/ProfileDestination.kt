package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.z_company.domain.navigation.Router
import com.z_company.route.ui.ProfileScreen
import com.z_company.route.viewmodel.ProfileViewModel
import com.z_company.route.viewmodel.PullToSyncViewModel

@Composable
fun ProfileDestination(
    router: Router
){
    val viewModel: ProfileViewModel = viewModel()
    val pullToSyncViewModel: PullToSyncViewModel = viewModel()
    val pullToSyncState by pullToSyncViewModel.uiState.collectAsState()
    ProfileScreen(
        viewModel = viewModel,
        onBillingClick = router::showPurchasesScreen,
        isPullRefreshing = pullToSyncState.isRefreshing,
        onPullRefresh = { pullToSyncViewModel.refresh(viewModel::refresh) },
//        onLogOut = router::showSignIn,
    )
}
