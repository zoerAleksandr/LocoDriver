package com.z_company.data_local.route

import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.core.ResultState.Companion.flowMap
import com.z_company.core.ResultState.Companion.flowRequest
import com.z_company.data_local.route.dao.RouteDao
import com.z_company.data_local.route.entity_converters.*
import com.z_company.domain.entities.route.*
import com.z_company.domain.repositories.RouteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID

class RoomRouteRepository : RouteRepository, KoinComponent {
    private val dao: RouteDao by inject()
    override fun loadRoutesByPeriod(
        startPeriod: Long,
        endPeriod: Long
    ): Flow<ResultState<List<Route>>> {
        return flowMap {
            dao.getAllRouteByPeriod(startPeriod, endPeriod).map { routes ->
                ResultState.Success(
                    routes.map { route ->
                        RouteConverter.toData(route)
                    }
                )
            }
        }
    }

    override fun loadRoutesAsFlow(): Flow<ResultState<List<Route>>> {
        return flowMap {
            dao.getAllRouteAsFlow().map { routes ->
                ResultState.Success(
                    routes.map { route ->
                        RouteConverter.toData(route)
                    }
                )
            }
        }
    }

    override fun loadRoutes(): List<Route> {
        return dao.getAllRoute().map {
            RouteConverter.toData(it)
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

    override fun loadLocoListByBasicId(basicId: String): List<Locomotive> {
        return dao.getLocoListByBasicId(basicId).map { locomotive ->
            LocomotiveConverter.toData(locomotive)
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

    override fun loadTrainListByBasicId(basicId: String): List<Train> {
        return dao.getTrainListByBasicId(basicId).map { train ->
            TrainConverter.toData(train)
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

    override fun loadPassengerListByBasicId(basicId: String): List<Passenger> {
        return dao.getPassengerListByBasicId(basicId).map { passenger ->
            PassengerConverter.toData(passenger)
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
            val newRoute = RouteConverter.fromData(route)
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
            dao.save(newRoute)
        }
    }

    override fun setRemoteObjectIdRoute(
        basicId: String,
        remoteRouteId: String?
    ): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.setRemoteObjectIdRoute(basicId, remoteRouteId)
        }
    }

    override fun setRemoteObjectIdBasicData(
        basicId: String,
        remoteObjectId: String?
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

    override fun setRemoteObjectIdTrain(
        trainId: String,
        remoteObjectId: String
    ): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.setRemoteObjectIdTrain(trainId, remoteObjectId)
        }
    }

    override fun setRemoteObjectIdPassenger(
        passengerId: String,
        objectId: String
    ): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.setRemoteObjectIdPassenger(passengerId, objectId)
        }
    }

    override fun setRemoteObjectIdPhoto(
        photoId: String,
        objectId: String
    ): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.setRemoteObjectIdPhoto(photoId, objectId)
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

    override fun setSynchronizedRoute(basicId: String): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.setSynchronizedRoute(basicId)
        }
    }

//    override fun setSchemaVersion(version: Int, id: String): Flow<ResultState<Unit>> {
//        return flowRequest {
//            dao.setSchemaVersion(version, id)
//        }
//    }

    override fun clearRepository(): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.clearRepository()
        }
    }

    override fun setFavoriteRoute(basicId: String, isFavorite: Boolean): Flow<ResultState<Boolean>> {
        return flow {
            emit(ResultState.Loading)
            dao.setFavorite(basicId, isFavorite)
            emit(ResultState.Success(isFavorite))
        }.catch {
            emit(ResultState.Error(ErrorEntity(it)))
        }.flowOn(Dispatchers.IO)
    }
}