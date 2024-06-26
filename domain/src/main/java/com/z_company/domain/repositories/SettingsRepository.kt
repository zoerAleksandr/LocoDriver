package com.z_company.domain.repositories

import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.NightTime
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.entities.route.LocoType
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getDieselCoefficient(): Flow<Double>
    fun setDieselCoefficient(value: Double?): Flow<ResultState<Unit>>
    fun getMinTimeRest(): Flow<Long?>
    fun setMinTimeRest(value: Long?): Flow<ResultState<Unit>>
    fun getStandardDurationOfWork(): Flow<Long>
    fun setStandardDurationOfWork(value: Long): Flow<ResultState<Unit>>
    fun getTypeLoco(): Flow<LocoType>
    fun setTypeLoco(type: LocoType): Flow<ResultState<Unit>>
    fun getNightTime(): Flow<ResultState<Int>>
    fun updateNightTime(nightTime: NightTime): Flow<ResultState<Unit>>
    fun setSettings(userSettings: UserSettings): Flow<ResultState<Unit>>
    fun getSettings(): Flow<ResultState<UserSettings?>>
    fun setUpdateAt(timestamp: Long): Flow<ResultState<Unit>>
    fun setWorkTimeDefault(timeInMillis: Long): Flow<ResultState<Unit>>
    fun setCurrentMonthOfYear(monthOfYear: MonthOfYear): Flow<ResultState<Unit>>
}