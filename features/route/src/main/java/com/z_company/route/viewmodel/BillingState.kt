package com.z_company.route.viewmodel

import androidx.annotation.StringRes
import ru.rustore.sdk.billingclient.model.product.Product

data class BillingState(
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
    @StringRes val snackbarResId: Int? = null
) {
    val isEmpty: Boolean = products.isEmpty() && !isLoading
}