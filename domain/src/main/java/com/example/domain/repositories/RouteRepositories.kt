package com.example.domain.repositories

import com.example.core.ResultState
import com.example.domain.entities.route.Route
import kotlinx.coroutines.flow.Flow

interface RouteRepositories {
    fun loadRoutes(): Flow<ResultState<List<Route>>>

    fun loadRoute(routeId: String): Flow<ResultState<Route?>>

    fun saveRoute(route: Route): Flow<ResultState<Unit>>

    fun remove(route: Route): Flow<ResultState<Unit>>
}