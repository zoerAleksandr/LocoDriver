package com.z_company.data_remote

import com.z_company.domain.entities.route.BasicData

interface RemoteRouteRepository {
    suspend fun saveBasicData(basicData: BasicData)
    suspend fun getAllBasicData()
}