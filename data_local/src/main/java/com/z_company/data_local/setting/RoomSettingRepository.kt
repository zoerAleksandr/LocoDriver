package com.z_company.data_local.setting

import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.core.ResultState.Companion.flowMap
import com.z_company.core.ResultState.Companion.flowRequest
import com.z_company.data_local.setting.dao.SettingsDao
import com.z_company.data_local.setting.entity_converter.MonthOfYearConverter
import com.z_company.data_local.setting.entity_converter.NightTimeConverter
import com.z_company.data_local.setting.entity_converter.UserSettingsConverter
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.setting.NightTime
import com.z_company.domain.entities.setting.SETTINGS_KEY
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.repositories.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.lang.Exception

class RoomSettingRepository : SettingsRepository, KoinComponent {
    private val dao: SettingsDao by inject()

    override fun updateSubscriptionPeriod(
        time: Long,
    ): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.updateSubscriptionPeriod(time = time, key = SETTINGS_KEY)
        }
    }

    override fun setDieselCoefficient(value: Double): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.setDieselCoefficient(coefficient = value, key = SETTINGS_KEY)
        }
    }

    override fun updateMonthOfYearInUserSetting(monthOfYear: MonthOfYear): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.updateMonthOfYearInUserSetting(
                monthOfYear = MonthOfYearConverter.fromData(monthOfYear),
                key = SETTINGS_KEY
            )
        }
    }

    override fun updateNightTime(nightTime: NightTime): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.updateNightTime(
                nightTime = NightTimeConverter.fromData(nightTime),
                key = SETTINGS_KEY
            )
        }
    }

    override fun setSettings(userSettings: UserSettings): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.saveSettings(UserSettingsConverter.fromData(userSettings))
        }
    }

//    override fun getFlowSettingsState(): Flow<ResultState<UserSettings>> {
//        return flowMap {
//            dao.getFlowSettings().map { settings ->
//                if (settings != null) {
//                    ResultState.Success(UserSettingsConverter.toData(settings))
//                } else {
//                    val defaultSettings =
//                        UserSettingsConverter.fromData(UserSettings())  // Создаём default (предполагаю, что конструктор с дефолтами; адаптируйте, если нужно задать поля явно, например, subscriptionPeriod = 0L)
////                    dao.saveSettings(defaultSettings)  // Вставляем в базу (добавьте @Insert метод в DAO, если нет)
//                    ResultState.Success(UserSettingsConverter.toData(defaultSettings))
//                }
//            }
//        }
//    }

    override fun getFlowSettingsState(): Flow<ResultState<UserSettings>> {
        return dao.getFlowSettings().map { setting ->
            if (setting != null) {
                // Изменено: Если настройки найдены, возвращаем Success с конвертированными данными.
                // Для чего: Чтобы сохранить исходную логику, но в формате ResultState для лучшей обработки ошибок.
                ResultState.Success(UserSettingsConverter.toData(setting))
            } else {
                // Изменено: Если null, возвращаем Error вместо автоматического создания дефолтов.
                // Для чего: Чтобы избежать неожиданных перезаписей дефолтами. Теперь вызывающий код (например, SettingsUseCase) может решить, создавать дефолты или показать ошибку пользователю (например, "Настройки не найдены, перелогиньтесь"). Это делает функцию более предсказуемой и снижает риски, как в вашей проблеме с подпиской.
                ResultState.Error(ErrorEntity(message = "Настройки пользователя не найдены в локальной БД"))
            }
        }
    }

    override fun getUserSettingFlow(): Flow<UserSettings> {
        return dao.getFlowSettings().map { setting ->
            if (setting != null) {
                UserSettingsConverter.toData(setting)
            } else {
                val defaultSettings = UserSettingsConverter.fromData(UserSettings())  // Создаём default (предполагаю, что конструктор с дефолтами; адаптируйте, если нужно задать поля явно, например, subscriptionPeriod = 0L)
                UserSettingsConverter.toData(defaultSettings)
            }
        }
    }

    override fun getUserSettings(): UserSettings {
        return UserSettingsConverter.toData(dao.getUserSettings())
    }

    override fun setUpdateAt(timestamp: Long): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.setUpdateAt(timestamp = timestamp, key = SETTINGS_KEY)
        }
    }

    override fun setWorkTimeDefault(timeInMillis: Long): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.setWorkTimeDefault(timeInMillis = timeInMillis, key = SETTINGS_KEY)
        }
    }

    override fun setCurrentMonthOfYear(monthOfYear: MonthOfYear): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.setCurrentMonthOfYear(
                monthOfYear = MonthOfYearConverter.fromData(monthOfYear),
                key = SETTINGS_KEY
            )
        }
    }

    override fun clearRepository(): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.clearCalendar()
        }
    }

    override fun setStations(stations: List<String>): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.setStationList(stations = stations, key = SETTINGS_KEY)
        }
    }

    override fun getStations(): List<String> {
        return dao.getStations()
    }

    override fun setLocomotiveSeriesList(locomotiveSeries: List<String>): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.setLocomotiveSeriesList(locomotiveSeries = locomotiveSeries, key = SETTINGS_KEY)
        }
    }

    override fun getLocomotiveSeriesList(): List<String> {
        return dao.getLocomotiveSeriesList()
    }
}