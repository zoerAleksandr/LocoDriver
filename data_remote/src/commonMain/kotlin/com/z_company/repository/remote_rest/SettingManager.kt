package com.z_company.repository.remote_rest

import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.ProductionCalendarDay
import com.z_company.domain.entities.ReleaseDay
import com.z_company.domain.entities.norma_time.LocomotiveSeries
import com.z_company.domain.entities.norma_time.StationNorm
import com.z_company.domain.entities.partner.Partner
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

/**
 * Менеджер синхронизации настроек.
 * Шаг 2 KMP-миграции: object → class с инжекцией зависимостей через Koin.
 */
class SettingManager(
    private val remoteRestApi: RemoteRestApi
) {

    fun saveUserSettingInRemote(
        userSettings: UserSettings,
        bearerToken: String
    ): Flow<ResultState<Unit>> = flow {
        emit(ResultState.Loading())
        remoteRestApi.saveUserSetting(token = bearerToken, body = userSettings)
        emit(ResultState.Success(Unit))
    }.catch { e ->
        emit(ResultState.Error(ErrorEntity(message = NetworkErrorMapper.humanMessage(e), throwable = e)))
    }

    fun getUserSettingFromRemote(
        bearerToken: String
    ): Flow<ResultState<UserSettings>> = flow {
        emit(ResultState.Loading())
        val setting = remoteRestApi.getUserSetting(token = bearerToken)
        emit(ResultState.Success(setting))
    }.catch { e ->
        emit(ResultState.Error(ErrorEntity(message = NetworkErrorMapper.humanMessage(e), throwable = e)))
    }

    fun saveSalarySettingInRemote(
        salarySetting: SalarySetting,
        bearerToken: String
    ): Flow<ResultState<Unit>> = flow {
        emit(ResultState.Loading())
        remoteRestApi.saveSalarySetting(token = bearerToken, body = salarySetting)
        emit(ResultState.Success(Unit))
    }.catch { e ->
        emit(ResultState.Error(ErrorEntity(message = NetworkErrorMapper.humanMessage(e), throwable = e)))
    }

    fun getSalarySettingFromRemote(
        bearerToken: String
    ): Flow<ResultState<SalarySetting>> = flow {
        emit(ResultState.Loading())
        val setting = remoteRestApi.getSalarySetting(token = bearerToken)
        emit(ResultState.Success(setting))
    }.catch { e ->
        emit(ResultState.Error(ErrorEntity(message = NetworkErrorMapper.humanMessage(e), throwable = e)))
    }

    fun saveMonthOfYearListInRemote(
        monthOfYearList: List<MonthOfYear>,
        bearerToken: String
    ): Flow<ResultState<Unit>> = flow {
        emit(ResultState.Loading())
        remoteRestApi.saveMonthOfYearList(token = bearerToken, body = monthOfYearList)
        emit(ResultState.Success(Unit))
    }.catch { e ->
        emit(ResultState.Error(ErrorEntity(message = NetworkErrorMapper.humanMessage(e), throwable = e)))
    }

    fun getMonthOfYearListFromRemote(
        bearerToken: String
    ): Flow<ResultState<List<MonthOfYear>>> = flow {
        emit(ResultState.Loading())
        val months = remoteRestApi.getMonthOfYearList(token = bearerToken)
        emit(ResultState.Success(months))
    }.catch { e ->
        emit(ResultState.Error(ErrorEntity(message = NetworkErrorMapper.humanMessage(e), throwable = e)))
    }

    // --- ReleaseDay ---

    fun saveReleaseDaysInRemote(
        days: List<ReleaseDay>,
        bearerToken: String
    ): Flow<ResultState<Unit>> = flow {
        emit(ResultState.Loading())
        remoteRestApi.saveReleaseDays(token = bearerToken, body = days)
        emit(ResultState.Success(Unit))
    }.catch { e ->
        emit(ResultState.Error(ErrorEntity(message = NetworkErrorMapper.humanMessage(e), throwable = e)))
    }

    fun getReleaseDaysFromRemote(
        bearerToken: String
    ): Flow<ResultState<List<ReleaseDay>>> = flow {
        emit(ResultState.Loading())
        val days = remoteRestApi.getReleaseDays(token = bearerToken)
        emit(ResultState.Success(days))
    }.catch { e ->
        emit(ResultState.Error(ErrorEntity(message = NetworkErrorMapper.humanMessage(e), throwable = e)))
    }

    // --- NormaTime ---

    fun saveNormaTimeLocomotivesInRemote(
        series: List<LocomotiveSeries>,
        bearerToken: String
    ): Flow<ResultState<Unit>> = flow {
        emit(ResultState.Loading())
        remoteRestApi.saveNormaTimeLocomotives(token = bearerToken, body = series)
        emit(ResultState.Success(Unit))
    }.catch { e ->
        emit(ResultState.Error(ErrorEntity(message = NetworkErrorMapper.humanMessage(e), throwable = e)))
    }

    fun getNormaTimeLocomotivesFromRemote(
        bearerToken: String
    ): Flow<ResultState<List<LocomotiveSeries>>> = flow {
        emit(ResultState.Loading())
        val series = remoteRestApi.getNormaTimeLocomotives(token = bearerToken)
        emit(ResultState.Success(series))
    }.catch { e ->
        emit(ResultState.Error(ErrorEntity(message = NetworkErrorMapper.humanMessage(e), throwable = e)))
    }

    fun saveNormaTimeStationsInRemote(
        stations: List<StationNorm>,
        bearerToken: String
    ): Flow<ResultState<Unit>> = flow {
        emit(ResultState.Loading())
        remoteRestApi.saveNormaTimeStations(token = bearerToken, body = stations)
        emit(ResultState.Success(Unit))
    }.catch { e ->
        emit(ResultState.Error(ErrorEntity(message = NetworkErrorMapper.humanMessage(e), throwable = e)))
    }

    fun getNormaTimeStationsFromRemote(
        bearerToken: String
    ): Flow<ResultState<List<StationNorm>>> = flow {
        emit(ResultState.Loading())
        val stations = remoteRestApi.getNormaTimeStations(token = bearerToken)
        emit(ResultState.Success(stations))
    }.catch { e ->
        emit(ResultState.Error(ErrorEntity(message = NetworkErrorMapper.humanMessage(e), throwable = e)))
    }

    // --- Partners (справочник напарников) ---

    fun savePartnersInRemote(
        partners: List<Partner>,
        bearerToken: String
    ): Flow<ResultState<Unit>> = flow {
        emit(ResultState.Loading())
        remoteRestApi.savePartners(token = bearerToken, body = partners)
        emit(ResultState.Success(Unit))
    }.catch { e ->
        emit(ResultState.Error(ErrorEntity(message = NetworkErrorMapper.humanMessage(e), throwable = e)))
    }

    fun getPartnersFromRemote(
        bearerToken: String
    ): Flow<ResultState<List<Partner>>> = flow {
        emit(ResultState.Loading())
        val partners = remoteRestApi.getPartners(token = bearerToken)
        emit(ResultState.Success(partners))
    }.catch { e ->
        emit(ResultState.Error(ErrorEntity(message = NetworkErrorMapper.humanMessage(e), throwable = e)))
    }

    // --- ProductionCalendar ---

    fun getProductionCalendarFromRemote(
        country: String,
        year: Int
    ): Flow<ResultState<List<ProductionCalendarDay>>> = flow {
        emit(ResultState.Loading())
        val days = remoteRestApi.getProductionCalendar(country = country, year = year)
        emit(ResultState.Success(days))
    }.catch { e ->
        emit(ResultState.Error(ErrorEntity(message = NetworkErrorMapper.humanMessage(e), throwable = e)))
    }
}
