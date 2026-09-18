package com.z_company.domain.repositories

import com.z_company.core.ResultState
import com.z_company.domain.entities.setting.SalarySetting
import kotlinx.coroutines.flow.Flow

interface SalarySettingRepository {
    fun getSalarySetting(): SalarySetting
    fun getSalarySettingState(): Flow<ResultState<SalarySetting>>
    fun saveSalarySetting(setting: SalarySetting): Flow<ResultState<Unit>>
    /** Меняет только метку [SalarySetting.updatedAt] (после push — на серверную). */
    fun setUpdatedAt(timestamp: Long): Flow<ResultState<Unit>>

    fun getSalarySettingFlow(): Flow<SalarySetting>
}