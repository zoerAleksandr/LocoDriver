package com.z_company.domain.repositories

import com.z_company.core.ResultState
import com.z_company.domain.entities.route.LocoType
import kotlinx.coroutines.flow.Flow

interface UserSettingsRepository {
    fun getDieselCoefficient(): Flow<Double>
    fun setDieselCoefficient(value: Double?): Flow<ResultState<Unit>>
    fun getMinTimeRest(): Flow<Long?>
    fun setMinTimeRest(value: Long?): Flow<ResultState<Unit>>
    fun getStandardDurationOfWork(): Flow<Long>
    fun setStandardDurationOfWork(value: Long): Flow<ResultState<Unit>>
    fun getTypeLoco(): Flow<LocoType>
    fun setTypeLoco(type: LocoType): Flow<ResultState<Unit>>
    fun getStartNightHour(): Flow<Int>
    fun getStartNightMinute(): Flow<Int>
    fun getEndNightHour(): Flow<Int>
    fun getEndNightMinute(): Flow<Int>

}