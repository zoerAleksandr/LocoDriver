package com.z_company.repository.remote_rest.response

import kotlinx.serialization.Serializable

/**
 * DTO ответа `GET /v1/tariffs` — единый источник цен подписки для приложения
 * и сайта. Соответствует `TariffsResponse` на сервере.
 *
 * Цены и скидки задаются в кабинете администратора; при офлайне клиент
 * откатывается к дефолтным тарифам (см. PurchasesViewModel).
 */
@Serializable
data class TariffsResponse(
    val currency: String = "RUB",
    val tariffs: List<TariffResponse> = emptyList(),
)

@Serializable
data class TariffResponse(
    // Стабильный код тарифа ('month' | 'quarter' | 'year'); передаётся в оплату
    // как shp_tariff_code — по нему сервер начисляет срок подписки.
    val code: String,
    val title: String,
    val desc: String = "",
    val periodDays: Int,
    // Базовая («старая») цена — показывается зачёркнутой при активной скидке.
    val basePrice: Double,
    // Итоговая цена к оплате (со скидкой либо равна basePrice).
    val price: Double,
    val discountPercent: Int = 0,
    val discountActive: Boolean = false,
    // ms Unix epoch окончания акции (или null = бессрочно/нет скидки).
    val discountUntil: Long? = null,
    val currency: String = "RUB",
)
