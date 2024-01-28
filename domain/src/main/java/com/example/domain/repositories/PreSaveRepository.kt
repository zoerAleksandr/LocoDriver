package com.example.domain.repositories

import com.example.core.ResultState
import com.example.domain.entities.route.Locomotive
import com.example.domain.entities.route.pre_save.PreLocomotive
import kotlinx.coroutines.flow.Flow

/** Этот репозиторий предназначен для временного хранения
 *  Child Entity до сохранения его в Local Repository.
 *  метод loadPreLoco используется перед сохранением Route Entity */
interface PreSaveRepository {
    fun loadPreLoco(locoId: String): Flow<ResultState<PreLocomotive?>>
    fun loadAllLoco(basicId: String): Flow<ResultState<List<Locomotive>>>
    fun removeLoco(preLocomotive: PreLocomotive): Flow<ResultState<Unit>>
    fun clearRepository(): Flow<ResultState<Unit>>
    fun saveLocomotive(preLocomotive: PreLocomotive): Flow<ResultState<Unit>>
}