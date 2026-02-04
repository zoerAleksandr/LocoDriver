package com.z_company.repository.remote_rest

import android.util.Log
import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar
import java.util.Calendar.MONTH
import java.util.Calendar.YEAR
import java.util.Date

// Data class для результата сохранения на сервер (upload).
// Описание: Содержит флаги успеха для каждой части данных, количество сохраненных маршрутов (-1 при ошибке) и timestamp (только если все успешно).
data class SyncUploadResult(
    var userSettingsSaved: Boolean = false,
    var salarySettingsSaved: Boolean = false,
    var monthsSaved: Boolean = false,
    var routesSavedCount: Int = -1,
    var timestamp: Long? = null  // Дата и время успешной полной синхронизации (millis)
)

// Data class для результата загрузки с сервера (download).
// Описание: Содержит флаги успеха для каждой части данных и количество загруженных маршрутов (-1 при ошибке).
data class SyncDownloadResult(
    var userSettingsLoaded: Boolean = false,
    var salarySettingsLoaded: Boolean = false,
    var monthsLoaded: Boolean = false,
    var routesLoadedCount: Int = -1
)

// Класс SyncManager для управления синхронизацией данных.
// Описание: Гарантирует последовательное сохранение/загрузку всех данных (настройки, месяцы, маршруты).
// Использует Flow для асинхронной обработки и возврата состояний (Loading, Success, Error).
// Инжектирует необходимые use cases и менеджеры через Koin.
class SyncManager : KoinComponent {
    private val settingsUseCase: SettingsUseCase by inject()
    private val salarySettingUseCase: SalarySettingUseCase by inject()
    private val calendarUseCase: CalendarUseCase by inject()
    private val routeUseCase: RouteUseCase by inject()
    private val routesManager: RoutesManager by inject()
    private val settingManager: SettingManager by inject()
    private val sharedPrefs: SharedPreferencesRepositories by inject()

    // Функция для сохранения всех данных на сервер (sync to remote).
    // Описание: Последовательно сохраняет настройки пользователя, зарплатные настройки, список месяцев и все локальные маршруты.
    // Возвращает Flow<ResultState<SyncUploadResult>> с детализацией.
    // Если все успешно — сохраняет timestamp в SharedPreferences.
    // bearerToken — токен авторизации для запросов на сервер.
    fun syncToRemote(bearerToken: String): Flow<ResultState<SyncUploadResult>> = flow {
        emit(ResultState.Loading())  // Начало процесса

        val result = SyncUploadResult()

        // 1. Сохранение UserSettings
        val localUserSettings = settingsUseCase.getUserSettingFlow().first()
        settingManager.saveUserSettingInRemote(localUserSettings, bearerToken)
            .catch { e ->
                emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения UserSettings: ${e.message}")))
                return@catch
            }
            .collect { saveState ->
                if (saveState is ResultState.Success) {
                    result.userSettingsSaved = true
                    emit(ResultState.Success(result.copy()))
                } else if (saveState is ResultState.Error) {
                    emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения UserSettings: ${saveState.entity.message}")))
                    return@collect
                }
            }

        // 2. Сохранение SalarySetting
        val localSalarySetting = salarySettingUseCase.salarySettingFlow().first()
        settingManager.saveSalarySettingInRemote(localSalarySetting, bearerToken)
            .catch { e ->
                emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения SalarySetting: ${e.message}")))
                return@catch
            }
            .collect { saveState ->
                if (saveState is ResultState.Success) {
                    result.salarySettingsSaved = true
                    emit(ResultState.Success(result.copy()))
                } else if (saveState is ResultState.Error) {
                    emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения SalarySetting: ${saveState.entity.message}")))
                    return@collect
                }
            }

        // 3. Сохранение MonthOfYearList
        val localMonths = calendarUseCase.loadFlowMonthOfYearListState().first()
        settingManager.saveMonthOfYearListInRemote(localMonths, bearerToken)
            .catch { e ->
                emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения MonthOfYearList: ${e.message}")))
                return@catch
            }
            .collect { saveState ->
                if (saveState is ResultState.Success) {
                    result.monthsSaved = true
                    emit(ResultState.Success(result.copy()))
                } else if (saveState is ResultState.Error) {
                    emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения MonthOfYearList: ${saveState.entity.message}")))
                    return@collect
                }
            }

        // 4. Сохранение всех маршрутов (аналогично startMigration)
        val localRoutesResult = routeUseCase.getListRoutesAsFlow()
            .first()  // Предполагаю, что getAllRoutes() возвращает Flow<ResultState<List<Route>>>
        val routes = localRoutesResult
        var savedCount = 0
        var hasError = false
        for (route in routes) {
            if (!route.basicData.isSynchronized) {
                routesManager.saveRouteInRemote(route, bearerToken)
                    .catch { e ->
                        hasError = true
                        emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения маршрута ${route.basicData.id}: ${e.message}")))
                        return@catch
                    }
                    .collect { saveResult ->
                        if (saveResult is ResultState.Success) {
                            routeUseCase.setSynchronizedRoute(route.basicData.id).collect {
                                Log.d("zzz", "setSynchronizedRoute $it")
                            }
                            savedCount++
                            // помечаем маршрут как синхронизированый
                        } else if (saveResult is ResultState.Error) {
                            hasError = true
                            Log.d(
                                "zzz",
                                "Ошибка сохранения маршрута \${route.basicData.id}: \${e.message}"
                            )
                            emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения маршрута ${route.basicData.id}: ${saveResult.entity.message}")))
                            return@collect
                        }
                    }
            }
        }
        if (!hasError) {
            result.routesSavedCount = savedCount
            emit(ResultState.Success(result.copy()))
        }

        // Если все части успешны — сохраняем timestamp и эмитим Success
        if (result.userSettingsSaved && result.salarySettingsSaved && result.monthsSaved && result.routesSavedCount >= 0) {
            val timestamp = Date().time
            sharedPrefs.setLastSyncTimestamp(timestamp)
            emit(ResultState.Success(result.copy(timestamp = timestamp)))
        } else {
            emit(ResultState.Error(ErrorEntity(message = "Не все данные сохранены успешно")))
        }
    }.flowOn(Dispatchers.IO)  // Выполнение в IO-диспетчере

