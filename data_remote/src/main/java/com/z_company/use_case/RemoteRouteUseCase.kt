package com.z_company.use_case

import com.z_company.core.ResultState
import com.z_company.repository.RemoteRouteRepository
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent

class RemoteRouteUseCase(private val repository: RemoteRouteRepository) : KoinComponent {

    suspend fun syncBasicDataPeriodic(): Flow<ResultState<Unit>> {
        return repository.synchronizedRoutePeriodic()
    }

    suspend fun cancelingSync() {
        repository.cancelingSync()
    }
}