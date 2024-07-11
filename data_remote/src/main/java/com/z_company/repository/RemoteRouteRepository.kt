package com.z_company.repository

import androidx.work.Data
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Route
import com.z_company.entity.Locomotive as LocomotiveRemote
import com.z_company.entity.BasicData as BasicDataRemote
import com.z_company.entity.Train as TrainRemote
import com.z_company.entity.Passenger as PassengerRemote
import com.z_company.entity.Photo as PhotoRemote
import kotlinx.coroutines.flow.Flow

interface RemoteRouteRepository {
    suspend fun loadBasicDataFromRemote(id: String): Flow<ResultState<BasicDataRemote?>>
    suspend fun saveRoute(route: Route): Flow<ResultState<Data>>
    suspend fun getAllBasicDataId(): Flow<ResultState<List<String>?>>
    suspend fun removeBasicData(remoteObjectId: String): Flow<ResultState<Data>>
    suspend fun synchronizedRoutePeriodic(): Flow<ResultState<Unit>>
    suspend fun synchronizedRouteOneTime(): Flow<ResultState<Unit>>
    suspend fun saveLocomotive(locomotive: LocomotiveRemote): Flow<ResultState<Data>>
    suspend fun removeLocomotive(remoteObjectId: String): Flow<ResultState<Data>>
    suspend fun removeTrain(remoteObjectId: String): Flow<ResultState<Data>>
    suspend fun removePassenger(remoteId: String): Flow<ResultState<Data>>
    suspend fun removePhoto(remoteId: String): Flow<ResultState<Data>>
    suspend fun loadLocomotiveFromRemote(basicId: String): Flow<ResultState<List<LocomotiveRemote>?>>
    suspend fun loadTrainFromRemote(basicId: String): Flow<ResultState<List<TrainRemote>?>>
    suspend fun loadPassengerFromRemote(basicId: String): Flow<ResultState<List<PassengerRemote>?>>
    suspend fun loadPhotoFromRemote(basicId: String): Flow<ResultState<List<PhotoRemote>?>>
}