package com.z_company.domain.repositories

import com.z_company.core.ResultState
import com.z_company.domain.entities.SalarySetting
import kotlinx.coroutines.flow.Flow

interface SalarySettingRepository {
    fun getSalarySetting(): Flow<ResultState<SalarySetting?>>
    fun saveSalarySetting(setting: SalarySetting): Flow<ResultState<Unit>>
}