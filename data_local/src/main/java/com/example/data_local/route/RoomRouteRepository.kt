package com.example.data_local.route

import com.example.core.ResultState
import com.example.core.ResultState.Companion.flowMap
import com.example.core.ResultState.Companion.flowRequest
import com.example.data_local.route.dao.RouteDao
import com.example.data_local.route.entity_converters.*
import com.example.domain.entities.route.*
import com.example.domain.repositories.RouteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID

class RoomRouteRepository : RouteRepository, KoinComponent {
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
                    loco?.let {
                        LocomotiveConverter.toData(loco)
                    }
                )
            }
        }
    }

    override fun loadTrain(trainId: String): Flow<ResultState<Train?>> {
        return flowMap {
            dao.getTrainById(trainId).map { train ->
                ResultState.Success(
                    train?.let {
                        TrainConverter.toData(train)
                    }
                )
            }
        }
    }

    override fun loadPassenger(passengerId: String): Flow<ResultState<Passenger?>> {
        return flowMap {
            dao.getPassengerById(passengerId).map { passenger ->
                ResultState.Success(
                    passenger?.let {
                        PassengerConverter.toData(passenger)
                    }
                )
            }
        }
    }

    override fun loadNotes(notesId: String): Flow<ResultState<Notes?>> {
        return flowMap {
            dao.getNotesById(notesId).map { notes ->
                ResultState.Success(
                    notes?.let {
                        NotesConverter.toData(notes)
                    }
                )
            }
        }
    }

    override fun saveRoute(route: Route): Flow<ResultState<Unit>> {
        return flowRequest {
            if (route.basicData.id.isBlank()) {
                route.basicData.id = UUID.randomUUID().toString()
            }
            route.locomotives.forEach {
                if (it.basicId.isBlank()) {
                    it.basicId = route.basicData.id
                }
            }
            route.trains.forEach {
                if (it.basicId.isBlank()) {
                    it.basicId = route.basicData.id
                }
            }
            route.passengers.forEach {
                if (it.baseId.isBlank()) {
                    it.baseId = route.basicData.id
                }
            }
            route.notes?.let { notes ->
                if (notes.baseId.isBlank()) {
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
            if (locomotive.locoId.isBlank()) {
                locomotive.locoId = UUID.randomUUID().toString()
            }
            dao.saveLocomotive(LocomotiveConverter.fromData(locomotive))
        }
    }

    override fun saveTrain(train: Train): Flow<ResultState<Unit>> {
        return flowRequest {
            if (train.trainId.isBlank()) {
                train.trainId = UUID.randomUUID().toString()
            }
            dao.saveTrain(TrainConverter.fromData(train))
        }
    }

    override fun savePassenger(passenger: Passenger): Flow<ResultState<Unit>> {
        return flowRequest {
            if (passenger.passengerId.isBlank()) {
                passenger.passengerId = UUID.randomUUID().toString()
            }
            dao.savePassenger(PassengerConverter.fromData(passenger))
        }
    }

    override fun saveNotes(notes: Notes): Flow<ResultState<Unit>> {
        return flowRequest {
            if (notes.notesId.isBlank()) {
                notes.notesId = UUID.randomUUID().toString()
            }
            dao.saveNotes(NotesConverter.fromData(notes))
        }
    }
}