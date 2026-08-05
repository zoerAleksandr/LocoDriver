package com.z_company.route.viewmodel

data class PreviewRouteUiState(
    /** Расчётный домашний отдых — абсолютный момент окончания (для «до …»). */
    val homeRest: Long? = null,
    /** Фактический отдых (до следующей явки): продолжительность. */
    val actualRestDuration: Long? = null,
    /** Фактический отдых: момент следующей явки (для «до …»). */
    val actualRestUntil: Long? = null,
    /** Форматированная оплата за смену (для быстрого просмотра на Главном). */
    val paymentText: String? = null,
)
