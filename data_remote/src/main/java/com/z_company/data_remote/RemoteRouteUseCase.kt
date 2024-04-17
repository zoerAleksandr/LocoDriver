package com.z_company.data_remote

import com.z_company.domain.entities.route.Route
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.entity_converter.BasicDataConverter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RemoteRouteUseCase(val repository: RemoteRouteRepository) : KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    suspend fun getAllBasicData() {
        repository.getAllBasicData()
    }

    private suspend fun saveBasicData(route: Route) {
        repository.saveBasicData(BasicDataConverter.fromData(route.basicData))
    }

     fun syncBasicData() {
        repository.synchronizedRoute()
    }

    private suspend fun removeBasicData(remoteObjectId: String) {
        repository.removeBasicData(remoteObjectId)
    }
}