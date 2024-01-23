package com.example.data_local.route

import com.example.core.ResultState
import com.example.core.ResultState.Companion.flowMap
import com.example.core.ResultState.Companion.flowRequest
import com.example.data_local.route.dao.RouteDao
import com.example.data_local.route.entity_converters.LocomotiveConverter
import com.example.data_local.route.entity_converters.NotesConverter
import com.example.data_local.route.entity_converters.PassengerConverter
import com.example.data_local.route.entity_converters.RouteConverter
import com.example.data_local.route.entity_converters.TrainConverter
import com.example.domain.entities.route.Locomotive
import com.example.domain.entities.route.Notes
import com.example.domain.entities.route.Passenger
import com.example.domain.entities.route.Route
import com.example.domain.entities.route.Train
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

    override fun loadLoco(locoId: String): Flow<ResultState<Locomotive?>> {
        return flowMap {
            dao.getLocoById(locoId).map { loco ->
                ResultState.Success(
                    loco?.let { LocomotiveConverter.toData(loco) }
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

    override fun removeLoco(locomotive: Locomotive): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.deleteLocomotives(LocomotiveConverter.fromData(locomotive))
        }
    }

    override fun removeTrain(train: Train): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.deleteTrain(TrainConverter.fromData(train))
        }
    }

    override fun removePassenger(passenger: Passenger): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.deletePassenger(PassengerConverter.fromData(passenger))
        }
    }

    override fun removeNotes(notes: Notes): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.deleteNotes(NotesConverter.fromData(notes))
        }
    }

    override fun saveLocomotive(locomotive: Locomotive): Flow<ResultState<Unit>> {
        return flowRequest {
            if (locomotive.locoId.isBlank()){
                locomotive.locoId = UUID.randomUUID().toString()
            }
            locomotive.basicId.let {
                locomotive.basicId = ""
            }
            dao.saveLocomotive(LocomotiveConverter.fromData(locomotive))
        }
    }
}