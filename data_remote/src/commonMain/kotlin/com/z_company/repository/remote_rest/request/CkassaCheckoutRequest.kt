package com.z_company.repository.remote_rest.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Тело запроса `POST /v1/payment/ckassa/checkout`.
 *
 * Клиент передаёт только код тарифа и платформу. Цену и срок сервер берёт из
 * таблицы тарифов (кабинет api.locodriver.ru), со скидками — подменить сумму
 * с клиента нельзя. `platform` нужна, чтобы в платежах было видно источник
 * оплаты (её нельзя протащить через `properties` CKassa — там только E_MAIL).
 */
@Serializable
data class CkassaCheckoutRequest(
    @SerialName("tariff_code") val tariffCode: String,
    val platform: String,
)
