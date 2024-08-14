package com.z_company.route.extention

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import ru.rustore.sdk.billingclient.RuStoreBillingClient
import ru.rustore.sdk.billingclient.model.purchase.Purchase

suspend fun Purchase.getEndTimeSubscription(billingClient: RuStoreBillingClient): Flow<Long> {
    return channelFlow {
        withContext(Dispatchers.IO) {
            val startTime = this@getEndTimeSubscription.purchaseTime?.time
            startTime?.let {
                val productId = this@getEndTimeSubscription.productId
                val product = billingClient.products.getProducts(listOf(productId)).await().first()
                product.subscription?.subscriptionPeriod?.days?.toLong()?.let {
                    val period = it * 86_400_000L
                    val endTime = startTime + period
                    trySend(endTime)
                }
            }
        }
    }
}