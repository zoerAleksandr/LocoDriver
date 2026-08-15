package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.z_company.domain.navigation.Router
import com.z_company.route.ui.PurchasesScreen
import com.z_company.route.viewmodel.PurchasesViewModel
import com.z_company.route.viewmodel.PullToSyncViewModel

@Composable
fun PurchasesDestination(
    router: Router
){
    val viewModel: PurchasesViewModel = viewModel()
    val pullToSyncViewModel: PullToSyncViewModel = viewModel()
    val pullToSyncState by pullToSyncViewModel.uiState.collectAsState()
    val state by viewModel.state.collectAsState()

    PurchasesScreen(
        viewModel = viewModel,
        billingState = state,
        onProductClick = viewModel::onProductClick,
        onBack = router::back,
        eventSharedFlow = viewModel.event,
        isPullRefreshing = pullToSyncState.isRefreshing,
        onPullRefresh = { pullToSyncViewModel.refresh(viewModel::refreshProductsAndPurchases) },
    )
}
