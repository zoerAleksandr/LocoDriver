package com.example.route.viewmodel

import com.example.core.ResultState
import com.example.domain.entities.route.Locomotive
import com.example.domain.entities.route.Notes
import com.example.domain.entities.route.Passenger
import com.example.domain.entities.route.Route
import com.example.domain.entities.route.Train

data class RouteFormUiState(
    val routeDetailState : ResultState<Route?> = ResultState.Loading,
    val locoListState: ResultState<MutableList<Locomotive>?> = ResultState.Loading,
    val trainListState: ResultState<MutableList<Train>?> = ResultState.Loading,
    val passengerListState: ResultState<MutableList<Passenger>?> = ResultState.Loading,
    val notesState: ResultState<Notes?> = ResultState.Loading,
    val saveRouteState : ResultState<Unit>? = null,
    val errorMessage: String? = null,
    val minTimeRest: Long? = null,
    val fullTimeRest: Long? = null
)
