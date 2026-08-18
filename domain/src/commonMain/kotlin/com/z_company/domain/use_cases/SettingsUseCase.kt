package com.z_company.domain.use_cases

import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.setting.NightTime
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.repositories.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
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
            withContext(Dispatchers.Default) {
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

    fun updateSubscriptionPeriod(time: Long): Flow<ResultState<Unit>> {
        return settingsRepository.updateSubscriptionPeriod(time)
    }

    fun setDefaultSettings(currentMonthOfYear: MonthOfYear): Flow<ResultState<Unit>> {
        val setting = UserSettings(
            selectMonthOfYear = currentMonthOfYear
        )
        return settingsRepository.setSettings(setting)
    }

    fun getFlowCurrentSettingsState(): Flow<ResultState<UserSettings>> {
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

    fun setShowBreak(value: Boolean): Flow<ResultState<Unit>> {
        return settingsRepository.setShowBreak(value)
    }

    suspend fun removeStation(value: String) {
        coroutineScope {
            withContext(Dispatchers.Default) {
                settingsRepository.getFlowSettingsState().collect { result ->
                    if (result is ResultState.Success) {
                        result.data?.let { settings ->
                            val oldStations = settings.stationList
                            val newList = mutableListOf<String>()
                            newList.addAll(oldStations)
                            // trim-сравнение: в списке имя могло сохраниться с
                            // пробелами, а вызывающий передаёт уже обрезанное.
                            newList.removeAll { it.trim() == value.trim() }

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
            withContext(Dispatchers.Default) {
                settingsRepository.getFlowSettingsState().collect { result ->
                    if (result is ResultState.Success) {
                        result.data?.let { settings ->
                            val oldSeries = settings.locomotiveSeriesList
                            val newList = mutableListOf<String>()
                            newList.addAll(oldSeries)
                            newList.removeAll { it.trim() == value.trim() }
                            settingsRepository.setLocomotiveSeriesList(newList).collect()
                        }
                        this.cancel()
                    }
                }
            }
        }
    }

    suspend fun setLocomotiveSeries(series: String) {
        withContext(Dispatchers.Default) {
            var result = settingsRepository.getFlowSettingsState().first { it is ResultState.Success || it is ResultState.Error }
            var settings = if (result is ResultState.Success) {
                result.data
            } else {
                // Create default settings if not exist
                val defaultSettings = UserSettings() // Adjust with appropriate defaults
                settingsRepository.setSettings(defaultSettings).first { it is ResultState.Success || it is ResultState.Error }
                defaultSettings
            }

            val oldSeries = settings?.locomotiveSeriesList ?: emptyList()
            val newList = mutableListOf<String>()
            newList.add(series)
            newList.addAll(oldSeries)
            val uniqueSeriesName: MutableList<String> =
                newList.filter { it.isNotBlank() }.distinct().toMutableList()
            settingsRepository.setLocomotiveSeriesList(uniqueSeriesName).first { it is ResultState.Success || it is ResultState.Error }
        }
    }

    /** Добавляет пользовательский тип «прочей работы» в список настроек (без дублей). */
    suspend fun setOtherWorkType(type: String) {
        if (type.isBlank()) return
        withContext(Dispatchers.Default) {
            val result = settingsRepository.getFlowSettingsState()
                .first { it is ResultState.Success || it is ResultState.Error }
            val settings = if (result is ResultState.Success) result.data else null
            settings?.let { s ->
                val newList = (listOf(type) + s.otherWorkTypeList)
                    .filter { it.isNotBlank() }
                    .distinct()
                settingsRepository.setSettings(s.copy(otherWorkTypeList = newList))
                    .first { it is ResultState.Success || it is ResultState.Error }
            }
        }
    }

    /** Удаляет пользовательский тип «прочей работы» из списка настроек. */
    suspend fun removeOtherWorkType(type: String) {
        withContext(Dispatchers.Default) {
            val result = settingsRepository.getFlowSettingsState()
                .first { it is ResultState.Success || it is ResultState.Error }
            val settings = if (result is ResultState.Success) result.data else null
            settings?.let { s ->
                val newList = s.otherWorkTypeList.filterNot { it == type }
                settingsRepository.setSettings(s.copy(otherWorkTypeList = newList))
                    .first { it is ResultState.Success || it is ResultState.Error }
            }
        }
    }

    suspend fun setLocomotiveSeriesList(series: List<String>) {
        withContext(Dispatchers.Default) {
            var result = settingsRepository.getFlowSettingsState().first { it is ResultState.Success || it is ResultState.Error }
            var settings = if (result is ResultState.Success) {
                result.data
            } else {
                // Create default settings if not exist
                val defaultSettings = UserSettings() // Adjust with appropriate defaults
                settingsRepository.setSettings(defaultSettings).first { it is ResultState.Success || it is ResultState.Error }
                defaultSettings
            }

            val oldSeries = settings?.locomotiveSeriesList ?: emptyList()
            val newList = mutableListOf<String>()
            newList.addAll(series)
            newList.addAll(oldSeries)
            val uniqueSeriesName: MutableList<String> =
                newList.filter { it.isNotBlank() }.distinct().toMutableList()
            settingsRepository.setLocomotiveSeriesList(uniqueSeriesName).first { it is ResultState.Success || it is ResultState.Error }
        }
    }
}