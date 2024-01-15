package com.example.route.viewmodel

import com.example.core.ResultState
import com.example.domain.entities.route.Locomotive
import com.example.domain.entities.route.Notes
import com.example.domain.entities.route.Passenger
import com.example.domain.entities.route.Route
import com.example.domain.entities.route.Train

data class RouteFormUiState(
    val routeDetailState : ResultState<Route?> = ResultState.Loading,
    val locoListState: ResultState<List<Locomotive>> = ResultState.Success(listOf(Locomotive())),
    val trainListState: ResultState<List<Train>> = ResultState.Success(listOf(Train())),
    val passengerListState: ResultState<List<Passenger>> = ResultState.Success(listOf(Passenger())),
    val notesState: ResultState<Notes> = ResultState.Success(Notes()),
    val saveRouteState : ResultState<Unit>? = null,
    val errorMessage: String? = null,
    val minTimeRest: Long? = null,
    val fullTimeRest: Long? = null
)
