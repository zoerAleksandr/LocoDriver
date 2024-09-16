package com.z_company.domain.use_cases

import com.z_company.core.ResultState
import com.z_company.domain.entities.SalarySetting
import com.z_company.domain.repositories.SalarySettingRepository
import kotlinx.coroutines.flow.Flow

class SalarySettingUseCase(val repository: SalarySettingRepository) {
    fun getSalarySetting(): Flow<ResultState<SalarySetting?>> =
        repository.getSalarySetting()

    fun saveSalarySetting(setting: SalarySetting): Flow<ResultState<Unit>> =
        repository.saveSalarySetting(setting)
}