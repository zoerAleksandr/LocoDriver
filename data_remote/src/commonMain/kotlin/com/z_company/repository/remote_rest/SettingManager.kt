package com.z_company.repository.remote_rest

import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.ProductionCalendarDay
import com.z_company.domain.entities.ReleaseDay
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
        emit(ResultState.Error(ErrorEntity(throwable = e, appError = e.toAppError())))
    }

    fun getUserSettingFromRemote(
        bearerToken: String
    ): Flow<ResultState<UserSettings>> = flow {
        emit(ResultState.Loading())
        val setting = remoteRestApi.getUserSetting(token = bearerToken)
        emit(ResultState.Success(setting))
    }.catch { e ->
        emit(ResultState.Error(ErrorEntity(throwable = e, appError = e.toAppError())))
    }

    fun saveSalarySettingInRemote(
        salarySetting: SalarySetting,
        bearerToken: String
    ): Flow<ResultState<Unit>> = flow {
        emit(ResultState.Loading())
        remoteRestApi.saveSalarySetting(token = bearerToken, body = salarySetting)
        emit(ResultState.Success(Unit))
    }.catch { e ->
        emit(ResultState.Error(ErrorEntity(throwable = e, appError = e.toAppError())))
    }

    fun getSalarySettingFromRemote(
        bearerToken: String
    ): Flow<ResultState<SalarySetting>> = flow {
        emit(ResultState.Loading())
        val setting = remoteRestApi.getSalarySetting(token = bearerToken)
        emit(ResultState.Success(setting))
    }.catch { e ->
        emit(ResultState.Error(ErrorEntity(throwable = e, appError = e.toAppError())))
    }

    fun saveMonthOfYearListInRemote(
        monthOfYearList: List<MonthOfYear>,
        bearerToken: String
    ): Flow<ResultState<Unit>> = flow {
        emit(ResultState.Loading())
        remoteRestApi.saveMonthOfYearList(token = bearerToken, body = monthOfYearList)
        emit(ResultState.Success(Unit))
    }.catch { e ->
        emit(ResultState.Error(ErrorEntity(throwable = e, appError = e.toAppError())))
    }

    fun getMonthOfYearListFromRemote(
        bearerToken: String
    ): Flow<ResultState<List<MonthOfYear>>> = flow {
        emit(ResultState.Loading())
        val months = remoteRestApi.getMonthOfYearList(token = bearerToken)
        emit(ResultState.Success(months))
    }.catch { e ->
        emit(ResultState.Error(ErrorEntity(throwable = e, appError = e.toAppError())))
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
        emit(ResultState.Error(ErrorEntity(throwable = e, appError = e.toAppError())))
    }

    fun getReleaseDaysFromRemote(
        bearerToken: String
    ): Flow<ResultState<List<ReleaseDay>>> = flow {
        emit(ResultState.Loading())
        val days = remoteRestApi.getReleaseDays(token = bearerToken)
        emit(ResultState.Success(days))
    }.catch { e ->
        emit(ResultState.Error(ErrorEntity(throwable = e, appError = e.toAppError())))
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
        emit(ResultState.Error(ErrorEntity(throwable = e, appError = e.toAppError())))
    }
}
