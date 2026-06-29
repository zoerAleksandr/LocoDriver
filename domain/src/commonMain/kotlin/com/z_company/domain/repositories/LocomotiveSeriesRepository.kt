package com.z_company.domain.repositories

import com.z_company.core.ResultState
import com.z_company.domain.entities.norma_time.LocomotiveSeries
import kotlinx.coroutines.flow.Flow

interface LocomotiveSeriesRepository {
    fun getAllFlow(): Flow<List<LocomotiveSeries>>
    fun getAll(): List<LocomotiveSeries>
    fun replaceAll(series: List<LocomotiveSeries>): Flow<ResultState<Unit>>
}
