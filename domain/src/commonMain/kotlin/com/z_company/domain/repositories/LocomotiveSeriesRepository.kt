package com.z_company.domain.repositories

import com.z_company.core.ResultState
import com.z_company.domain.entities.norma_time.LocomotiveSeries
import com.z_company.domain.entities.norma_time.LocomotiveSeriesTombstone
import kotlinx.coroutines.flow.Flow

interface LocomotiveSeriesRepository {
    fun getAllFlow(): Flow<List<LocomotiveSeries>>
    fun getAll(): List<LocomotiveSeries>

    /**
     * Правка справочника пользователем. Пропавшие из списка серии считаются удалёнными —
     * для них записываются надгробия; у присланных серий надгробие снимается.
     */
    fun replaceAll(series: List<LocomotiveSeries>): Flow<ResultState<Unit>>

    fun getTombstones(): List<LocomotiveSeriesTombstone>

    /** Записать результат слияния с сервером как есть — без вывода удалений из разницы. */
    fun applySync(
        series: List<LocomotiveSeries>,
        tombstones: List<LocomotiveSeriesTombstone>,
    ): Flow<ResultState<Unit>>
}
