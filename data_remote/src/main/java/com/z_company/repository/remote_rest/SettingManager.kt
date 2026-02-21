package com.z_company.repository.remote_rest

import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
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
        try {
            remoteRestApi.saveUserSetting(token = bearerToken, body = userSettings)
            emit(ResultState.Success(Unit))
        } catch (e: Exception) {
            emit(ResultState.Error(ErrorEntity(throwable = e)))
        }
    }

    fun getUserSettingFromRemote(
        bearerToken: String
    ): Flow<ResultState<UserSettings>> = flow {
        emit(ResultState.Loading())
        val setting = remoteRestApi.getUserSetting(token = bearerToken)
        emit(ResultState.Success(setting))
    }.catch { e ->
        emit(ResultState.Error(ErrorEntity(throwable = e)))
    }

    fun saveSalarySettingInRemote(
        salarySetting: SalarySetting,
        bearerToken: String
    ): Flow<ResultState<Unit>> = flow {
        emit(ResultState.Loading())
        remoteRestApi.saveSalarySetting(token = bearerToken, body = salarySetting)
        emit(ResultState.Success(Unit))
    }.catch { e ->
        emit(ResultState.Error(ErrorEntity(throwable = e)))
    }

    fun getSalarySettingFromRemote(
        bearerToken: String
    ): Flow<ResultState<SalarySetting>> = flow {
        emit(ResultState.Loading())
        val setting = remoteRestApi.getSalarySetting(token = bearerToken)
        emit(ResultState.Success(setting))
    }.catch { e ->
        emit(ResultState.Error(ErrorEntity(throwable = e)))
    }

    fun saveMonthOfYearListInRemote(
        monthOfYearList: List<MonthOfYear>,
        bearerToken: String
    ): Flow<ResultState<Unit>> = flow {
        emit(ResultState.Loading())
        remoteRestApi.saveMonthOfYearList(token = bearerToken, body = monthOfYearList)
        emit(ResultState.Success(Unit))
    }.catch { e ->
        emit(ResultState.Error(ErrorEntity(throwable = e)))
    }

    fun getMonthOfYearListFromRemote(
        bearerToken: String
    ): Flow<ResultState<List<MonthOfYear>>> = flow {
        emit(ResultState.Loading())
        val months = remoteRestApi.getMonthOfYearList(token = bearerToken)
        emit(ResultState.Success(months))
    }.catch { e ->
        emit(ResultState.Error(ErrorEntity(throwable = e)))
    }
}
