package com.z_company.data_remote

import androidx.work.Data
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Route
import com.z_company.entity.Locomotive
import com.z_company.entity.BasicData as BasicDataRemote
import kotlinx.coroutines.flow.Flow

interface RemoteRouteRepository {
    suspend fun saveRoute(route: Route): Flow<ResultState<Data>>
    suspend fun getAllBasicData(): Flow<ResultState<List<BasicDataRemote>?>>
    suspend fun removeBasicData(remoteObjectId: String): Flow<ResultState<Data>>
    fun synchronizedRoute()
    suspend fun saveLocomotive(locomotive: Locomotive): Flow<ResultState<Data>>
    suspend fun removeLocomotive(remoteObjectId: String): Flow<ResultState<Data>>
    suspend fun removeTrain(remoteObjectId: String): Flow<ResultState<Data>>
    suspend fun removePassenger(remoteId: String): Flow<ResultState<Data>>
    suspend fun removePhoto(remoteId: String): Flow<ResultState<Data>>
}