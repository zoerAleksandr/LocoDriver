package com.z_company.data_remote

import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.BasicData

class RemoteRouteUseCase(val repository: RemoteRouteRepository) {
    suspend fun getAllBasicData(route: Route) {
        return repository.getAllBasicData()
    }

    suspend fun saveBasicData(basicData: BasicData) {
        repository.saveBasicData(basicData)
    }
}