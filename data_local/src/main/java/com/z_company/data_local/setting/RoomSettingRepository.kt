package com.z_company.data_local.setting

import com.z_company.core.ResultState
import com.z_company.core.ResultState.Companion.flowMap
import com.z_company.core.ResultState.Companion.flowRequest
import com.z_company.data_local.setting.dao.SettingsDao
import com.z_company.data_local.setting.entity_converter.NightTimeConverter
import com.z_company.data_local.setting.entity_converter.UserSettingsConverter
import com.z_company.domain.entities.NightTime
import com.z_company.domain.entities.SETTINGS_KEY
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.repositories.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RoomSettingRepository : SettingsRepository, KoinComponent {
    private val dao: SettingsDao by inject()
    override fun getDieselCoefficient(): Flow<Double> {
        TODO("Not yet implemented")
    }

    override fun setDieselCoefficient(value: Double?): Flow<ResultState<Unit>> {
        TODO("Not yet implemented")
    }

    override fun getMinTimeRest(): Flow<Long?> {
        TODO("Not yet implemented")
    }

    override fun setMinTimeRest(value: Long?): Flow<ResultState<Unit>> {
        TODO("Not yet implemented")
    }

    override fun getStandardDurationOfWork(): Flow<Long> {
        TODO("Not yet implemented")
    }

    override fun setStandardDurationOfWork(value: Long): Flow<ResultState<Unit>> {
        TODO("Not yet implemented")
    }

    override fun getTypeLoco(): Flow<LocoType> {
        TODO("Not yet implemented")
    }

    override fun setTypeLoco(type: LocoType): Flow<ResultState<Unit>> {
        TODO("Not yet implemented")
    }

    override fun getNightTime(): Flow<ResultState<Int>> {
        TODO("Not yet implemented")
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
}