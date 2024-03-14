package com.example.route.viewmodel

import com.example.core.ResultState
import com.example.domain.entities.route.Passenger

data class PassengerFormUiState(
    val passengerDetailState: ResultState<Passenger?> = ResultState.Loading,
    val savePassengerState: ResultState<Unit>? = null,
    val errorTimeState: ResultState<Unit>? = null,
    val resultTime: Long? = null,
    val formValid: Boolean = true
)