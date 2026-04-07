package com.z_company.domain.repositories

import com.z_company.core.ResultState
import com.z_company.domain.entities.ReleaseDay
import kotlinx.coroutines.flow.Flow

interface ReleaseDayRepository {
    /** Поток всех дней отвлечений */
    fun getAllFlow(): Flow<List<ReleaseDay>>

    /** Дни отвлечений за конкретный месяц */
    fun getByYearMonthFlow(year: Int, month: Int): Flow<List<ReleaseDay>>

    /** Все дни отвлечений (разовый запрос, для синхронизации) */
    fun getAll(): List<ReleaseDay>

    /** Сохранить/обновить список дней отвлечений */
    fun saveAll(days: List<ReleaseDay>): Flow<ResultState<Unit>>

    /** Удалить конкретный день */
    fun delete(id: String): Flow<ResultState<Unit>>

    /** Удалить все дни за месяц (синхронно, для использования внутри flowRequest) */
    fun deleteByYearMonthSync(year: Int, month: Int)

    /** Удалить все дни за месяц */
    fun deleteByYearMonth(year: Int, month: Int): Flow<ResultState<Unit>>

    /** Полностью заменить все дни (для загрузки с сервера) */
    fun replaceAll(days: List<ReleaseDay>): Flow<ResultState<Unit>>
}
