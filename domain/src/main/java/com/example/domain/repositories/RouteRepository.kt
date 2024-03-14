package com.example.domain.repositories

import com.example.core.ResultState
import com.example.domain.entities.route.Locomotive
import com.example.domain.entities.route.Passenger
import com.example.domain.entities.route.Photo
import com.example.domain.entities.route.Route
import com.example.domain.entities.route.Train
import kotlinx.coroutines.flow.Flow

interface RouteRepository {
    fun loadRoutes(): Flow<ResultState<List<Route>>>
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
    fun saveLocomotive(locomotive: Locomotive): Flow<ResultState<Unit>>
    fun saveTrain(train: Train): Flow<ResultState<Unit>>
    fun savePassenger(passenger: Passenger): Flow<ResultState<Unit>>
    fun savePhoto(photo: Photo): Flow<ResultState<Unit>>
}