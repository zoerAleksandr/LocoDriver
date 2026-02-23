package com.z_company.data_local.route

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.z_company.core.ResultState
import com.z_company.core.ResultState.Companion.flowMap
import com.z_company.core.ResultState.Companion.flowRequest
import com.z_company.data_local.route.searchdb.SearchResponseDatabase
import com.z_company.domain.entities.route.SearchResponse
import com.z_company.domain.repositories.HistoryResponseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SqlDelightHistoryResponseRepository : HistoryResponseRepository, KoinComponent {
    private val db: SearchResponseDatabase by inject()

    override fun getAllResponse(): Flow<ResultState<List<SearchResponse>>> {
        return flowMap {
            db.searchResponseQueries.getAll()
                .asFlow()
                .mapToList(Dispatchers.Default)
                .map { rows ->
                    ResultState.Success(rows.map { SearchResponse(it) })
                }
        }
    }

    override fun addResponse(response: SearchResponse): Flow<ResultState<Unit>> {
        return flowRequest { db.searchResponseQueries.insert(response.responseText) }
    }

    override fun removeResponse(response: SearchResponse): Flow<ResultState<Unit>> {
        return flowRequest { db.searchResponseQueries.delete(response.responseText) }
    }
}
