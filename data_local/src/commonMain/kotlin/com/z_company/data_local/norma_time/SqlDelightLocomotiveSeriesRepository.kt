package com.z_company.data_local.norma_time

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.z_company.core.ResultState
import com.z_company.core.ResultState.Companion.flowRequest
import com.z_company.data_local.setting.db.SettingsDatabase
import com.z_company.domain.entities.norma_time.LocomotiveSeries
import com.z_company.domain.entities.norma_time.SectionNumberingType
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.repositories.LocomotiveSeriesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SqlDelightLocomotiveSeriesRepository : LocomotiveSeriesRepository, KoinComponent {
    private val db: SettingsDatabase by inject()

    override fun getAllFlow(): Flow<List<LocomotiveSeries>> =
        db.locomotiveSeriesQueries.getAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    override fun getAll(): List<LocomotiveSeries> =
        db.locomotiveSeriesQueries.getAll().executeAsList().map { it.toDomain() }

    override fun replaceAll(series: List<LocomotiveSeries>): Flow<ResultState<Unit>> = flowRequest {
        db.locomotiveSeriesQueries.deleteAll()
        series.forEach { s ->
            db.locomotiveSeriesQueries.insertOrReplace(
                seriesId = s.seriesId,
                name = s.name,
                type = s.type.name,
                acceptanceDurationMin = s.acceptanceDurationMin?.toLong(),
                deliveryDurationMin = s.deliveryDurationMin?.toLong(),
                acceptanceHandToHandMin = s.acceptanceHandToHandMin?.toLong(),
                deliveryHandToHandMin = s.deliveryHandToHandMin?.toLong(),
                sectionNumberingType = s.sectionNumberingType.name,
                updatedAt = s.updatedAt
            )
        }
    }

    private fun com.zcompany.datalocal.setting.db.LocomotiveSeries.toDomain(): LocomotiveSeries =
        LocomotiveSeries(
            seriesId = seriesId,
            name = name,
            type = LocoType.entries.find { it.name == type } ?: LocoType.ELECTRIC,
            acceptanceDurationMin = acceptanceDurationMin?.toInt(),
            deliveryDurationMin = deliveryDurationMin?.toInt(),
            acceptanceHandToHandMin = acceptanceHandToHandMin?.toInt(),
            deliveryHandToHandMin = deliveryHandToHandMin?.toInt(),
            sectionNumberingType = SectionNumberingType.entries.find {
                it.name == sectionNumberingType
            } ?: SectionNumberingType.NUMERIC,
            updatedAt = updatedAt
        )
}
