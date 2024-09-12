package com.z_company.route.viewmodel

data class SalaryCalculationUIState(
    val paymentAtTariffHours: Long? = null,
    val paymentAtTariffMoney: Double? = null,
    val paymentAtPassengerHours: Long? = null,
    val paymentAtPassengerMoney: Double? = null,
    val paymentAtSingleLocomotiveHours: Long? = null,
    val paymentAtSingleLocomotiveMoney: Double? = null,

)