package com.z_company.domain.repositories

import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Photo
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Train
import kotlinx.coroutines.flow.Flow

interface RouteRepository {
    fun loadRoutes(): Flow<ResultState<List<Route>>>
    fun loadRoutesWithDeleting(): List<Route>
    fun loadRoute(routeId: String): Flow<ResultState<Route?>>
    fun loadLoco(locoId: String): Flow<ResultState<Locomotive?>>
    fun loadTrain(trainId: String): Flow<ResultState<Train?>>
    fun loadPassenger(passengerId: String): Flow<ResultState<Passenger?>>
    fun loadPhoto(photoId: String): Flow<ResultState<Photo?>>
    fun loadPhotosByRoute(basicId: String): Flow<ResultState<List<Photo>>>
    fun remove(route: Route): Flow<ResultState<Unit>>
    fun removeLoco(locomotive: Locomotive): Flow<ResultState<Unit>>
    fun removeTrain(train: Train): Flow<ResultState<Unit>>
    fun removePassenger(passenger: Passenger): Flow<ResultState<Unit>>
    fun removePhoto(photo: Photo): Flow<ResultState<Unit>>
    fun saveRoute(route: Route): Flow<ResultState<Unit>>
    fun isSynchronizedBasicData(basicId: String, remoteObjectId: String): Flow<ResultState<Unit>>
    fun isSynchronizedLocomotive(locoId: String, remoteObjectId: String): Flow<ResultState<Unit>>
    fun saveLocomotive(locomotive: Locomotive): Flow<ResultState<Unit>>
    fun saveTrain(train: Train): Flow<ResultState<Unit>>
    fun savePassenger(passenger: Passenger): Flow<ResultState<Unit>>
    fun savePhoto(photo: Photo): Flow<ResultState<Unit>>
    fun markAsRemoved(route: Route): Flow<ResultState<Unit>>
}