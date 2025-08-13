package com.z_company.route.viewmodel.home_view_model

import ru.rustore.sdk.core.feature.model.FeatureAvailabilityResult

sealed class StartPurchasesEvent {
    data class PurchasesAvailability(val availability: FeatureAvailabilityResult) : StartPurchasesEvent()
    data class Error(val throwable: Throwable): StartPurchasesEvent()
}