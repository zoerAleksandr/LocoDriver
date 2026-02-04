package com.z_company.repository.remote_rest

import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.entities.setting.NightTime
import com.z_company.domain.entities.setting.SETTINGS_KEY
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.entities.setting.hourInMillis20
import com.z_company.domain.entities.setting.hourInMillis8
import com.z_company.domain.entities.setting.timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue

object SettingManager : KoinComponent {
    private val remoteRestApi: RemoteRestApi by inject()

    fun saveUserSettingInRemote(
        userSettings: UserSettings,
        bearerToken: String
    ): Flow<ResultState<Unit>> = flow {
        emit(ResultState.Loading())
        try {
            val response = remoteRestApi.saveUserSetting(
                token = bearerToken,
                body = userSettings
            )
            if (response.isSuccessful) {
                emit(ResultState.Success(Unit))
            } else {
                emit(ResultState.Error(ErrorEntity(message = response.code().toString())))
            }
        } catch (e: Exception) {
            emit(ResultState.Error(ErrorEntity(throwable = e)))  // Другие ошибки
        }
    }

    fun getUserSettingFromRemote(
        bearerToken: String
    ): Flow<ResultState<UserSettings>> = flow {
        emit(ResultState.Loading())

        val response = remoteRestApi.getUserSetting(
            token = bearerToken,
        )
        if (response.isSuccessful) {
            val setting = response.body() ?: UserSettings()
            emit(ResultState.Success(setting))
        } else {
            emit(ResultState.Error(ErrorEntity(message = response.code().toString())))
        }
    }.catch { e ->
        emit(ResultState.Error(ErrorEntity(throwable = e)))  // Другие ошибки
    }

    fun saveSalarySettingInRemote(
        salarySetting: SalarySetting,
        bearerToken: String
    ): Flow<ResultState<Unit>> = flow {
        emit(ResultState.Loading())
        val response = remoteRestApi.saveSalarySetting(
            token = bearerToken,
            body = salarySetting
        )
        if (response.isSuccessful) {
            emit(ResultState.Success(Unit))
        } else {
            emit(ResultState.Error(ErrorEntity(message = response.code().toString())))
        }

    }.catch { e ->
        emit(ResultState.Error(ErrorEntity(throwable = e)))  // Другие ошибки
    }

    fun getSalarySettingFromRemote(
        bearerToken: String
    ): Flow<ResultState<SalarySetting>> = flow {
        emit(ResultState.Loading())
        val response = remoteRestApi.getSalarySetting(
            token = bearerToken,
        )
        if (response.isSuccessful) {
            val setting = response.body() ?: SalarySetting()
            emit(ResultState.Success(setting))
        } else {
            emit(ResultState.Error(ErrorEntity(message = response.code().toString())))
        }

    }.catch { e ->
        emit(ResultState.Error(ErrorEntity(throwable = e)))  // Другие ошибки
    }


    fun saveMonthOfYearListInRemote(
        monthOfYearList: List<MonthOfYear>,
        bearerToken: String
    ): Flow<ResultState<Unit>> = flow {
        emit(ResultState.Loading())
        val response = remoteRestApi.saveMonthOfYearList(
            token = bearerToken,
            body = monthOfYearList
        )
        if (response.isSuccessful) {
            emit(ResultState.Success(Unit))
        } else {
            emit(ResultState.Error(ErrorEntity(message = response.code().toString())))
        }
    }.catch { e ->
        emit(ResultState.Error(ErrorEntity(throwable = e)))
    }

    fun getMonthOfYearListFromRemote(
        bearerToken: String
    ): Flow<ResultState<List<MonthOfYear>>> = flow {
        emit(ResultState.Loading())
        val response = remoteRestApi.getMonthOfYearList(
            token = bearerToken,
        )
        if (response.isSuccessful) {
            val months = response.body() ?: emptyList()
            emit(ResultState.Success(months))
        } else {
            emit(ResultState.Error(ErrorEntity(message = response.code().toString())))
        }

    }.catch { e ->
        emit(ResultState.Error(ErrorEntity(throwable = e)))  // Другие ошибки
    }
}