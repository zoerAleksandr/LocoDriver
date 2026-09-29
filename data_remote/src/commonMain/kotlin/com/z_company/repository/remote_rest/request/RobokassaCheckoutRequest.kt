package com.z_company.repository.remote_rest.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Тело запроса `POST /v1/payment/checkout` — серверный checkout Robokassa.
 *
 * Клиент передаёт только код тарифа: цену (со скидкой), срок и подпись
 * платежа формирует сервер, пароли мерчанта на клиенте не нужны.
 * `autoRenew` + `consentVersion` — согласие на автопродление: сервер пишет
 * его в журнал и выставляет первый платёж серии (`Recurring=true`).
 * `platform` определяет, куда страница возврата поведёт пользователя.
 */
@Serializable
data class RobokassaCheckoutRequest(
    @SerialName("tariff_code") val tariffCode: String,
    @SerialName("auto_renew") val autoRenew: Boolean = false,
    @SerialName("consent_version") val consentVersion: String? = null,
    val platform: String = "android",
)
