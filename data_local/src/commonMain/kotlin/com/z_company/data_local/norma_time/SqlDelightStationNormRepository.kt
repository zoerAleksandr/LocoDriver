package com.z_company.data_local.norma_time

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.z_company.core.ResultState
import com.z_company.core.ResultState.Companion.flowRequest
import com.z_company.data_local.setting.db.SettingsDatabase
import com.z_company.domain.entities.norma_time.StationNorm
import com.z_company.domain.repositories.StationNormRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SqlDelightStationNormRepository : StationNormRepository, KoinComponent {
    private val db: SettingsDatabase by inject()

    override fun getAllFlow(): Flow<List<StationNorm>> =
        db.stationNormQueries.getAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    override fun getAll(): List<StationNorm> =
        db.stationNormQueries.getAll().executeAsList().map { it.toDomain() }

    override fun replaceAll(stations: List<StationNorm>): Flow<ResultState<Unit>> = flowRequest {
        db.stationNormQueries.deleteAll()
        stations.forEach { s ->
            db.stationNormQueries.insertOrReplace(
                stationId = s.stationId,
                name = s.name,
                appearanceToStartMin = s.appearanceToStartMin?.toLong(),
                endToBarrierMin = s.endToBarrierMin?.toLong(),
                barrierToStartMin = s.barrierToStartMin?.toLong(),
                endToWorkEndMin = s.endToWorkEndMin?.toLong(),
                updatedAt = s.updatedAt
            )
        }
    }

    private fun com.zcompany.datalocal.setting.db.StationNorm.toDomain(): StationNorm =
        StationNorm(
            stationId = stationId,
            name = name,
            appearanceToStartMin = appearanceToStartMin?.toInt(),
            endToBarrierMin = endToBarrierMin?.toInt(),
            barrierToStartMin = barrierToStartMin?.toInt(),
            endToWorkEndMin = endToWorkEndMin?.toInt(),
            updatedAt = updatedAt
        )
}
