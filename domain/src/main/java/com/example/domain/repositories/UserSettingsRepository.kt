package com.example.domain.repositories

import com.example.core.ResultState
import com.example.domain.entities.route.LocoType
import kotlinx.coroutines.flow.Flow

interface UserSettingsRepository {
    fun getDieselCoefficient(): Flow<Double>
    fun setDieselCoefficient(value: Double?): Flow<ResultState<Unit>>
    fun getMinTimeRest(): Flow<Long?>
    fun setMinTimeRest(value: Long): Flow<ResultState<Unit>>
    fun getStandardDurationOfWork(): Flow<Long>
    fun setStandardDurationOfWork(value: Long): Flow<ResultState<Unit>>
    suspend fun getTypeLoco(): Flow<LocoType>
    fun setTypeLoco(type: LocoType): Flow<ResultState<Unit>>
}