package com.example.data_local.route

import com.example.core.ResultState
import com.example.core.ResultState.Companion.flowMap
import com.example.core.ResultState.Companion.flowRequest
import com.example.data_local.route.dao.RouteDao
import com.example.data_local.route.entity_converters.RouteConverter
import com.example.domain.entities.route.Route
import com.example.domain.repositories.RouteRepositories
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID

class RoomRouteRepository : RouteRepositories, KoinComponent {
    private val dao: RouteDao by inject()
    override fun loadRoutes(): Flow<ResultState<List<Route>>> {
        return flowMap {
            dao.getAllRoute().map { routes ->
                ResultState.Success(routes.map { route ->
                    RouteConverter.toData(route)
                })
            }
        }
    }

    override fun loadRoute(routeId: String): Flow<ResultState<Route?>> {
        return flowMap {
            dao.getRouteById(routeId).map { route ->
                ResultState.Success(
                    route?.let { RouteConverter.toData(route) }
                )
            }
        }
    }

    override fun saveRoute(route: Route): Flow<ResultState<Unit>> {
        return flowRequest{
            if (route.basicData.id.isBlank()){
                route.basicData.id = UUID.randomUUID().toString()
            }
            route.locomotives.forEach {
                if (it.basicId.isBlank()){
                    it.basicId = route.basicData.id
                }
            }
            route.trains.forEach {
                if (it.baseId.isBlank()){
                    it.baseId = route.basicData.id
                }
            }
            route.passengers.forEach {
                if (it.baseId.isBlank()){
                    it.baseId = route.basicData.id
                }
            }
            route.notes?.let { notes ->
                if (notes.baseId.isBlank()){
                    notes.baseId = route.basicData.id
                }
            }
            dao.save(RouteConverter.fromData(route))
        }
    }

    override fun remove(route: Route): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.delete(RouteConverter.fromData(route))
        }
    }
}