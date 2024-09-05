package com.z_company.data_local.setting

import com.z_company.core.ResultState
import com.z_company.core.ResultState.Companion.flowMap
import com.z_company.core.ResultState.Companion.flowRequest
import com.z_company.data_local.setting.dao.SettingsDao
import com.z_company.data_local.setting.entity_converter.MonthOfYearConverter
import com.z_company.data_local.setting.entity_converter.NightTimeConverter
import com.z_company.data_local.setting.entity_converter.UserSettingsConverter
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.NightTime
import com.z_company.domain.entities.SETTINGS_KEY
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.repositories.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RoomSettingRepository : SettingsRepository, KoinComponent {
    private val dao: SettingsDao by inject()

    override fun setDieselCoefficient(value: Double): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.setDieselCoefficient(coefficient = value, key = SETTINGS_KEY)
        }
    }

    override fun updateNightTime(nightTime: NightTime): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.updateNightTime(
                nightTime = NightTimeConverter.fromData(nightTime),
                key = SETTINGS_KEY
            )
        }
    }

    override fun setSettings(userSettings: UserSettings): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.saveSettings(UserSettingsConverter.fromData(userSettings))
        }
    }

    override fun getSettings(): Flow<ResultState<UserSettings?>> {
        return flowMap {
            dao.getSettings().map { settings ->
                ResultState.Success(
                    settings?.let {
                        UserSettingsConverter.toData(settings)
                    }
                )
            }
        }
    }

    override fun setUpdateAt(timestamp: Long): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.setUpdateAt(timestamp = timestamp, key = SETTINGS_KEY)
        }
    }

    override fun setWorkTimeDefault(timeInMillis: Long): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.setWorkTimeDefault(timeInMillis = timeInMillis, key = SETTINGS_KEY)
        }
    }

    override fun setCurrentMonthOfYear(monthOfYear: MonthOfYear): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.setCurrentMonthOfYear(
                monthOfYear = MonthOfYearConverter.fromData(monthOfYear),
                key = SETTINGS_KEY
            )
        }
    }

    override fun clearRepository(): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.clearCalendar()
        }
    }
}