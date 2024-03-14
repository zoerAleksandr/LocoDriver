package com.z_company.route.viewmodel

import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Passenger

data class PassengerFormUiState(
    val passengerDetailState: ResultState<Passenger?> = ResultState.Loading,
    val savePassengerState: ResultState<Unit>? = null,
    val errorTimeState: ResultState<Unit>? = null,
    val resultTime: Long? = null,
    val formValid: Boolean = true
)