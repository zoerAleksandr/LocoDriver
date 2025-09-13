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
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

class SettingsUseCase(private val settingsRepository: SettingsRepository) {

    fun getTimeZone(timeZoneInMillis: Long = 0L): String {
        val offsetInMillis = timeZoneInMillis + 10_800_000
        val offset = offsetInMillis.div(3_600_000L)
        return "GMT+$offset"
    }

    fun getOffsetBetweenCurrentTimeZoneAndUsageTimeZone(): Flow<Long> {
        return channelFlow {
            getUserSettingFlow().collect { setting ->
                if (setting.timeZone != 0L) {
                    trySend(setting.timeZone)
                }
                else {
                    trySend(0L)
                }
            }
        }
    }

    fun updateMonthOfYearInUserSetting(monthOfYear: MonthOfYear): Flow<ResultState<Unit>> {
        return settingsRepository.updateMonthOfYearInUserSetting(monthOfYear)
    }

    fun saveNightTime(nightTime: NightTime): Flow<ResultState<Unit>> {
        return settingsRepository.updateNightTime(nightTime)
    }

    suspend fun setStations(stations: List<String>) {
        coroutineScope {
            withContext(Dispatchers.IO) {
                settingsRepository.getFlowSettingsState().collect { result ->
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

    fun setDefaultSettings(currentMonthOfYear: MonthOfYear): Flow<ResultState<Unit>> {
        val setting = UserSettings(
            selectMonthOfYear = currentMonthOfYear
        )
        return settingsRepository.setSettings(setting)
    }

    fun getFlowCurrentSettingsState(): Flow<ResultState<UserSettings?>> {
        return settingsRepository.getFlowSettingsState()
    }

    fun getUserSettingFlow(): Flow<UserSettings> {
           return settingsRepository.getUserSettingFlow()
    }

    fun getUserSetting(): UserSettings {
        return settingsRepository.getUserSettings()
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
                settingsRepository.getFlowSettingsState().collect { result ->
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

    suspend fun removeLocomotiveSeries(value: String) {
        coroutineScope {
            withContext(Dispatchers.IO) {
                settingsRepository.getFlowSettingsState().collect { result ->
                    if (result is ResultState.Success) {
                        result.data?.let { settings ->
                            val oldSeries = settings.locomotiveSeriesList
                            val newList = mutableListOf<String>()
                            newList.addAll(oldSeries)
                            newList.remove(value)
                            settingsRepository.setLocomotiveSeriesList(newList).collect()
                        }
                        this.cancel()
                    }
                }
            }
        }
    }

    suspend fun setLocomotiveSeries(series: String) {
        coroutineScope {
            withContext(Dispatchers.IO) {
                settingsRepository.getFlowSettingsState().collect { result ->
                    if (result is ResultState.Success) {
                        result.data?.let { settings ->
                            val oldSeries = settings.locomotiveSeriesList
                            val newList = mutableListOf<String>()
                            newList.add(series)
                            newList.addAll(oldSeries)
                            val uniqueSeriesName: MutableList<String> =
                                newList.filter { it.isNotBlank() }.distinct().toMutableList()
                            settingsRepository.setLocomotiveSeriesList(uniqueSeriesName).collect()
                        }
                        this.cancel()
                    }
                }
            }
        }
    }

    suspend fun setLocomotiveSeriesList(series: List<String>) {
        coroutineScope {
            withContext(Dispatchers.IO) {
                settingsRepository.getFlowSettingsState().collect { result ->
                    if (result is ResultState.Success) {
                        result.data?.let { settings ->
                            val oldSeries = settings.locomotiveSeriesList
                            val newList = mutableListOf<String>()
                            newList.addAll(series)
                            newList.addAll(oldSeries)
                            val uniqueSeriesName: MutableList<String> =
                                newList.filter { it.isNotBlank() }.distinct().toMutableList()
                            settingsRepository.setLocomotiveSeriesList(uniqueSeriesName).collect()
                        }
                        this.cancel()
                    }
                }
            }
        }
    }
}