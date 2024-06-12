package com.z_company.domain.use_cases

import com.z_company.core.ResultState
import com.z_company.domain.entities.NightTime
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.repositories.SettingsRepository
import kotlinx.coroutines.flow.Flow

class SettingsUseCase(private val settingsRepository: SettingsRepository) {
    fun saveNightTime(nightTime: NightTime): Flow<ResultState<Unit>> {
        return settingsRepository.updateNightTime(nightTime)
    }

    fun setDefaultSettings(): Flow<ResultState<Unit>> {
        return settingsRepository.setSettings(UserSettings())
    }

    fun getCurrentSettings(): Flow<ResultState<UserSettings?>> {
        return settingsRepository.getSettings()
    }

    fun setUpdateAt(timestamp: Long):  Flow<ResultState<Unit>> {
        return settingsRepository.setUpdateAt(timestamp)
    }
}