package com.z_company.data_local.setting

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.core.ResultState.Companion.flowMap
import com.z_company.core.ResultState.Companion.flowRequest
import com.z_company.data_local.setting.db.SettingsDatabase
import com.z_company.data_local.setting.mapping.MonthOfYearMapper
import com.z_company.data_local.setting.mapping.SettingsMapper
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.setting.NightTime
import com.z_company.domain.entities.setting.SETTINGS_KEY
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.repositories.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SqlDelightSettingRepository : SettingsRepository, KoinComponent {
    private val db: SettingsDatabase by inject()

    private fun insertUserSettings(us: UserSettings) {
        db.userSettingsQueries.insertOrReplace(
            settingsKey = us.key,
            minTimeRest = us.minTimeRestPointOfTurnover,
            minTimeHomeRest = us.minTimeHomeRest,
            lastEnteredDieselCoefficient = us.lastEnteredDieselCoefficient,
            nightTime = SettingsMapper.encodeNightTime(us.nightTime),
            updateAt = us.updateAt,
            defaultLocoType = SettingsMapper.encodeLocoType(us.defaultLocoType),
            defaultWorkTime = us.defaultWorkTime,
            usingDefaultWorkTime = if (us.usingDefaultWorkTime) 1L else 0L,
            isConsiderFutureRoute = if (us.isConsiderFutureRoute) 1L else 0L,
            monthOfYear = SettingsMapper.encodeMonthOfYear(us.selectMonthOfYear),
            stationList = SettingsMapper.encodeStringList(us.stationList),
            timeZone = us.timeZone,
            locomotiveSeriesList = SettingsMapper.encodeStringList(us.locomotiveSeriesList),
            servicePhases = SettingsMapper.encodeServicePhaseList(us.servicePhases),
            standardTimesStartWork = SettingsMapper.encodeLongList(us.standardTimesStartWork),
            subscriptionPeriod = us.subscriptionPeriod,
            isDecimalTime = if (us.isDecimalTime) 1L else 0L,
            isShowBreak = if (us.isShowBreak) 1L else 0L,
            isShowOnePersonSwitch = if (us.isShowOnePersonSwitch) 1L else 0L,
            isShowLocoHeating = if (us.isShowLocoHeating) 1L else 0L,
            isShowLocoAuxiliary = if (us.isShowLocoAuxiliary) 1L else 0L,
            isShowLocoStatistics = if (us.isShowLocoStatistics) 1L else 0L,
            isShowLocoNorma = if (us.isShowLocoNorma) 1L else 0L,
            isShowOtherCurrent = if (us.isShowOtherCurrent) 1L else 0L,
            country = us.country,
            crossMonthTimezone = us.crossMonthTimezone.name,
            useStandardTimePicker = if (us.useStandardTimePicker) 1L else 0L,
            region = us.region,
            isShowTrain = if (us.isShowTrain) 1L else 0L,
            isShowOtherWork = if (us.isShowOtherWork) 1L else 0L,
            otherWorkTypeList = SettingsMapper.encodeStringList(us.otherWorkTypeList),
            isShowLocomotive = if (us.isShowLocomotive) 1L else 0L,
            isShowPassenger = if (us.isShowPassenger) 1L else 0L,
            isShowPartner = if (us.isShowPartner) 1L else 0L
        )
    }

    override fun setSettings(userSettings: UserSettings): Flow<ResultState<Unit>> {
        return flowRequest { insertUserSettings(userSettings) }
    }

    override fun getFlowSettingsState(): Flow<ResultState<UserSettings>> {
        return db.userSettingsQueries.getAll()
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { row ->
                if (row != null) {
                    ResultState.Success(SettingsMapper.toData(row))
                } else {
                    ResultState.Error(ErrorEntity(message = "Настройки пользователя не найдены в локальной БД"))
                }
            }
    }

    override fun getUserSettingFlow(): Flow<UserSettings> {
        return db.userSettingsQueries.getAll()
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { row ->
                if (row != null) SettingsMapper.toData(row) else UserSettings()
            }
    }

    override fun getUserSettings(): UserSettings {
        val row = db.userSettingsQueries.getAll().executeAsOneOrNull()
        return if (row != null) SettingsMapper.toData(row) else UserSettings()
    }

    override fun setUpdateAt(timestamp: Long): Flow<ResultState<Unit>> {
        return flowRequest {
            val current = getUserSettings()
            insertUserSettings(current.copy(updateAt = timestamp))
        }
    }

    override fun setWorkTimeDefault(timeInMillis: Long): Flow<ResultState<Unit>> {
        return flowRequest {
            val current = getUserSettings()
            insertUserSettings(current.copy(defaultWorkTime = timeInMillis))
        }
    }

    override fun updateNightTime(nightTime: NightTime): Flow<ResultState<Unit>> {
        return flowRequest {
            val current = getUserSettings()
            insertUserSettings(current.copy(nightTime = nightTime))
        }
    }

    override fun updateMonthOfYearInUserSetting(monthOfYear: MonthOfYear): Flow<ResultState<Unit>> {
        return flowRequest {
            val current = getUserSettings()
            insertUserSettings(current.copy(selectMonthOfYear = monthOfYear))
        }
    }

    override fun setCurrentMonthOfYear(monthOfYear: MonthOfYear): Flow<ResultState<Unit>> {
        return updateMonthOfYearInUserSetting(monthOfYear)
    }

    override fun setDieselCoefficient(value: Double): Flow<ResultState<Unit>> {
        return flowRequest {
            val current = getUserSettings()
            insertUserSettings(current.copy(lastEnteredDieselCoefficient = value))
        }
    }

    override fun updateSubscriptionPeriod(time: Long): Flow<ResultState<Unit>> {
        return flowRequest {
            val current = getUserSettings()
            insertUserSettings(current.copy(subscriptionPeriod = time))
        }
    }

    override fun setStations(stations: List<String>): Flow<ResultState<Unit>> {
        return flowRequest {
            val current = getUserSettings()
            insertUserSettings(current.copy(stationList = stations))
        }
    }

    override fun getStations(): List<String> = getUserSettings().stationList

    override fun setLocomotiveSeriesList(locomotiveSeries: List<String>): Flow<ResultState<Unit>> {
        return flowRequest {
            val current = getUserSettings()
            insertUserSettings(current.copy(locomotiveSeriesList = locomotiveSeries))
        }
    }

    override fun getLocomotiveSeriesList(): List<String> = getUserSettings().locomotiveSeriesList

    override fun setShowBreak(value: Boolean): Flow<ResultState<Unit>> {
        return flowRequest {
            val current = getUserSettings()
            insertUserSettings(current.copy(isShowBreak = value))
        }
    }

    override fun clearRepository(): Flow<ResultState<Unit>> {
        return flowRequest { db.monthOfYearQueries.deleteAll() }
    }
}
