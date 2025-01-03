package com.z_company.domain.use_cases

import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.NightTime
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.repositories.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

class SettingsUseCase(private val settingsRepository: SettingsRepository) {
    fun saveNightTime(nightTime: NightTime): Flow<ResultState<Unit>> {
        return settingsRepository.updateNightTime(nightTime)
    }

    suspend fun setStations(stations: List<String>) {
        coroutineScope {
            withContext(Dispatchers.IO) {
                settingsRepository.getSettings().collect { result ->
                    if (result is ResultState.Success) {
                        result.data?.let { settings ->
                            val oldStations = settings.stationList
                            val newList = mutableListOf<String>()

                            newList.addAll(stations)
                            newList.addAll(oldStations)

                            val uniqueStationsName: MutableList<String> =
                                newList.filter { it.isNotBlank() }.distinct().toMutableList()

                            settingsRepository.setStations(uniqueStationsName).collect()
                        }
                        this.cancel()
                    }
                }
            }
        }
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

    fun setUpdateAt(timestamp: Long): Flow<ResultState<Unit>> {
        return settingsRepository.setUpdateAt(timestamp)
    }

    fun setWorkTimeDefault(timeInMillis: Long): Flow<ResultState<Unit>> {
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

    suspend fun removeStation(value: String) {
        coroutineScope {
            withContext(Dispatchers.IO) {
                settingsRepository.getSettings().collect { result ->
                    if (result is ResultState.Success) {
                        result.data?.let { settings ->
                            val oldStations = settings.stationList
                            val newList = mutableListOf<String>()
                            newList.addAll(oldStations)
                            newList.remove(value)

                            settingsRepository.setStations(newList).collect()
                        }
                        this.cancel()
                    }
                }
            }
        }
    }
}