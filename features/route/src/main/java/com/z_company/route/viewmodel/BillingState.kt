package com.z_company.route.viewmodel

import androidx.annotation.StringRes
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.SubscriptionDetails
import ru.rustore.sdk.billingclient.model.product.Product

data class BillingState(
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
    val subscriptions: List<SubscriptionDetails> = emptyList(),
    val dateAndTimeConverter: DateAndTimeConverter? = null,
    @StringRes val snackbarResId: Int? = null,
) {
    val isEmpty: Boolean = products.isEmpty() && !isLoading
}