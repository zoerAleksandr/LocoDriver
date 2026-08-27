package com.z_company.route.viewmodel

data class DialogRestUiState(
    val minTimeDuration: Long? = null,
    val fullTimeDuration: Long? = null,
    val timeEndMinTimeRestPointOfTurnover:  Long? = null,
    val timeEndFullTimeRestPointOfTurnover:  Long? = null,

    val homeRestDuration: Long? = null,
    val timeEndHomeRest: Long? = null,
    val timeEndMinHomeRest: Long? = null,

    // Фактический отдых до следующей явки (по расписанию), если он есть.
    val actualRestDuration: Long? = null,
    val timeEndActualRest: Long? = null,
)
