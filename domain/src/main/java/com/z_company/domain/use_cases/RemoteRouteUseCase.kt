package com.z_company.domain.use_cases

import com.z_company.domain.entities.route.Route
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.repositories.RemoteRouteRepository
import kotlinx.coroutines.flow.Flow

class RemoteRouteUseCase(val repository: RemoteRouteRepository) {
    fun saveRoute(route: Route): Flow<ResultState<Unit>> {
        return repository.saveRoute(route)
    }
    fun saveBasicData(basicData: BasicData): Flow<ResultState<Unit>> {
        return repository.saveBasicData(basicData)
    }
}