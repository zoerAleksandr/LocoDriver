@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.data_local.norma_time

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.z_company.core.ResultState
import com.z_company.core.ResultState.Companion.flowRequest
import com.z_company.data_local.setting.db.SettingsDatabase
import com.z_company.domain.entities.norma_time.StationNorm
import com.z_company.domain.entities.norma_time.StationTombstone
import com.z_company.domain.entities.setting.ServicePhaseStationKind
import com.z_company.domain.repositories.StationNormRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SqlDelightStationNormRepository : StationNormRepository, KoinComponent {
    private val db: SettingsDatabase by inject()

    override fun getAllFlow(): Flow<List<StationNorm>> =
        db.stationNormQueries.getAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }
            // Повреждённая локальная схема не должна ронять подписчика: отдаём пустой список.
            .catch { emit(emptyList()) }

    override fun getAll(): List<StationNorm> =
        db.stationNormQueries.getAll().executeAsList().map { it.toDomain() }

    override fun getTombstones(): List<StationTombstone> =
        db.stationNormQueries.getTombstones().executeAsList().map {
            StationTombstone(stationId = it.stationId, name = it.name, deletedAt = it.deletedAt)
        }

    override fun replaceAll(stations: List<StationNorm>): Flow<ResultState<Unit>> = flowRequest {
        val now = Clock.System.now().toEpochMilliseconds()
        db.stationNormQueries.transaction {
            val kept = stations.map { it.stationId }.toSet()
            // Пропавшие из списка станции — удалены пользователем: оставляем надгробие.
            db.stationNormQueries.getAll().executeAsList()
                .filter { it.stationId !in kept }
                .forEach { gone ->
                    db.stationNormQueries.insertTombstone(gone.stationId, gone.name, now)
                }
            kept.forEach { db.stationNormQueries.deleteTombstone(it) }
            writeStations(stations)
        }
    }

    override fun applySync(
        stations: List<StationNorm>,
        tombstones: List<StationTombstone>,
    ): Flow<ResultState<Unit>> = flowRequest {
        db.stationNormQueries.transaction {
            writeStations(stations)
            db.stationNormQueries.deleteAllTombstones()
            tombstones.forEach {
                db.stationNormQueries.insertTombstone(it.stationId, it.name, it.deletedAt)
            }
        }
    }

    private fun writeStations(stations: List<StationNorm>) {
        db.stationNormQueries.deleteAll()
        stations.forEach { s ->
            db.stationNormQueries.insertOrReplace(
                stationId = s.stationId,
                name = s.name,
                appearanceToStartMin = s.appearanceToStartMin?.toLong(),
                endToBarrierMin = s.endToBarrierMin?.toLong(),
                barrierToStartMin = s.barrierToStartMin?.toLong(),
                endToWorkEndMin = s.endToWorkEndMin?.toLong(),
                updatedAt = s.updatedAt,
                latitude = s.latitude,
                longitude = s.longitude,
                esr = s.esr,
                osmId = s.osmId,
                kind = s.kind?.name,
                axisKm = s.axisKm?.toLong(),
                axisPicket = s.axisPicket?.toLong(),
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
            latitude = latitude,
            longitude = longitude,
            esr = esr,
            osmId = osmId,
            kind = kind?.let { k -> ServicePhaseStationKind.entries.firstOrNull { it.name == k } },
            axisKm = axisKm?.toInt(),
            axisPicket = axisPicket?.toInt(),
            updatedAt = updatedAt
        )
}
