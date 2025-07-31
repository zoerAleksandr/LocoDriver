package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.z_company.domain.navigation.Router
import com.z_company.route.ui.PurchasesScreen
import com.z_company.route.viewmodel.PurchasesViewModel

@Composable
fun PurchasesDestination(
    router: Router
){
    val viewModel: PurchasesViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    PurchasesScreen(
        billingState = state,
        onProductClick = viewModel::onProductClick,
        onBack = router::back,
        eventSharedFlow = viewModel.event,
        dateAndTimeConverter = state.dateAndTimeConverter
    )
}