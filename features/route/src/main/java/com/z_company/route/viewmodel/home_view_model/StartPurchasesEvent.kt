package com.z_company.route.viewmodel.home_view_model

sealed class StartPurchasesEvent {
    object ShowPurchasesScreen : StartPurchasesEvent()
    data class Error(val throwable: Throwable) : StartPurchasesEvent()
}
