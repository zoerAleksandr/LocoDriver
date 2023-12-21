package com.example.data_local.setting

import com.example.core.ResultState
import com.example.core.ResultState.Companion.flowMap
import com.example.core.ResultState.Companion.flowRequest
import com.example.data_local.setting.dao.SettingsDao
import com.example.domain.entities.UserSettings
import com.example.domain.repositories.SettingsRepositories
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RoomSettingsRepository: SettingsRepositories, KoinComponent {
    private val dao: SettingsDao by inject()
    override fun getSettings(): Flow<ResultState<UserSettings>> {
        return flowMap {
            dao.getSettings().map { settings ->
                ResultState.Success(
                    settings?.let { UserSettingsConverter.toData(it) } ?: UserSettings()
                )
            }
        }
    }

    override fun saveSettings(userSettings: UserSettings): Flow<ResultState<Unit>> {
        return flowRequest{
            dao.saveSettings(UserSettingsConverter.fromData(userSettings))
        }
    }
}