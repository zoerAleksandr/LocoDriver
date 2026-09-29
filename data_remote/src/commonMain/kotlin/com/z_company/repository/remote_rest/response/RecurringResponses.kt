package com.z_company.repository.remote_rest.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `GET /v1/payment/recurring/terms?tariff_code=…` — текст согласия на
 * автопродление для выбранного тарифа. Текст формирует сервер (его же он
 * сохраняет в журнал согласий), клиент показывает его без изменений.
 * `available=false` — услуга выключена на сервере, чекбокс не показываем.
 */
@Serializable
data class RecurringTermsResponse(
    val available: Boolean = false,
    @SerialName("tariff_code") val tariffCode: String = "",
    // Первый платёж (с акцией) и сумма последующих автосписаний (без неё).
    val amount: Double = 0.0,
    @SerialName("renewal_amount") val renewalAmount: Double? = null,
    @SerialName("period_days") val periodDays: Int = 0,
    @SerialName("consent_version") val consentVersion: String = "",
    @SerialName("consent_text") val consentText: String = "",
    @SerialName("offer_url") val offerUrl: String = "",
)

/**
 * `GET /v1/payment/recurring` и ответ `POST /v1/payment/recurring/disable` —
 * состояние автопродления пользователя. Даты — ms epoch.
 */
@Serializable
data class RecurringStatusResponse(
    val available: Boolean = false,
    val enabled: Boolean = false,
    @SerialName("tariff_code") val tariffCode: String? = null,
    val amount: Double? = null,
    @SerialName("next_charge_at") val nextChargeAt: Long? = null,
    @SerialName("last_charge_at") val lastChargeAt: Long? = null,
    // "paid" | "failed" | null
    @SerialName("last_charge_status") val lastChargeStatus: String? = null,
    @SerialName("last_error") val lastError: String? = null,
    // "user" | "charge_failed" | null
    @SerialName("disabled_reason") val disabledReason: String? = null,
)
