package com.z_company.repository.remote_rest.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO ответа `POST /v1/payment/ckassa/checkout`.
 *
 * `paymentUrl` — ссылка на хостовую страницу оплаты CKassa
 * (например `https://bc.ckassa.ru/xxxx`), которую клиент открывает в браузере.
 */
@Serializable
data class CkassaCheckoutResponse(
    @SerialName("payment_url") val paymentUrl: String,
    @SerialName("tariff_code") val tariffCode: String? = null,
)
