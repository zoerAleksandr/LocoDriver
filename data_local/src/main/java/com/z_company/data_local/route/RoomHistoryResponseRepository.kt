package com.z_company.data_local.route

import com.z_company.core.ResultState
import com.z_company.core.ResultState.Companion.flowMap
import com.z_company.core.ResultState.Companion.flowRequest
import com.z_company.data_local.route.dao.ResponseDao
import com.z_company.data_local.route.entity_converters.SearchResponseConverter
import com.z_company.domain.entities.route.SearchResponse
import com.z_company.domain.repositories.HistoryResponseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RoomHistoryResponseRepository : HistoryResponseRepository, KoinComponent {
    private val dao: ResponseDao by inject()
    override fun getAllResponse(): Flow<ResultState<List<SearchResponse>>> {
        return flowMap {
            dao.getAllResponse().map { responses ->
                ResultState.Success(
                    responses.map { response ->
                        SearchResponseConverter.toData(response)
                    }
                )
            }
        }
    }

    override fun addResponse(response: SearchResponse): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.insert(SearchResponseConverter.fromData(response))
        }
    }

    override fun removeResponse(response: SearchResponse): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.remove(SearchResponseConverter.fromData(response))
        }
    }
}