    // Функция для загрузки всех данных с сервера и сохранения локально (sync from remote).
    // Описание: Последовательно загружает список месяцев, зарплатные настройки, настройки пользователя и все маршруты с сервера.
    // Сохраняет их локально через use cases.
    // Возвращает Flow<ResultState<SyncDownloadResult>> с детализацией.
    // bearerToken — токен авторизации для запросов на сервер.
    fun syncFromRemote(bearerToken: String): Flow<ResultState<SyncDownloadResult>> = flow {
        emit(ResultState.Loading())  // Начало процесса

        val result = SyncDownloadResult()

        settingManager.getMonthOfYearListFromRemote(bearerToken)
            .catch { e ->
                emit(ResultState.Error(ErrorEntity(message = "Ошибка загрузки MonthOfYearList: ${e.message}")))
                return@catch  // Изменено: Заменил return@flow на return@catch. // Для чего: Чтобы прервать только обработку этого Flow в catch, а не весь внешний flow, как подсказывает IDEA. Это делает возврат более локальным и избегает non-local return warnings.
            }
            .collect { loadState ->
                when (loadState) {  // Изменено: Заменил if-else на when для лучшей читаемости и обработки всех случаев.
                    is ResultState.Success -> {
                        calendarUseCase.saveCalendar(loadState.data)
                            .collect { saveResult ->  // Изменено: Добавил .collect {} для обработки результата сохранения локально.
                                when (saveResult) {  // Для чего: Чтобы гарантировать, что сохранение прошло успешно перед установкой флага monthsLoaded. Если ошибка сохранения — emit Error и прерываем flow.
                                    is ResultState.Success -> {
                                        result.monthsLoaded = true
                                        emit(ResultState.Success(result.copy()))
                                    }

                                    is ResultState.Error -> {
                                        emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения MonthOfYearList локально: ${saveResult.entity.message}")))
                                        return@collect  // Прерываем весь sync при ошибке сохранения
                                    }

                                    else -> {}  // Loading игнорируем
                                }
                            }
                    }

                    is ResultState.Error -> {
                        emit(ResultState.Error(ErrorEntity(message = "Ошибка загрузки MonthOfYearList: ${loadState.entity.message}")))
                        return@collect
                    }

                    else -> {}  // Loading или другие состояния
                }
            }

        // 2. Загрузка SalarySetting
        settingManager.getSalarySettingFromRemote(bearerToken)
            .catch { e ->
                emit(ResultState.Error(ErrorEntity(message = "Ошибка загрузки SalarySetting: ${e.message}")))
                return@catch  // Изменено: Заменил return@flow на return@catch. // Для чего: Чтобы прервать только обработку этого Flow в catch, а не весь внешний flow, делая возврат более локальным и избегая предупреждений IDE о non-local return.
            }
            .collect { loadState ->
                when (loadState) {  // Изменено: Заменил if-else на when для лучшей читаемости и обработки всех случаев.
                    is ResultState.Success -> {
                        salarySettingUseCase.saveSalarySetting(loadState.data)
                            .collect { saveResult ->  // Изменено: Добавил .collect {} для обработки результата сохранения локально.
                                when (saveResult) {  // Для чего: Чтобы гарантировать, что сохранение прошло успешно перед установкой флага salarySettingsLoaded. Если ошибка сохранения — emit Error и прерываем flow.
                                    is ResultState.Success -> {
                                        result.salarySettingsLoaded = true
                                        emit(ResultState.Success(result.copy()))
                                    }

                                    is ResultState.Error -> {
                                        emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения SalarySetting локально: ${saveResult.entity.message}")))
                                        return@collect  // Прерываем весь sync при ошибке сохранения
                                    }

                                    else -> {}  // Loading игнорируем
                                }
                            }
                    }

                    is ResultState.Error -> {
                        emit(ResultState.Error(ErrorEntity(message = "Ошибка загрузки SalarySetting: ${loadState.entity.message}")))
                        return@collect
                    }

                    else -> {}  // Loading или другие состояния
                }
            }

        // 3. Загрузка UserSettings
        settingManager.getUserSettingFromRemote(bearerToken)
            .catch { e ->
                emit(ResultState.Error(ErrorEntity(message = "Ошибка загрузки UserSettings: ${e.message}")))
                return@catch  // Изменено: Заменил return@flow на return@catch. // Для чего: Чтобы прервать только обработку этого Flow в catch, а не весь внешний flow, делая возврат более локальным и избегая предупреждений IDE о non-local return.
            }
            .collect { loadState ->
                when (loadState) {  // Изменено: Заменил if-else на when для лучшей читаемости и обработки всех случаев.
                    is ResultState.Success -> {
                        val listMonthOfYear = calendarUseCase.loadFlowMonthOfYearListState().first()
                        val currentCalendar = Calendar.getInstance()
                        val currentMonthOfYear = listMonthOfYear.find {
                            it.month == currentCalendar.get(MONTH) && it.year == currentCalendar.get(
                                YEAR
                            )
                        }

                        val userSettings = loadState.data.copy(
                            selectMonthOfYear = currentMonthOfYear ?: listMonthOfYear.first()
                        )

                        settingsUseCase.saveSetting(userSettings)
                            .collect { saveResult ->  // Изменено: Добавил .collect {} для обработки результата сохранения локально.
                                when (saveResult) {  // Для чего: Чтобы гарантировать, что сохранение прошло успешно перед установкой флага userSettingsLoaded. Если ошибка сохранения — emit Error и прерываем flow.
                                    is ResultState.Success -> {
                                        result.userSettingsLoaded = true
                                        emit(ResultState.Success(result.copy()))
                                    }

                                    is ResultState.Error -> {
                                        emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения UserSettings локально: ${saveResult.entity.message}")))
                                        return@collect  // Прерываем весь sync при ошибке сохранения
                                    }

                                    else -> {}  // Loading игнорируем
                                }
                            }
                    }

                    is ResultState.Error -> {
                        emit(ResultState.Error(ErrorEntity(message = "Ошибка загрузки UserSettings: ${loadState.entity.message}")))
                        return@collect
                    }

                    else -> {}  // Loading или другие состояния
                }
            }

        // 4. Загрузка всех маршрутов
        routesManager.getRoutesFromRemote(bearerToken)
            .catch { e ->
                emit(ResultState.Error(ErrorEntity(message = "Ошибка загрузки маршрутов: ${e.message}")))
                return@catch  // Изменено: Заменил return@flow на return@catch. // Для чего: Чтобы прервать только обработку этого Flow в catch, а не весь внешний flow, делая возврат более локальным и избегая предупреждений IDE о non-local return.
            }
            .collect { loadState ->
                when (loadState) {  // Изменено: Заменил if-else на when для лучшей читаемости и обработки всех случаев.
                    is ResultState.Success -> {
                        val routes = loadState.data
                        var savedCount = 0
                        for (route in routes) {
                            val r = route.copy(
                                basicData = route.basicData.copy(
                                    isSynchronized = true
                                )
                            )
                            routeUseCase.saveRouteAfterLoading(r)
                                .collect { saveResult ->  // Изменено: Обернул каждый saveRoute в .collect {} для обработки результата сохранения локально.
                                    when (saveResult) {  // Для чего: Чтобы гарантировать, что каждый маршрут сохранен успешно. Если ошибка в любом — emit Error и прерываем flow. savedCount инкрементируется только при Success.
                                        is ResultState.Success -> {
                                            savedCount++
                                        }

                                        is ResultState.Error -> {
                                            Log.d(
                                                "zzz",
                                                "Ошибка сохранения маршрута локально \${route.basicData.id}: \${e.message}"
                                            )

                                            emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения маршрута локально: ${saveResult.entity.message}")))

                                            return@collect  // Прерываем весь sync при ошибке сохранения любого маршрута
                                        }

                                        else -> {}  // Loading игнорируем
                                    }
                                }
                        }
                        result.routesLoadedCount = savedCount
                        emit(ResultState.Success(result.copy()))
                    }

                    is ResultState.Error -> {
                        emit(ResultState.Error(ErrorEntity(message = "Ошибка загрузки маршрутов: ${loadState.entity.message}")))
                        return@collect
                    }

                    else -> {}  // Loading или другие состояния
                }
            }

        // Если все успешно — эмитим Success, иначе Error
        if (result.userSettingsLoaded && result.salarySettingsLoaded && result.monthsLoaded && result.routesLoadedCount >= 0) {
            emit(ResultState.Success(result))
        } else {
            emit(ResultState.Error(ErrorEntity(message = "Не все данные загружены успешно")))
        }
    }.flowOn(Dispatchers.IO)  // Выполнение в IO-диспетчере
}