package com.z_company.repository.remote_rest.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Ответ `POST /v1/payment/checkout`: готовая подписанная ссылка оплаты Robokassa. */
@Serializable
data class RobokassaCheckoutResponse(
    @SerialName("payment_url") val paymentUrl: String,
    @SerialName("invoice_id") val invoiceId: Long? = null,
)
