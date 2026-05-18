package com.z_company.domain.repositories

import com.z_company.core.ResultState
import com.z_company.domain.entities.norma_time.StationNorm
import kotlinx.coroutines.flow.Flow

interface StationNormRepository {
    fun getAllFlow(): Flow<List<StationNorm>>
    fun getAll(): List<StationNorm>
    fun replaceAll(stations: List<StationNorm>): Flow<ResultState<Unit>>
}
