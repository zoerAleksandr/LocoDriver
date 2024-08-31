package com.z_company.route.viewmodel

import com.z_company.core.ResultState

data class DialogRestUiState(
    val minTimeRestPointOfTurnover: Long? = null,
    val minTimeHomeRest: Long? = null,
    val minUntilTimeRestPointOfTurnover:  ResultState<Long?> = ResultState.Loading,
    val fullUntilTimeRestPointOfTurnover:  ResultState<Long?> = ResultState.Loading,
    val untilTimeHomeRest: ResultState<Long?> = ResultState.Loading
)
