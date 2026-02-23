package com.z_company.domain.repositories

import com.z_company.core.ResultState
import com.z_company.domain.entities.route.SearchResponse
import kotlinx.coroutines.flow.Flow

interface HistoryResponseRepository {
    fun getAllResponse(): Flow<ResultState<List<SearchResponse>>>
    fun addResponse(response: SearchResponse): Flow<ResultState<Unit>>
    fun removeResponse(response: SearchResponse): Flow<ResultState<Unit>>
}