@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.data_local.norma_time

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.z_company.core.ResultState
import com.z_company.core.ResultState.Companion.flowRequest
import com.z_company.data_local.setting.db.SettingsDatabase
import com.z_company.domain.brake.LocoSeriesDefaults
import com.z_company.domain.entities.norma_time.LocomotiveSeries
import com.z_company.domain.entities.norma_time.LocomotiveSeriesTombstone
import com.z_company.domain.entities.norma_time.SectionNumberingType
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.repositories.LocomotiveSeriesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
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

    override fun getTombstones(): List<LocomotiveSeriesTombstone> =
        db.locomotiveSeriesQueries.getTombstones().executeAsList().map {
            LocomotiveSeriesTombstone(
                seriesId = it.seriesId,
                name = it.name,
                type = LocoType.entries.find { t -> t.name == it.type } ?: LocoType.ELECTRIC,
                deletedAt = it.deletedAt,
            )
        }

    override fun replaceAll(series: List<LocomotiveSeries>): Flow<ResultState<Unit>> = flowRequest {
        val now = Clock.System.now().toEpochMilliseconds()
        db.locomotiveSeriesQueries.transaction {
            val kept = series.map { it.seriesId }.toSet()
            // Пропавшие из списка серии — удалены пользователем: оставляем надгробие.
            db.locomotiveSeriesQueries.getAll().executeAsList()
                .filter { it.seriesId !in kept }
                .forEach { gone ->
                    db.locomotiveSeriesQueries.insertTombstone(gone.seriesId, gone.name, gone.type, now)
                }
            kept.forEach { db.locomotiveSeriesQueries.deleteTombstone(it) }
            writeSeries(series)
        }
    }

    override fun applySync(
        series: List<LocomotiveSeries>,
        tombstones: List<LocomotiveSeriesTombstone>,
    ): Flow<ResultState<Unit>> = flowRequest {
        db.locomotiveSeriesQueries.transaction {
            writeSeries(series)
            db.locomotiveSeriesQueries.deleteAllTombstones()
            tombstones.forEach {
                db.locomotiveSeriesQueries.insertTombstone(it.seriesId, it.name, it.type.name, it.deletedAt)
            }
        }
    }

    override fun prefillBrakeDefaults(): Boolean {
        val now = Clock.System.now().toEpochMilliseconds()
        var changed = false
        db.locomotiveSeriesQueries.transaction {
            val all = getAll()
            if (all.none { LocoSeriesDefaults.needsDefaults(it) }) return@transaction
            val filled = all.map { s ->
                if (!LocoSeriesDefaults.needsDefaults(s)) s
                else with(LocoSeriesDefaults) { s.withDefaults() }.copy(updatedAt = now)
            }
            // Только запись, без надгробий: набор серий не меняется. updatedAt вырос - значения уйдут на сервер.
            writeSeries(filled)
            changed = true
        }
        return changed
    }

    private fun writeSeries(series: List<LocomotiveSeries>) {
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
                updatedAt = s.updatedAt,
                brakeVolumeL = s.brakeVolumeL?.toLong(),
                axleCount = s.axleCount?.toLong(),
                axleLoadTf = s.axleLoadTf,
                weightT = s.weightT,
                sectionCount = s.sectionCount?.toLong(),
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
            brakeVolumeL = brakeVolumeL?.toInt(),
            axleCount = axleCount?.toInt(),
            axleLoadTf = axleLoadTf,
            weightT = weightT,
            sectionCount = sectionCount?.toInt(),
            updatedAt = updatedAt
        )
}
