package com.example.domain.repositories

import com.example.core.ResultState
import com.example.domain.entities.route.Locomotive
import com.example.domain.entities.route.Notes
import com.example.domain.entities.route.Passenger
import com.example.domain.entities.route.Route
import com.example.domain.entities.route.Train
import kotlinx.coroutines.flow.Flow

interface RouteRepositories {
    fun loadRoutes(): Flow<ResultState<List<Route>>>
    fun loadRoute(routeId: String): Flow<ResultState<Route?>>
    fun loadLoco(locoId: String): Flow<ResultState<Locomotive?>>
    fun saveRoute(route: Route): Flow<ResultState<Unit>>
    fun remove(route: Route): Flow<ResultState<Unit>>
    fun removeLoco(locomotive: Locomotive): Flow<ResultState<Unit>>
    fun removeTrain(train: Train): Flow<ResultState<Unit>>
    fun removePassenger(passenger: Passenger): Flow<ResultState<Unit>>
    fun removeNotes(notes: Notes): Flow<ResultState<Unit>>
    fun saveLocomotive(locomotive: Locomotive): Flow<ResultState<Unit>>
}