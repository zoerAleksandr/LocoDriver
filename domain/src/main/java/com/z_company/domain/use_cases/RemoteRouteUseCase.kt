package com.z_company.domain.use_cases

import com.z_company.domain.entities.route.Route
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.repositories.RemoteRouteRepository
import kotlinx.coroutines.flow.Flow

class RemoteRouteUseCase(val repository: RemoteRouteRepository) {
    fun getAllBasicData(route: Route) {
        return repository.getAllBasicData()
    }
    fun saveBasicData(basicData: BasicData) {
         repository.saveBasicData(basicData)
    }
    fun syncData() {
        repository.syncData()
    }
}