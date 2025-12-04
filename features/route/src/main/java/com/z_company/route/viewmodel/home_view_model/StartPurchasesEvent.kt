package com.z_company.route.viewmodel.home_view_model

import ru.rustore.sdk.pay.model.PurchaseAvailabilityResult


sealed class StartPurchasesEvent {
    data class PurchasesAvailability(val availability: PurchaseAvailabilityResult) : StartPurchasesEvent()
    data class Error(val throwable: Throwable): StartPurchasesEvent()
}