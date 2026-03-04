package com.z_company.shared.platform

import com.z_company.domain.entities.Product
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-specific billing provider.
 *
 * Android: Robokassa payment SDK.
 * iOS: placeholder with URL-based payment (StoreKit later).
 */
interface BillingProvider {
    /** Available products for purchase. */
    val products: StateFlow<List<Product>>

    /** Subscription end time in epoch millis. 0 if none. */
    val purchasesEndTime: StateFlow<Long>

    /** Loading indicator. */
    val isLoading: StateFlow<Boolean>

    /** Initiate purchase flow for given product. */
    fun purchase(product: Product)

    /** Restore purchases from server. */
    fun restorePurchases()

    /** Refresh products and subscription state. */
    fun refresh()
}

/**
 * Factory function. Each platform provides its own implementation.
 */
expect fun createBillingProvider(): BillingProvider
