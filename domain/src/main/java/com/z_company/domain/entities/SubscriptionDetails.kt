package com.z_company.domain.entities

data class SubscriptionDetails(
    val productId: String,
    val title: String?,
    val description: String?,
    val priceLabel: String?,
    val expiryTime: String,
)
