package com.z_company.domain.use_cases

import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.NightTime
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.repositories.SettingsRepository
import kotlinx.coroutines.flow.Flow

class SettingsUseCase(private val settingsRepository: SettingsRepository) {
    fun saveNightTime(nightTime: NightTime): Flow<ResultState<Unit>> {
        return settingsRepository.updateNightTime(nightTime)
    }

    fun setStations(stations: List<String>): Flow<ResultState<Unit>> {
        val uniqueStationsName = stations
            .distinct()
        return settingsRepository.setStations(uniqueStationsName)
    }

    fun setDieselCoefficient(coefficient: Double): Flow<ResultState<Unit>> {
        return settingsRepository.setDieselCoefficient(coefficient)
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

    fun setWorkTimeDefault(timeInMillis: Long):  Flow<ResultState<Unit>> {
        return settingsRepository.setWorkTimeDefault(timeInMillis)
    }

    fun saveSetting(settings: UserSettings): Flow<ResultState<Unit>> {
        return settingsRepository.setSettings(settings)
    }

    fun setCurrentMonthOfYear(monthOfYear: MonthOfYear): Flow<ResultState<Unit>> {
        return settingsRepository.setCurrentMonthOfYear(monthOfYear)
    }

    fun clearLocalUserSettingRepository(): Flow<ResultState<Unit>> {
        return settingsRepository.clearRepository()
    }
}