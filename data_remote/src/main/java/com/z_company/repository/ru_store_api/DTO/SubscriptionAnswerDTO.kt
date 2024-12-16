package com.z_company.repository.ru_store_api.DTO

data class SubscriptionAnswerDTO(
    val startTimeMillis: String,
    val expiryTimeMillis: String,
    val autoRenewing: Boolean,
    val priceCurrencyCode: String,
    val priceAmountMicros: String,
    val countryCode: String,
    val paymentState: Int,
    val orderId: String,
    val acknowledgementState: Int,
    val kind: String,
    val purchaseType: Int,
    val introductoryPriceInfo: String
)

data class IntroductoryPriceInfo(
    val introductoryPriceCurrencyCode: String,
    val introductoryPriceAmountMicros: String,
    val introductoryPricePeriod: String,
    val introductoryPriceCycles: String
)
