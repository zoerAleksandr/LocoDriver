package com.z_company.route.viewmodel

data class DialogRestUiState(
    val minTimeDuration: Long? = null,
    val fullTimeDuration: Long? = null,
    val timeEndMinTimeRestPointOfTurnover:  Long? = null,
    val timeEndFullTimeRestPointOfTurnover:  Long? = null,
    // Второй отдых в ПО подряд — в шторке показывается предупреждение,
    // а короткий/полный считаются от второго минимума.
    val isSecondTurnaroundRest: Boolean = false,

    val homeRestDuration: Long? = null,
    val timeEndHomeRest: Long? = null,
    val timeEndMinHomeRest: Long? = null,

    // Фактический отдых до следующей явки (по расписанию), если он есть.
    val actualRestDuration: Long? = null,
    val timeEndActualRest: Long? = null,
)
