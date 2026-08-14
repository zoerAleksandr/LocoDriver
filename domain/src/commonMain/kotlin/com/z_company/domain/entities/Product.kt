package com.z_company.domain.entities

/**
 * Тариф подписки для экрана покупок.
 *
 * Цены приходят с сервера (`GET /v1/tariffs`) и задаются в кабинете
 * администратора; при офлайне используется дефолтный набор.
 *
 * @param name    Название тарифа («1 месяц» / «3 месяца» / «1 год»).
 * @param desc    Подпись тарифа («Новичок» / «Эксперт» / «Профи»).
 * @param sum     Итоговая цена к оплате (со скидкой, если активна). Именно она
 *                уходит в Robokassa и показывается крупно.
 * @param code    Код тарифа для оплаты (shp_tariff_code) — по нему сервер
 *                начисляет срок подписки. Пусто у legacy-дефолтов.
 * @param periodDays Длительность подписки в днях (для расчёта «за месяц»).
 * @param basePrice  Базовая («старая») цена — зачёркнута при активной скидке.
 * @param discountPercent Процент скидки (0 — скидки нет).
 * @param discountActive  Активна ли скидка сейчас (учитывает срок акции).
 * @param discountUntil   ms Unix epoch окончания акции, либо null.
 */
data class Product(
    val name: String,
    val desc: String,
    val sum: Double,
    val code: String = "",
    val periodDays: Int = 0,
    val basePrice: Double = sum,
    val discountPercent: Int = 0,
    val discountActive: Boolean = false,
    val discountUntil: Long? = null,
)
