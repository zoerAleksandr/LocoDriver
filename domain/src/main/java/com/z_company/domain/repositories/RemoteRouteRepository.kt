package com.z_company.domain.repositories

import com.z_company.core.ResultState
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Route
import kotlinx.coroutines.flow.Flow

interface RemoteRouteRepository {
    fun saveBasicData(basicData: BasicData)
    fun getAllBasicData()
    fun syncData()
}