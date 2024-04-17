package com.z_company.data_remote

import androidx.work.Data
import com.z_company.core.ResultState
import com.z_company.entity.BasicData as BasicDataRemote
import kotlinx.coroutines.flow.Flow

interface RemoteRouteRepository {
    suspend fun saveBasicData(basicData: BasicDataRemote): Flow<ResultState<Data>>
    suspend fun getAllBasicData(): Flow<ResultState<List<BasicDataRemote>?>>
    suspend fun removeBasicData(remoteObjectId: String): Flow<ResultState<Data>>
    fun synchronizedRoute()
}