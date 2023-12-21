package com.example.domain.use_cases

import com.example.core.ErrorEntity
import com.example.core.ResultState
import com.example.domain.entities.route.Route
import com.example.domain.repositories.RouteRepositories
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class RouteUseCase(private val repository: RouteRepositories) {
    fun listRoutes(): Flow<ResultState<List<Route>>> {
        return repository.loadRoutes()
    }

    fun routeDetails(routeId: String): Flow<ResultState<Route?>> {
        return repository.loadRoute(routeId)
    }

    fun removeRoute(route: Route): Flow<ResultState<Unit>> {
        return repository.remove(route)
    }

    fun saveRoute(route: Route): Flow<ResultState<Unit>> {
        return if (isRouteValid(route)) {
            repository.saveRoute(route)
        } else {
            flowOf(
                ResultState.Error(
                    ErrorEntity(IllegalStateException(), "-1", "Route is not valid.")
                )
            )
        }
    }

    private fun isRouteValid(route: Route): Boolean {
        // TODO("Not yet implemented")
        return true
    }
}