package com.z_company.data_local.route

import com.z_company.core.ResultState
import com.z_company.core.ResultState.Companion.flowMap
import com.z_company.core.ResultState.Companion.flowRequest
import com.z_company.data_local.route.dao.RouteDao
import com.z_company.data_local.route.entity_converters.*
import com.z_company.domain.entities.route.*
import com.z_company.domain.repositories.RouteRepository
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
                ResultState.Success(
                    routes.map { route ->
                        RouteConverter.toData(route)
                    }
                )
            }
        }
    }

    override fun loadRoutesWithDeleting(): List<Route> {
        return dao.getAllRouteWithDeleting().map {
            RouteConverter.toData(it)
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

    override fun loadPhoto(photoId: String): Flow<ResultState<Photo?>> {
        return flowMap {
            dao.getPhotoById(photoId).map { photo ->
                ResultState.Success(
                    photo?.let {
                        PhotoConverter.toData(photo)
                    }
                )
            }
        }
    }

    override fun loadPhotosByRoute(basicId: String): Flow<ResultState<List<Photo>>> {
        return flowMap {
            dao.getPhotosByRoute(basicId).map { photos ->
                ResultState.Success(
                    photos.map { photo ->
                        PhotoConverter.toData(photo)
                    }
                )
            }
        }
    }

    override fun saveRoute(route: Route): Flow<ResultState<Unit>> {
        return flowRequest {
            var newRoute = RouteConverter.fromData(route)
            if (route.basicData.id.isBlank()) {
                route.basicData.id = UUID.randomUUID().toString()
            }
            newRoute.locomotives.forEach {
                if (it.basicId.isBlank()) {
                    it.basicId = newRoute.basicData.id
                }
            }
            newRoute.trains.forEach {
                if (it.basicId.isBlank()) {
                    it.basicId = newRoute.basicData.id
                }
            }
            newRoute.passengers.forEach {
                if (it.basicId.isBlank()) {
                    it.basicId = newRoute.basicData.id
                }
            }
            newRoute.photos.forEach {
                if (it.basicId.isBlank()) {
                    it.basicId = newRoute.basicData.id
                }
            }
            newRoute = newRoute.copy(
                basicData = newRoute.basicData.copy(
                    isSynchronized = false
                )
            )
            dao.save(newRoute)
        }
    }

    override fun setRemoteObjectIdBasicData(
        basicId: String,
        remoteObjectId: String
    ): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.setRemoteObjectIdBasicData(basicId, remoteObjectId)
        }
    }

    override fun setRemoteObjectIdLocomotive(
        locoId: String,
        remoteObjectId: String
    ): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.setRemoteObjectIdLocomotive(locoId, remoteObjectId)
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

    override fun removePhoto(photo: Photo): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.deletePhoto(PhotoConverter.fromData(photo))
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

    override fun savePhoto(photo: Photo): Flow<ResultState<Unit>> {
        return flowRequest {
            if (photo.photoId.isBlank()) {
                photo.photoId = UUID.randomUUID().toString()
            }
            dao.savePhoto(PhotoConverter.fromData(photo))
        }
    }

    override fun markAsRemoved(route: Route): Flow<ResultState<Unit>> {
        val newRoute = route.copy(basicData = route.basicData.copy(isDeleted = true))
        return flowRequest {
            dao.save(RouteConverter.fromData(newRoute))
        }
    }

    override fun isSynchronizedBasicData(basicId: String): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.isSynchronizedRoute(basicId)
        }
    }
}