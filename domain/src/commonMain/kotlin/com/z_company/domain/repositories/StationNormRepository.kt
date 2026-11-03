package com.z_company.domain.repositories

import com.z_company.core.ResultState
import com.z_company.domain.entities.norma_time.StationNorm
import com.z_company.domain.entities.norma_time.StationTombstone
import kotlinx.coroutines.flow.Flow

interface StationNormRepository {
    fun getAllFlow(): Flow<List<StationNorm>>
    fun getAll(): List<StationNorm>

    /**
     * Правка справочника пользователем. Станции, которые были и пропали из списка, считаются
     * удалёнными — для них записываются надгробия; у присланных станций надгробие снимается.
     */
    fun replaceAll(stations: List<StationNorm>): Flow<ResultState<Unit>>

    fun getTombstones(): List<StationTombstone>

    /** Записать результат слияния с сервером как есть — без вывода удалений из разницы. */
    fun applySync(stations: List<StationNorm>, tombstones: List<StationTombstone>): Flow<ResultState<Unit>>
}
