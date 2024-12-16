package com.z_company.domain.repositories

import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Photo
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Train
import kotlinx.coroutines.flow.Flow

interface RouteRepository {
    fun loadRoutesByPeriod(startPeriod: Long, endPeriod: Long): Flow<ResultState<List<Route>>>
    fun loadRoutesAsFlow(): Flow<ResultState<List<Route>>>
    fun loadRoutes(): List<Route>
    fun loadRoutesWithDeleting(): List<Route>
    fun loadRoute(routeId: String): Flow<ResultState<Route?>>
    fun loadLoco(locoId: String): Flow<ResultState<Locomotive?>>
    fun loadLocoListByBasicId(basicId: String): List<Locomotive>
    fun loadTrain(trainId: String): Flow<ResultState<Train?>>
    fun loadTrainListByBasicId(basicId: String): List<Train>
    fun loadPassenger(passengerId: String): Flow<ResultState<Passenger?>>
    fun loadPassengerListByBasicId(basicId: String): List<Passenger>
    fun loadPhoto(photoId: String): Flow<ResultState<Photo?>>
    fun loadPhotosByRoute(basicId: String): Flow<ResultState<List<Photo>>>
    fun remove(route: Route): Flow<ResultState<Unit>>
    fun removeLoco(locomotive: Locomotive): Flow<ResultState<Unit>>
    fun removeTrain(train: Train): Flow<ResultState<Unit>>
    fun removePassenger(passenger: Passenger): Flow<ResultState<Unit>>
    fun removePhoto(photo: Photo): Flow<ResultState<Unit>>
    fun saveRoute(route: Route): Flow<ResultState<Unit>>
    fun setRemoteObjectIdRoute(basicId: String, remoteObjectId: String?): Flow<ResultState<Unit>>
    fun setRemoteObjectIdBasicData(basicId: String, remoteObjectId: String?): Flow<ResultState<Unit>>
    fun setRemoteObjectIdLocomotive(locoId: String, remoteObjectId: String): Flow<ResultState<Unit>>
    fun setRemoteObjectIdTrain(trainId: String, remoteObjectId: String): Flow<ResultState<Unit>>
    fun setRemoteObjectIdPassenger(passengerId: String, objectId: String): Flow<ResultState<Unit>>
    fun setRemoteObjectIdPhoto(photoId: String, objectId: String): Flow<ResultState<Unit>>
    fun saveLocomotive(locomotive: Locomotive): Flow<ResultState<Unit>>
    fun saveTrain(train: Train): Flow<ResultState<Unit>>
    fun savePassenger(passenger: Passenger): Flow<ResultState<Unit>>
    fun savePhoto(photo: Photo): Flow<ResultState<Unit>>
    fun markAsRemoved(route: Route): Flow<ResultState<Unit>>
    fun setSynchronizedRoute(basicId: String): Flow<ResultState<Unit>>
    fun clearRepository(): Flow<ResultState<Unit>>
}