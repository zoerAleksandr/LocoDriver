@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.repository.remote_rest

import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.core.sendToSentry
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.ReleaseDayUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.entities.route.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.time.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// Data class для результата сохранения на сервер (upload).
data class SyncUploadResult(
    var userSettingsSaved: Boolean = false,
    var salarySettingsSaved: Boolean = false,
    var releaseDaysSaved: Boolean = false,
    var routesSavedCount: Int = -1,
    var timestamp: Long? = null,
    var routeWarnings: List<String> = emptyList(),
    var routeErrors: List<String> = emptyList()
)

// Data class для результата загрузки с сервера (download).
data class SyncDownloadResult(
    var userSettingsLoaded: Boolean = false,
    var salarySettingsLoaded: Boolean = false,
    var releaseDaysLoaded: Boolean = false,
    var routesLoadedCount: Int = -1
)

/**
 * Менеджер синхронизации данных.
 * Шаг 10 KMP-миграции: KoinComponent → конструкторная инжекция, Android API → KMP API.
 */
class SyncManager(
    private val settingsUseCase: SettingsUseCase,
    private val salarySettingUseCase: SalarySettingUseCase,
    private val calendarUseCase: CalendarUseCase,
    private val releaseDayUseCase: ReleaseDayUseCase,
    private val routeUseCase: RouteUseCase,
    private val routesManager: RoutesManager,
    private val settingManager: SettingManager,
    private val sharedPrefs: SharedPreferencesRepositories
) {

    fun syncToRemote(bearerToken: String): Flow<ResultState<SyncUploadResult>> = flow {
        emit(ResultState.Loading())

        val result = SyncUploadResult()

        // 1. Сохранение UserSettings
        val localUserSettingsState = settingsUseCase.getFlowCurrentSettingsState()
            .first { it is ResultState.Success || it is ResultState.Error }
        if (localUserSettingsState is ResultState.Success) {
            val localUserSettings = localUserSettingsState.data
            val subscriptionPeriod = localUserSettings.subscriptionPeriod
            if (subscriptionPeriod > Clock.System.now().toEpochMilliseconds()) {
                settingManager.saveUserSettingInRemote(localUserSettings, bearerToken)
                    .catch { e ->
                        emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения UserSettings: ${e.message ?: e.cause?.message ?: "Нет соединения"}")))
                        return@catch
                    }
                    .collect { saveState ->
                        if (saveState is ResultState.Success) {
                            result.userSettingsSaved = true
                            emit(ResultState.Success(result.copy()))
                        } else if (saveState is ResultState.Error) {
                            emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения UserSettings: ${saveState.entity.message ?: "Нет соединения"}")))
                            return@collect
                        }
                    }
            } else {
                result.userSettingsSaved = false
                emit(ResultState.Success(result.copy()))
            }
        } else {
            result.userSettingsSaved = false
            emit(ResultState.Success(result.copy()))
        }

        // 2. Сохранение SalarySetting
        val localSalarySetting = salarySettingUseCase.salarySettingFlow().first()
        settingManager.saveSalarySettingInRemote(localSalarySetting, bearerToken)
            .catch { e ->
                emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения SalarySetting: ${e.message ?: e.cause?.message ?: "Нет соединения"}")))
                return@catch
            }
            .collect { saveState ->
                if (saveState is ResultState.Success) {
                    result.salarySettingsSaved = true
                    emit(ResultState.Success(result.copy()))
                } else if (saveState is ResultState.Error) {
                    emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения SalarySetting: ${saveState.entity.message ?: "Нет соединения"}")))
                    return@collect
                }
            }

        // 2.5. Сохранение списка MonthOfYear (тарифные ставки) — best-effort
        val localMonthOfYearList = calendarUseCase.loadFlowMonthOfYearListState().first()
        if (localMonthOfYearList.isNotEmpty()) {
            settingManager.saveMonthOfYearListInRemote(localMonthOfYearList, bearerToken)
                .catch { /* Не прерываем синхронизацию */ }
                .collect {}
        }

        // 3. Сохранение дней отвлечений (ReleaseDay)
        val localReleaseDays = releaseDayUseCase.getAll()
        settingManager.saveReleaseDaysInRemote(localReleaseDays, bearerToken)
            .catch { e ->
                emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения дней отвлечений: ${e.message ?: e.cause?.message ?: "Нет соединения"}")))
                return@catch
            }
            .collect { saveState ->
                if (saveState is ResultState.Success) {
                    result.releaseDaysSaved = true
                    emit(ResultState.Success(result.copy()))
                } else if (saveState is ResultState.Error) {
                    emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения дней отвлечений: ${saveState.entity.message ?: "Нет соединения"}")))
                    return@collect
                }
            }

        val allWarnings = mutableListOf<String>()
        val allErrors = mutableListOf<String>()

        // 4. Удаление маршрутов, помеченных isDeleted = true
        val allRoutesWithDeleted = routeUseCase.listRouteWithDeleting()
        val deletedRoutes = allRoutesWithDeleted.filter { it.basicData.isDeleted }
        for (route in deletedRoutes) {
            val routeId = route.basicData.id
            val label = routeLabel(route)
            // Если маршрут никогда не был загружен на сервер (remoteRouteId is null/blank) —
            // не трогаем его: сервер о нём ничего не знает, а локально он уже помечен
            // isDeleted=true и скрыт из списков. Это защищает shared-preview маршруты
            // (импортированные по публичной ссылке и ожидающие решения пользователя)
            // от случайного удаления во время sync-а.
            if (route.basicData.remoteRouteId.isNullOrBlank()) {
                continue
            }
            try {
                routesManager.deleteRouteInRemote(routeId, bearerToken)
                    .collect { deleteResult ->
                        if (deleteResult is ResultState.Success) {
                            routeUseCase.removeRoute(route).collect {}
                        } else if (deleteResult is ResultState.Error) {
                            val msg = deleteResult.entity.message
                                ?: deleteResult.entity.throwable?.message ?: "Ошибка"
                            allErrors.add("[$routeId] Удаление $label: $msg")
                            deleteResult.entity.throwable?.sendToSentry(
                                "SyncManager", "deleteDeletedRoutes"
                            )
                        }
                    }
            } catch (e: Exception) {
                allErrors.add("[$routeId] Удаление $label: ${e.message ?: e.cause?.message ?: "Нет соединения"}")
                e.sendToSentry("SyncManager", "deleteDeletedRoutes")
            }
        }

        // 5. Сохранение всех маршрутов
        val routes = routeUseCase.getListRoutesAsFlow().first()
        var savedCount = 0
        for (route in routes) {
            if (!route.basicData.isSynchronized) {
                val routeId = route.basicData.id
                val label = routeLabel(route)
                routesManager.saveRouteInRemote(route, bearerToken)
                    .catch { e ->
                        allErrors.add("[$routeId] $label: ${e.message ?: e.cause?.message ?: "Нет соединения"}")
                        return@catch
                    }
                    .collect { saveResult ->
                        if (saveResult is ResultState.Success) {
                            val data = saveResult.data
                            if (data.warnings.isNotEmpty()) {
                                data.warnings.forEach { w -> allWarnings.add("[$routeId] $label: $w") }
                            }
                            routeUseCase.setSynchronizedRoute(routeId).collect {}
                            savedCount++
                        } else if (saveResult is ResultState.Error) {
                            allErrors.add("[$routeId] $label: ${saveResult.entity.message ?: saveResult.entity.throwable?.message ?: "Ошибка"}")
                        }
                    }
            }
        }
        result.routesSavedCount = savedCount
        result.routeWarnings = allWarnings
        result.routeErrors = allErrors
        emit(ResultState.Success(result.copy()))

        if (result.userSettingsSaved && result.salarySettingsSaved && result.releaseDaysSaved && result.routesSavedCount >= 0) {
            val timestamp = Clock.System.now().toEpochMilliseconds()
            sharedPrefs.setLastSyncTimestamp(timestamp)
            emit(ResultState.Success(result.copy(timestamp = timestamp)))
        } else if (allErrors.isNotEmpty()) {
            emit(ResultState.Error(ErrorEntity(message = allErrors.joinToString("\n"))))
        } else {
            emit(ResultState.Error(ErrorEntity(message = "Не все данные сохранены успешно")))
        }
    }.flowOn(Dispatchers.Default)

    fun syncFromRemote(bearerToken: String): Flow<ResultState<SyncDownloadResult>> = flow {
        emit(ResultState.Loading())

        val result = SyncDownloadResult()

        // 1. Загрузка дней отвлечений (ReleaseDay)
        settingManager.getReleaseDaysFromRemote(bearerToken)
            .catch { e ->
                emit(ResultState.Error(ErrorEntity(message = "Ошибка загрузки дней отвлечений: ${e.message ?: e.cause?.message ?: "Нет соединения"}")))
                return@catch
            }
            .collect { loadState ->
                when (loadState) {
                    is ResultState.Success -> {
                        releaseDayUseCase.replaceAllFromRemote(loadState.data)
                            .collect { saveResult ->
                                when (saveResult) {
                                    is ResultState.Success -> {
                                        result.releaseDaysLoaded = true
                                        emit(ResultState.Success(result.copy()))
                                    }
                                    is ResultState.Error -> {
                                        emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения дней отвлечений локально: ${saveResult.entity.message ?: "Нет соединения"}")))
                                        return@collect
                                    }
                                    else -> {}
                                }
                            }
                    }
                    is ResultState.Error -> {
                        emit(ResultState.Error(ErrorEntity(message = "Ошибка загрузки дней отвлечений: ${loadState.entity.message ?: "Нет соединения"}")))
                        return@collect
                    }
                    else -> {}
                }
            }

        // 2. Загрузка SalarySetting
        settingManager.getSalarySettingFromRemote(bearerToken)
            .catch { e ->
                emit(ResultState.Error(ErrorEntity(message = "Ошибка загрузки SalarySetting: ${e.message ?: e.cause?.message ?: "Нет соединения"}")))
                return@catch
            }
            .collect { loadState ->
                when (loadState) {
                    is ResultState.Success -> {
                        salarySettingUseCase.saveSalarySetting(loadState.data)
                            .collect { saveResult ->
                                when (saveResult) {
                                    is ResultState.Success -> {
                                        result.salarySettingsLoaded = true
                                        emit(ResultState.Success(result.copy()))
                                    }
                                    is ResultState.Error -> {
                                        emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения SalarySetting локально: ${saveResult.entity.message ?: "Нет соединения"}")))
                                        return@collect
                                    }
                                    else -> {}
                                }
                            }
                    }
                    is ResultState.Error -> {
                        emit(ResultState.Error(ErrorEntity(message = "Ошибка загрузки SalarySetting: ${loadState.entity.message ?: "Нет соединения"}")))
                        return@collect
                    }
                    else -> {}
                }
            }

        // 3. Загрузка UserSettings
        settingManager.getUserSettingFromRemote(bearerToken)
            .catch { e ->
                emit(ResultState.Error(ErrorEntity(message = "Ошибка загрузки UserSettings: ${e.message ?: e.cause?.message ?: "Нет соединения"}")))
                return@catch
            }
            .collect { loadState ->
                when (loadState) {
                    is ResultState.Success -> {
                        val listMonthOfYear = calendarUseCase.loadFlowMonthOfYearListState().first()
                        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                        // Calendar.MONTH — 0-based, kotlinx-datetime monthNumber — 1-based
                        val currentMonthOfYear = listMonthOfYear.find {
                            it.month == now.monthNumber - 1 && it.year == now.year
                        }
                        // Защита подписки: берём максимум из локального и серверного значения,
                        // чтобы не затереть локально сохранённую подписку.
                        val localSettings = settingsUseCase.getFlowCurrentSettingsState()
                            .first { it is ResultState.Success || it is ResultState.Error }
                        val localSubscriptionPeriod = (localSettings as? ResultState.Success)
                            ?.data?.subscriptionPeriod ?: 0L
                        val remoteSubscriptionPeriod = loadState.data.subscriptionPeriod
                        val mergedSubscriptionPeriod = maxOf(localSubscriptionPeriod, remoteSubscriptionPeriod)
                        val userSettings = loadState.data.copy(
                            selectMonthOfYear = currentMonthOfYear ?: listMonthOfYear.firstOrNull() ?: com.z_company.domain.entities.MonthOfYear(),
                            subscriptionPeriod = mergedSubscriptionPeriod
                        )
                        settingsUseCase.saveSetting(userSettings)
                            .collect { saveResult ->
                                when (saveResult) {
                                    is ResultState.Success -> {
                                        result.userSettingsLoaded = true
                                        emit(ResultState.Success(result.copy()))
                                    }
                                    is ResultState.Error -> {
                                        emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения UserSettings локально: ${saveResult.entity.message ?: "Нет соединения"}")))
                                        return@collect
                                    }
                                    else -> {}
                                }
                            }
                    }
                    is ResultState.Error -> {
                        emit(ResultState.Error(ErrorEntity(message = "Ошибка загрузки UserSettings: ${loadState.entity.message ?: "Нет соединения"}")))
                        return@collect
                    }
                    else -> {}
                }
            }

        // 3.5. Загрузка MonthOfYear (тарифные ставки) — best-effort
        // Обновляем только те месяцы, которые пришли с сервера; локальные не трогаем
        settingManager.getMonthOfYearListFromRemote(bearerToken)
            .catch { /* Не прерываем синхронизацию */ }
            .collect { loadState ->
                if (loadState is ResultState.Success && loadState.data.isNotEmpty()) {
                    calendarUseCase.saveCalendar(loadState.data).collect {}
                }
            }

        // 4. Загрузка всех маршрутов
        routesManager.getRoutesFromRemote(bearerToken)
            .catch { e ->
                emit(ResultState.Error(ErrorEntity(message = "Ошибка загрузки маршрутов: ${e.message ?: e.cause?.message ?: "Нет соединения"}")))
                return@catch
            }
            .collect { loadState ->
                when (loadState) {
                    is ResultState.Success -> {
                        val routes = loadState.data
                        var savedCount = 0
                        for (route in routes) {
                            val r = route.copy(
                                basicData = route.basicData.copy(isSynchronized = true)
                            )
                            routeUseCase.saveRouteAfterLoading(r)
                                .collect { saveResult ->
                                    when (saveResult) {
                                        is ResultState.Success -> { savedCount++ }
                                        is ResultState.Error -> {
                                            emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения маршрута локально: ${saveResult.entity.message ?: "Нет соединения"}")))
                                            return@collect
                                        }
                                        else -> {}
                                    }
                                }
                        }
                        result.routesLoadedCount = savedCount
                        emit(ResultState.Success(result.copy()))
                    }
                    is ResultState.Error -> {
                        emit(ResultState.Error(ErrorEntity(message = "Ошибка загрузки маршрутов: ${loadState.entity.message ?: "Нет соединения"}")))
                        return@collect
                    }
                    else -> {}
                }
            }

        if (result.userSettingsLoaded && result.salarySettingsLoaded && result.releaseDaysLoaded && result.routesLoadedCount >= 0) {
            emit(ResultState.Success(result))
        } else {
            emit(ResultState.Error(ErrorEntity(message = "Не все данные загружены успешно")))
        }
    }.flowOn(Dispatchers.Default)

    fun firstSyncAfterRegistration(bearerToken: String): Flow<ResultState<SyncUploadResult>> = flow {
        emit(ResultState.Loading())

        val result = SyncUploadResult()

        // 1. Сохранение UserSettings
        val localUserSettingsState = settingsUseCase.getFlowCurrentSettingsState()
            .first { it is ResultState.Success || it is ResultState.Error }
        if (localUserSettingsState is ResultState.Success) {
            val localUserSettings = localUserSettingsState.data
            val endTimeSubscription = sharedPrefs.getSubscriptionExpiration()
            val l = localUserSettings.copy(subscriptionPeriod = endTimeSubscription)
            settingManager.saveUserSettingInRemote(l, bearerToken)
                .catch { e ->
                    emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения UserSettings: ${e.message ?: e.cause?.message ?: "Нет соединения"}")))
                    return@catch
                }
                .collect { saveState ->
                    if (saveState is ResultState.Success) {
                        result.userSettingsSaved = true
                        emit(ResultState.Success(result.copy()))
                    } else if (saveState is ResultState.Error) {
                        emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения UserSettings: ${saveState.entity.message ?: "Нет соединения"}")))
                        return@collect
                    }
                }
        } else {
            result.userSettingsSaved = false
            emit(ResultState.Success(result.copy()))
        }

        // 2. Сохранение SalarySetting
        val localSalarySetting = salarySettingUseCase.salarySettingFlow().first()
        settingManager.saveSalarySettingInRemote(localSalarySetting, bearerToken)
            .catch { e ->
                emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения SalarySetting: ${e.message ?: e.cause?.message ?: "Нет соединения"}")))
                return@catch
            }
            .collect { saveState ->
                if (saveState is ResultState.Success) {
                    result.salarySettingsSaved = true
                    emit(ResultState.Success(result.copy()))
                } else if (saveState is ResultState.Error) {
                    emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения SalarySetting: ${saveState.entity.message ?: "Нет соединения"}")))
                    return@collect
                }
            }

        // 2.5. Сохранение списка MonthOfYear (тарифные ставки) — best-effort
        val localMonthOfYearListFirst = calendarUseCase.loadFlowMonthOfYearListState().first()
        if (localMonthOfYearListFirst.isNotEmpty()) {
            settingManager.saveMonthOfYearListInRemote(localMonthOfYearListFirst, bearerToken)
                .catch { /* Не прерываем синхронизацию */ }
                .collect {}
        }

        // 3. Сохранение дней отвлечений (ReleaseDay)
        val localReleaseDaysFirst = releaseDayUseCase.getAll()
        settingManager.saveReleaseDaysInRemote(localReleaseDaysFirst, bearerToken)
            .catch { e ->
                emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения дней отвлечений: ${e.message ?: e.cause?.message ?: "Нет соединения"}")))
                return@catch
            }
            .collect { saveState ->
                if (saveState is ResultState.Success) {
                    result.releaseDaysSaved = true
                    emit(ResultState.Success(result.copy()))
                } else if (saveState is ResultState.Error) {
                    emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения дней отвлечений: ${saveState.entity.message ?: "Нет соединения"}")))
                    return@collect
                }
            }

        // 4. Сохранение всех маршрутов
        val routes = routeUseCase.getListRoutesAsFlow().first()
        var savedCount = 0
        val allWarnings2 = mutableListOf<String>()
        val allErrors2 = mutableListOf<String>()
        for (route in routes) {
            if (!route.basicData.isSynchronized) {
                val label = routeLabel(route)
                routesManager.saveRouteInRemote(route, bearerToken)
                    .catch { e ->
                        allErrors2.add("$label: ${e.message}")
                        return@catch
                    }
                    .collect { saveResult ->
                        if (saveResult is ResultState.Success) {
                            val data = saveResult.data
                            if (data.warnings.isNotEmpty()) {
                                data.warnings.forEach { w -> allWarnings2.add("$label: $w") }
                            }
                            routeUseCase.setSynchronizedRoute(route.basicData.id).collect {}
                            savedCount++
                        } else if (saveResult is ResultState.Error) {
                            allErrors2.add("$label: ${saveResult.entity.message ?: saveResult.entity.throwable?.message ?: "Ошибка"}")
                        }
                    }
            }
        }
        result.routesSavedCount = savedCount
        result.routeWarnings = allWarnings2
        result.routeErrors = allErrors2
        emit(ResultState.Success(result.copy()))

        if (result.userSettingsSaved && result.salarySettingsSaved && result.releaseDaysSaved && result.routesSavedCount >= 0) {
            val timestamp = Clock.System.now().toEpochMilliseconds()
            sharedPrefs.setLastSyncTimestamp(timestamp)
            emit(ResultState.Success(result.copy(timestamp = timestamp)))
        } else if (allErrors2.isNotEmpty()) {
            emit(ResultState.Error(ErrorEntity(message = allErrors2.joinToString("\n"))))
        } else {
            emit(ResultState.Error(ErrorEntity(message = "Не все данные сохранены успешно")))
        }
    }.flowOn(Dispatchers.Default)

    companion object {
        /**
         * Формирует человекочитаемую метку маршрута: "Маршрут dd.MM.yy" + optional " №123"
         */
        fun routeLabel(route: Route): String {
            val date = route.basicData.timeStartWork?.let {
                val dt = Instant.fromEpochMilliseconds(it)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                "${dt.dayOfMonth.toString().padStart(2, '0')}.${
                    dt.monthNumber.toString().padStart(2, '0')
                }.${(dt.year % 100).toString().padStart(2, '0')}"
            } ?: "?"
            val number = route.basicData.number?.takeIf { it.isNotBlank() }?.let { " №$it" } ?: ""
            return "Маршрут $date$number"
        }
    }
}
