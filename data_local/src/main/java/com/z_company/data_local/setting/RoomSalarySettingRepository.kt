package com.z_company.data_local.setting

import com.z_company.core.ResultState
import com.z_company.core.ResultState.Companion.flowRequest
import com.z_company.data_local.setting.dao.SalarySettingDao
import com.z_company.data_local.setting.entity_converter.SalarySettingConverter
import com.z_company.domain.entities.SalarySetting
import com.z_company.domain.repositories.SalarySettingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RoomSalarySettingRepository : SalarySettingRepository, KoinComponent {
    private val salarySettingDao: SalarySettingDao by inject()
    override fun getSalarySetting(): SalarySetting {
        val setting = salarySettingDao.getSalarySetting()
        return if (setting == null) {
            SalarySetting()
        } else {
            SalarySettingConverter.toData(setting)
        }
    }

    override fun getSalarySettingState(): Flow<ResultState<SalarySetting?>> {
        return ResultState.flowMap {
            salarySettingDao.getFlowSalarySetting().map { setting ->
                ResultState.Success(
                    setting?.let {
                        SalarySettingConverter.toData(setting)
                    }
                )
            }
        }
    }

    override fun saveSalarySetting(setting: SalarySetting): Flow<ResultState<Unit>> {
        return flowRequest {
            salarySettingDao.saveSalarySetting(SalarySettingConverter.fromData(setting))
        }
    }

    override fun getSalarySettingFlow(): Flow<SalarySetting> {
        return salarySettingDao.getFlowSalarySetting().map { setting ->
            setting?.let {
                SalarySettingConverter.toData(setting)
            } ?: SalarySetting()
        }
    }
}