package com.z_company.domain.repositories

import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.NightTime
import com.z_company.domain.entities.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun setDieselCoefficient(value: Double): Flow<ResultState<Unit>>
    fun updateNightTime(nightTime: NightTime): Flow<ResultState<Unit>>
    fun setSettings(userSettings: UserSettings): Flow<ResultState<Unit>>
    fun getSettings(): Flow<ResultState<UserSettings?>>
    fun setUpdateAt(timestamp: Long): Flow<ResultState<Unit>>
    fun setWorkTimeDefault(timeInMillis: Long): Flow<ResultState<Unit>>
    fun setCurrentMonthOfYear(monthOfYear: MonthOfYear): Flow<ResultState<Unit>>
}