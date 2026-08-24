@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.repository.remote_rest

import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.core.sendToSentry
import com.z_company.domain.repositories.LocomotiveSeriesRepository
import com.z_company.domain.repositories.PartnerRepository
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.repositories.StationNormRepository
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.ProductionCalendarUseCase
import com.z_company.domain.use_cases.ReleaseDayUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.entities.route.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
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

// Data class для результата ДВУСТОРОННЕЙ синхронизации (одна кнопка «Синхронизация»).
// Настройки/отвлечения синхронизируются в обе стороны; для маршрутов ведём отдельные
// счётчики выгруженных/загруженных/удалённых, чтобы показать пользователю сводку.
data class SyncBidirectionalResult(
    var userSettingsSynced: Boolean = false,
    var salarySettingsSynced: Boolean = false,
    var releaseDaysSynced: Boolean = false,
    var routesUploaded: Int = 0,       // отправлено на сервер (новые/изменённые локально)
    var routesDownloaded: Int = 0,     // получено с сервера (новые/изменённые на другом устройстве)
    var routesDeletedRemote: Int = 0,  // удалено на сервере (были удалены локально)
    var routesDeletedLocal: Int = 0,   // удалено локально (были удалены на другом устройстве)
    var routesDone: Boolean = false,   // этап маршрутов завершён (для прогресса в UI)
    var timestamp: Long? = null,
    var routeWarnings: List<String> = emptyList(),
    var routeErrors: List<String> = emptyList(),
    // Маршруты, которые пропали с сервера, но НЕ удалены локально автоматически,
    // т.к. их доля/количество сочли значительным (см. SyncManager.isSignificantRouteDeletion).
    // UI должен спросить подтверждение и вызвать applyPendingRouteDeletions(pendingDeletionRouteIds).
    var pendingDeletionRouteIds: List<String> = emptyList(),
    var pendingDeletionLabels: List<String> = emptyList()
)

/**
 * Менеджер синхронизации данных.
 * Шаг 10 KMP-миграции: KoinComponent → конструкторная инжекция, Android API → KMP API.
 */
class SyncManager(
    private val settingsUseCase: SettingsUseCase,
    private val salarySettingUseCase: SalarySettingUseCase,
    private val calendarUseCase: CalendarUseCase,
    private val productionCalendarUseCase: ProductionCalendarUseCase,
    private val releaseDayUseCase: ReleaseDayUseCase,
    private val routeUseCase: RouteUseCase,
    private val routesManager: RoutesManager,
    private val settingManager: SettingManager,
    private val sharedPrefs: SharedPreferencesRepositories,
    private val locomotiveSeriesRepository: LocomotiveSeriesRepository,
    private val stationNormRepository: StationNormRepository,
    private val partnerRepository: PartnerRepository
) {

    private val syncMutex = Mutex()

    /** LWW-синхронизация отдельного персонального профиля рабочей недели. */
    private suspend fun syncWorkScheduleProfile(bearerToken: String): Boolean {
        val remote = (settingManager.getWorkScheduleProfileFromRemote(bearerToken)
            .first { it is ResultState.Success || it is ResultState.Error } as? ResultState.Success)
            ?.data ?: return false
        val local = sharedPrefs.getWorkScheduleProfile()

        if (local.updatedAt > remote.updatedAt ||
            (local.updatedAt == remote.updatedAt && local != remote && local.mode != com.z_company.domain.entities.WorkScheduleMode.STANDARD)
        ) {
            val saved = (settingManager.saveWorkScheduleProfileInRemote(local, bearerToken)
                .first { it is ResultState.Success || it is ResultState.Error } as? ResultState.Success)
                ?.data ?: return false
            // Сервер может сохранить более свежую версию другого устройства.
            if (saved.updatedAt >= local.updatedAt) sharedPrefs.setWorkScheduleProfile(saved)
        } else if (remote.updatedAt >= local.updatedAt) {
            sharedPrefs.setWorkScheduleProfile(remote)
        }
        return true
    }

    private fun <T> Flow<ResultState<T>>.withSyncDeadline(): Flow<ResultState<T>> = channelFlow {
        try {
            withTimeout(SYNC_OPERATION_TIMEOUT_MILLIS) {
                collect { send(it) }
            }
        } catch (e: TimeoutCancellationException) {
            send(
                ResultState.Error(
                    ErrorEntity(
                        message = NetworkErrorMapper.SYNC_TIMEOUT_MESSAGE,
                        throwable = e,
                    )
                )
            )
        }
    }

    /**
     * Автоматические, ручные и фоновые запуски используют один экземпляр SyncManager.
     * Пока mutex занят, новый автоматический запуск не нужен: текущая операция уже
     * отправит pending-изменения либо оставит флаг для следующей попытки.
     */
    fun isSyncInProgress(): Boolean = syncMutex.isLocked

    /** Не опрашивать сервер при каждом возврате на экран. Pending-правки не подавляются. */
    fun shouldRunAutomaticSync(
        now: Long = Clock.System.now().toEpochMilliseconds(),
        cooldownMillis: Long = AUTOMATIC_SYNC_COOLDOWN_MILLIS
    ): Boolean = sharedPrefs.getSettingsSyncPending() ||
        now - sharedPrefs.getLastSyncTimestamp() >= cooldownMillis

    private suspend fun beginSync() {
        syncMutex.lock()
    }

    private fun endSync() {
        syncMutex.unlock()
    }

    fun syncToRemote(
        bearerToken: String,
        pendingSettingsOnly: Boolean = false,
    ): Flow<ResultState<SyncUploadResult>> = flow {
        beginSync()
        try {
            emit(ResultState.Loading())

        val result = SyncUploadResult()
        val shouldUploadSettings = !pendingSettingsOnly || sharedPrefs.getSettingsSyncPending()
        if (!shouldUploadSettings) {
            result.userSettingsSaved = true
            result.salarySettingsSaved = true
            result.releaseDaysSaved = true
        }

        if (shouldUploadSettings) {
        // 1. Сохранение UserSettings
        val localUserSettingsState = settingsUseCase.getFlowCurrentSettingsState()
            .first { it is ResultState.Success || it is ResultState.Error }
        if (localUserSettingsState is ResultState.Success) {
            val localUserSettings = localUserSettingsState.data
            val localSubscriptionPeriod = localUserSettings.subscriptionPeriod
            // Защита подписки при выгрузке: запрашиваем серверное значение и берём максимум,
            // чтобы не перезаписать более длинную подписку, оформленную на другом устройстве.
            val remoteSubscriptionPeriod = try {
                val remoteState = settingManager.getUserSettingFromRemote(bearerToken)
                    .first { it is ResultState.Success || it is ResultState.Error }
                (remoteState as? ResultState.Success)?.data?.subscriptionPeriod ?: 0L
            } catch (e: Exception) {
                0L
            }
            val mergedSubscriptionPeriod = maxOf(localSubscriptionPeriod, remoteSubscriptionPeriod)
            if (mergedSubscriptionPeriod > Clock.System.now().toEpochMilliseconds()) {
                val settingsToUpload = localUserSettings.copy(subscriptionPeriod = mergedSubscriptionPeriod)
                settingManager.saveUserSettingInRemote(settingsToUpload, bearerToken)
                    .catch { e ->
                        emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения UserSettings: ${NetworkErrorMapper.humanMessage(e)}")))
                        return@catch
                    }
                    .collect { saveState ->
                        if (saveState is ResultState.Success) {
                            result.userSettingsSaved = true
                            emit(ResultState.Success(result.copy()))
                        } else if (saveState is ResultState.Error) {
                            emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения UserSettings: ${saveState.entity.message ?: NetworkErrorMapper.humanMessage(saveState.entity.throwable)}")))
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
                emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения SalarySetting: ${NetworkErrorMapper.humanMessage(e)}")))
                return@catch
            }
            .collect { saveState ->
                if (saveState is ResultState.Success) {
                    result.salarySettingsSaved = true
                    emit(ResultState.Success(result.copy()))
                } else if (saveState is ResultState.Error) {
                    emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения SalarySetting: ${saveState.entity.message ?: NetworkErrorMapper.humanMessage(saveState.entity.throwable)}")))
                    return@collect
                }
            }

        // 2.5. Сохранение тарифных ставок в Calendar (эндпоинт /v1/year/).
        // В Calendar хранятся ТОЛЬКО tariffRate и dateSetTariffRate для каждого месяца.
        // Производственный календарь (days) хранится отдельно в ProductionCalendarDay
        // и в /year/ не попадает — поэтому зануляем days перед отправкой.
        val localMonthOfYearList = calendarUseCase.loadFlowMonthOfYearListState().first()
        if (localMonthOfYearList.isNotEmpty()) {
            val tariffOnlyList = localMonthOfYearList.map { it.copy(days = emptyList()) }
            settingManager.saveMonthOfYearListInRemote(tariffOnlyList, bearerToken)
                .catch { /* Не прерываем синхронизацию */ }
                .collect {}
        }

        // 3. Сохранение дней отвлечений (ReleaseDay)
        val localReleaseDays = releaseDayUseCase.getAll()
        settingManager.saveReleaseDaysInRemote(localReleaseDays, bearerToken)
            .catch { e ->
                emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения дней отвлечений: ${NetworkErrorMapper.humanMessage(e)}")))
                return@catch
            }
            .collect { saveState ->
                if (saveState is ResultState.Success) {
                    result.releaseDaysSaved = true
                    emit(ResultState.Success(result.copy()))
                } else if (saveState is ResultState.Error) {
                    emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения дней отвлечений: ${saveState.entity.message ?: NetworkErrorMapper.humanMessage(saveState.entity.throwable)}")))
                    return@collect
                }
            }

        // 3.1. Индивидуальный график — отдельный LWW-ресурс, не часть UserSettings.
        syncWorkScheduleProfile(bearerToken)

        // 3.5. Синхронизация норм времени (серии локомотивов и станции).
        // Full replace: если есть локальные данные — POST на сервер.
        // Если локальных нет — пропускаем (данные придут в syncFromRemote).
        val localLocoSeries = locomotiveSeriesRepository.getAll()
        if (localLocoSeries.isNotEmpty()) {
            settingManager.saveNormaTimeLocomotivesInRemote(localLocoSeries, bearerToken)
                .catch { /* Не прерываем основную синхронизацию */ }
                .collect {}
        }
        val localStationNorms = stationNormRepository.getAll()
        if (localStationNorms.isNotEmpty()) {
            settingManager.saveNormaTimeStationsInRemote(localStationNorms, bearerToken)
                .catch { /* Не прерываем основную синхронизацию */ }
                .collect {}
        }

        // 3.6. Синхронизация справочника напарников (full replace).
        val localPartners = partnerRepository.getAll()
        if (localPartners.isNotEmpty()) {
            settingManager.savePartnersInRemote(localPartners, bearerToken)
                .catch { /* Не прерываем основную синхронизацию */ }
                .collect {}
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
                allErrors.add("[$routeId] Удаление $label: ${NetworkErrorMapper.humanMessage(e)}")
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
                        allErrors.add("[$routeId] $label: ${NetworkErrorMapper.humanMessage(e)}")
                        return@catch
                    }
                    .collect { saveResult ->
                        if (saveResult is ResultState.Success) {
                            val data = saveResult.data
                            if (data.warnings.isNotEmpty()) {
                                data.warnings.forEach { w -> allWarnings.add("[$routeId] $label: $w") }
                            }
                            routeUseCase.setSynchronizedRoute(routeId).collect {}
                            routeUseCase.setRemoteRouteIdRoute(routeId, routeId).collect {}
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

        if (result.userSettingsSaved && result.salarySettingsSaved && result.releaseDaysSaved &&
            result.routesSavedCount >= 0 && allErrors.isEmpty()
        ) {
            if (shouldUploadSettings) sharedPrefs.setSettingsSyncPending(false)
            val timestamp = Clock.System.now().toEpochMilliseconds()
            sharedPrefs.setLastSyncTimestamp(timestamp)
            emit(ResultState.Success(result.copy(timestamp = timestamp)))
        } else if (allErrors.isNotEmpty()) {
            emit(ResultState.Error(ErrorEntity(message = allErrors.joinToString("\n"))))
        } else {
            emit(ResultState.Error(ErrorEntity(message = "Не все данные сохранены успешно")))
        }
        } finally {
            endSync()
        }
    }.flowOn(Dispatchers.Default).withSyncDeadline()

    /**
     * Синхронизирует один маршрут на сервер.
     * Вызывается после явного нажатия «Сохранить» на FormScreen.
     * Если маршрут уже синхронизирован — возвращает Success без лишних запросов.
     */
    fun syncRoute(routeId: String, bearerToken: String): Flow<ResultState<Unit>> = flow {
        beginSync()
        try {
            emit(ResultState.Loading())
        val route = routeUseCase.getListRoutes().find { it.basicData.id == routeId }
        if (route == null) {
            emit(ResultState.Error(ErrorEntity(message = "Маршрут $routeId не найден")))
            return@flow
        }
        if (route.basicData.isSynchronized) {
            emit(ResultState.Success(Unit))
            return@flow
        }
        routesManager.saveRouteInRemote(route, bearerToken)
            .catch { e ->
                emit(ResultState.Error(ErrorEntity(message = e.message ?: e.cause?.message ?: "Ошибка синхронизации")))
            }
            .collect { saveResult ->
                when (saveResult) {
                    is ResultState.Success -> {
                        routeUseCase.setSynchronizedRoute(routeId).collect {}
                        routeUseCase.setRemoteRouteIdRoute(routeId, routeId).collect {}
                        emit(ResultState.Success(Unit))
                    }
                    is ResultState.Error -> emit(ResultState.Error(saveResult.entity))
                    else -> {}
                }
            }
        } finally {
            endSync()
        }
    }.flowOn(Dispatchers.Default).withSyncDeadline()

    fun syncFromRemote(bearerToken: String): Flow<ResultState<SyncDownloadResult>> = flow {
        beginSync()
        try {
            emit(ResultState.Loading())

        val result = SyncDownloadResult()

        // 1. Загрузка дней отвлечений (ReleaseDay)
        settingManager.getReleaseDaysFromRemote(bearerToken)
            .catch { e ->
                emit(ResultState.Error(ErrorEntity(message = "Ошибка загрузки дней отвлечений: ${NetworkErrorMapper.humanMessage(e)}")))
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
                                        emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения дней отвлечений локально: ${saveResult.entity.message ?: NetworkErrorMapper.humanMessage(saveResult.entity.throwable)}")))
                                        return@collect
                                    }
                                    else -> {}
                                }
                            }
                    }
                    is ResultState.Error -> {
                        emit(ResultState.Error(ErrorEntity(message = "Ошибка загрузки дней отвлечений: ${loadState.entity.message ?: NetworkErrorMapper.humanMessage(loadState.entity.throwable)}")))
                        return@collect
                    }
                    else -> {}
                }
            }

        // 1.5. Загрузка норм времени (только если нет локальных данных).
        // Full replace: если локально пусто — GET с сервера и сохранить.
        // Если локальные данные уже есть — они были загружены туда через syncToRemote.
        val localSeriesForDownload = locomotiveSeriesRepository.getAll()
        if (localSeriesForDownload.isEmpty()) {
            settingManager.getNormaTimeLocomotivesFromRemote(bearerToken)
                .catch { /* Не прерываем основную синхронизацию */ }
                .collect { loadState ->
                    if (loadState is ResultState.Success && loadState.data.isNotEmpty()) {
                        locomotiveSeriesRepository.replaceAll(loadState.data).collect {}
                    }
                }
        }
        val localStationsForDownload = stationNormRepository.getAll()
        if (localStationsForDownload.isEmpty()) {
            settingManager.getNormaTimeStationsFromRemote(bearerToken)
                .catch { /* Не прерываем основную синхронизацию */ }
                .collect { loadState ->
                    if (loadState is ResultState.Success && loadState.data.isNotEmpty()) {
                        stationNormRepository.replaceAll(loadState.data).collect {}
                    }
                }
        }
        // Справочник напарников: если локально пусто — GET с сервера и сохранить.
        val localPartnersForDownload = partnerRepository.getAll()
        if (localPartnersForDownload.isEmpty()) {
            settingManager.getPartnersFromRemote(bearerToken)
                .catch { /* Не прерываем основную синхронизацию */ }
                .collect { loadState ->
                    if (loadState is ResultState.Success && loadState.data.isNotEmpty()) {
                        partnerRepository.replaceAll(loadState.data).collect {}
                    }
                }
        }

        // 2. Загрузка SalarySetting
        settingManager.getSalarySettingFromRemote(bearerToken)
            .catch { e ->
                emit(ResultState.Error(ErrorEntity(message = "Ошибка загрузки SalarySetting: ${NetworkErrorMapper.humanMessage(e)}")))
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
                                        emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения SalarySetting локально: ${saveResult.entity.message ?: NetworkErrorMapper.humanMessage(saveResult.entity.throwable)}")))
                                        return@collect
                                    }
                                    else -> {}
                                }
                            }
                    }
                    is ResultState.Error -> {
                        emit(ResultState.Error(ErrorEntity(message = "Ошибка загрузки SalarySetting: ${loadState.entity.message ?: NetworkErrorMapper.humanMessage(loadState.entity.throwable)}")))
                        return@collect
                    }
                    else -> {}
                }
            }

        // 2.5. Загрузка тарифных ставок из Calendar (эндпоинт /v1/year/) — ПЕРЕД UserSettings!
        // В Calendar хранятся ТОЛЬКО tariffRate и dateSetTariffRate — из серверных данных
        // берём только эти два поля, остальное (days и т.д.) берётся из локальной БД.
        // Порядок важен: UserSettings ищет selectMonthOfYear по году+месяцу в локальной
        // таблице MonthOfYear — загружаем сначала, чтобы selectMonthOfYear нашёл актуальный tariffRate.
        settingManager.getMonthOfYearListFromRemote(bearerToken)
            .catch { /* Не прерываем синхронизацию */ }
            .collect { loadState ->
                if (loadState is ResultState.Success && loadState.data.isNotEmpty()) {
                    val serverMonths = loadState.data
                    val localMonths = calendarUseCase.loadFlowMonthOfYearListState().first()
                    val toSave = if (localMonths.isEmpty()) {
                        serverMonths
                    } else {
                        val serverByYearMonth = serverMonths.associateBy { it.year to it.month }
                        localMonths.map { local ->
                            val server = serverByYearMonth[local.year to local.month]
                            if (server != null) local.copy(
                                tariffRate = server.tariffRate,
                                dateSetTariffRate = server.dateSetTariffRate
                            ) else local
                        }
                    }
                    calendarUseCase.saveCalendar(toSave).collect {}
                }
            }

        // 3. Загрузка UserSettings — ПОСЛЕ MonthOfYear, чтобы selectMonthOfYear был актуальным
        settingManager.getUserSettingFromRemote(bearerToken)
            .catch { e ->
                emit(ResultState.Error(ErrorEntity(message = "Ошибка загрузки UserSettings: ${NetworkErrorMapper.humanMessage(e)}")))
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
                                        emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения UserSettings локально: ${saveResult.entity.message ?: NetworkErrorMapper.humanMessage(saveResult.entity.throwable)}")))
                                        return@collect
                                    }
                                    else -> {}
                                }
                            }
                        // ВАЖНО: перед applyRegionalHolidays перезагружаем production-календарь
                        // с сервера. Это перезаписывает HOLIDAY-теги, которые могли остаться от
                        // ранее выбранного региона. Без этого при смене региона на null или другой
                        // регион устройство-приёмник сохраняет старые HOLIDAY-теги в норме.
                        // Логика повторяет SettingsViewModel.fetchAndApplyCalendar() для согласованности.
                        try {
                            settingManager.getProductionCalendarFromRemote(userSettings.country, now.year)
                                .collect { calState ->
                                    if (calState is ResultState.Success) {
                                        productionCalendarUseCase.saveCalendar(calState.data).collect {}
                                        calendarUseCase.applyProductionCalendar(calState.data).collect {}
                                    }
                                }
                            if (now.monthNumber == 12) {
                                settingManager.getProductionCalendarFromRemote(userSettings.country, now.year + 1)
                                    .collect { calState ->
                                        if (calState is ResultState.Success) {
                                            productionCalendarUseCase.saveCalendar(calState.data).collect {}
                                            calendarUseCase.applyProductionCalendar(calState.data).collect {}
                                        }
                                    }
                            }
                            userSettings.region?.let { region ->
                                calendarUseCase.applyRegionalHolidays(region, now.year).collect {}
                                if (now.monthNumber == 12) {
                                    calendarUseCase.applyRegionalHolidays(region, now.year + 1).collect {}
                                }
                            }
                        } catch (e: Exception) {
                            e.sendToSentry("SyncManager", "syncFromRemote_calendar_refresh")
                        }
                    }
                    is ResultState.Error -> {
                        emit(ResultState.Error(ErrorEntity(message = "Ошибка загрузки UserSettings: ${loadState.entity.message ?: NetworkErrorMapper.humanMessage(loadState.entity.throwable)}")))
                        return@collect
                    }
                    else -> {}
                }
            }

        // 4. Загрузка всех маршрутов
        routesManager.getRoutesFromRemote(bearerToken)
            .catch { e ->
                emit(ResultState.Error(ErrorEntity(message = "Ошибка загрузки маршрутов: ${NetworkErrorMapper.humanMessage(e)}")))
                return@catch
            }
            .collect { loadState ->
                when (loadState) {
                    is ResultState.Success -> {
                        val routes = loadState.data
                        val localRoutesById = routeUseCase.listRouteWithDeleting()
                            .associateBy { it.basicData.id }
                        // Маршруты, помеченные локально как удалённые (isDeleted = true),
                        // не перезаписываем данными с сервера — сервер мог ещё не получить
                        // команду DELETE (сеть, тайм-аут). Следующий syncToRemote
                        // повторит удаление.
                        val localDeletedIds = localRoutesById.values
                            .filter { it.basicData.isDeleted }
                            .map { it.basicData.id }
                            .toSet()
                        var savedCount = 0
                        for (route in routes) {
                            if (route.basicData.id in localDeletedIds) continue
                            val orderedRoute = route.preserveSectionOrderFrom(localRoutesById[route.basicData.id])
                            val r = orderedRoute.copy(
                                basicData = route.basicData.copy(isSynchronized = true)
                            )
                            routeUseCase.saveRouteAfterLoading(r)
                                .collect { saveResult ->
                                    when (saveResult) {
                                        is ResultState.Success -> { savedCount++ }
                                        is ResultState.Error -> {
                                            emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения маршрута локально: ${saveResult.entity.message ?: NetworkErrorMapper.humanMessage(saveResult.entity.throwable)}")))
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
                        emit(ResultState.Error(ErrorEntity(message = "Ошибка загрузки маршрутов: ${loadState.entity.message ?: NetworkErrorMapper.humanMessage(loadState.entity.throwable)}")))
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
        } finally {
            endSync()
        }
    }.flowOn(Dispatchers.Default).withSyncDeadline()

    /**
     * ДВУСТОРОННЯЯ синхронизация — за одной кнопкой «Синхронизация».
     *
     * Модель (как в современных приложениях: почта, заметки):
     *  - **Настройки**: push локальных на сервер только если они менялись локально
     *    (флаг [SharedPreferencesRepositories.getSettingsSyncPending]) — иначе устройство,
     *    которое настройки не трогало, не затирает свежие серверные; затем pull с сервера.
     *    Для UserSettings — LWW по `updateAt` + защита подписки (max). Локальные правки
     *    настроек выгружаются сразу при сохранении (см. [autoPushSettings]).
     *  - **Маршруты**: fetch список с сервера → merge по `id` с LWW по `updatedAt` →
     *    apply. Удаления распространяются в ОБЕ стороны:
     *      • удалённый локально (isDeleted) → DELETE на сервере + жёсткое удаление локально;
     *      • удалённый на другом устройстве (был синхронизирован, пропал с сервера) →
     *        жёсткое удаление локально.
     *
     * Никаких изменений контракта: те же эндпоинты и JSON, только правильная
     * оркестрация на клиенте.
     */
    fun syncBidirectional(bearerToken: String): Flow<ResultState<SyncBidirectionalResult>> = flow {
        beginSync()
        try {
            emit(ResultState.Loading())
        val result = SyncBidirectionalResult()
        val settingsPending = sharedPrefs.getSettingsSyncPending()
        var settingsUploadSucceeded = true

        // ============ ЧАСТЬ 1. НАСТРОЙКИ ============

        // 1.1 UserSettings — LWW по updateAt + защита подписки (max).
        val localUserSettingsState = settingsUseCase.getFlowCurrentSettingsState()
            .first { it is ResultState.Success || it is ResultState.Error }
        val remoteUserSettings = try {
            (settingManager.getUserSettingFromRemote(bearerToken)
                .first { it is ResultState.Success || it is ResultState.Error } as? ResultState.Success)?.data
        } catch (e: Exception) { null }

        if (localUserSettingsState is ResultState.Success) {
            val local = localUserSettingsState.data
            val remoteSub = remoteUserSettings?.subscriptionPeriod ?: 0L
            val mergedSub = maxOf(local.subscriptionPeriod, remoteSub)
            // Локальные свежее ИЛИ на сервере ещё нет настроек → выгружаем локальные.
            val localIsNewer = remoteUserSettings == null || local.updateAt >= remoteUserSettings.updateAt
            if ((settingsPending || localIsNewer) && mergedSub > Clock.System.now().toEpochMilliseconds()) {
                val toUpload = local.copy(subscriptionPeriod = mergedSub)
                var ok = false
                settingManager.saveUserSettingInRemote(toUpload, bearerToken)
                    .catch { e ->
                        settingsUploadSucceeded = false
                        emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения UserSettings: ${NetworkErrorMapper.humanMessage(e)}")))
                    }
                    .collect { s ->
                        if (s is ResultState.Success) ok = true
                        if (s is ResultState.Error) settingsUploadSucceeded = false
                    }
                if (ok) { result.userSettingsSynced = true; emit(ResultState.Success(result.copy())) }
            }
        }
        // Pull UserSettings (сервер мог отдать более свежие — с другого устройства).
        if (remoteUserSettings != null) {
            val localNow = (settingsUseCase.getFlowCurrentSettingsState()
                .first { it is ResultState.Success || it is ResultState.Error } as? ResultState.Success)?.data
            val localUpdateAt = localNow?.updateAt ?: 0L
            // Не откатываем локально более свежие настройки (LWW).
            if (remoteUserSettings.updateAt >= localUpdateAt) {
                val listMonthOfYear = calendarUseCase.loadFlowMonthOfYearListState().first()
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val currentMonthOfYear = listMonthOfYear.find { it.month == now.monthNumber - 1 && it.year == now.year }
                val mergedSub = maxOf(localNow?.subscriptionPeriod ?: 0L, remoteUserSettings.subscriptionPeriod)
                val userSettings = remoteUserSettings.copy(
                    selectMonthOfYear = currentMonthOfYear ?: listMonthOfYear.firstOrNull() ?: com.z_company.domain.entities.MonthOfYear(),
                    subscriptionPeriod = mergedSub
                )
                settingsUseCase.saveSetting(userSettings).collect {}
                try {
                    settingManager.getProductionCalendarFromRemote(userSettings.country, now.year).collect { calState ->
                        if (calState is ResultState.Success) {
                            productionCalendarUseCase.saveCalendar(calState.data).collect {}
                            calendarUseCase.applyProductionCalendar(calState.data).collect {}
                        }
                    }
                    userSettings.region?.let { region -> calendarUseCase.applyRegionalHolidays(region, now.year).collect {} }
                } catch (e: Exception) { e.sendToSentry("SyncManager", "syncBidirectional_calendar") }
            }
            result.userSettingsSynced = true
            emit(ResultState.Success(result.copy()))
        } else if (remoteUserSettings == null && localUserSettingsState is ResultState.Success) {
            // Сервер не отдал настройки (первый вход) — локальные останутся, флаг снимем ниже.
            result.userSettingsSynced = true
            emit(ResultState.Success(result.copy()))
        }

        // 1.1.1 Отдельный профиль рабочей недели — LWW по client updatedAt.
        if (!syncWorkScheduleProfile(bearerToken)) settingsUploadSucceeded = false

        // 1.2 SalarySetting — push (если менялось локально), затем pull (сервер → локально).
        if (settingsPending) {
            val localSalary = salarySettingUseCase.salarySettingFlow().first()
            settingManager.saveSalarySettingInRemote(localSalary, bearerToken)
                .catch { settingsUploadSucceeded = false }
                .collect { if (it is ResultState.Error) settingsUploadSucceeded = false }
        }
        settingManager.getSalarySettingFromRemote(bearerToken)
            .catch { e -> emit(ResultState.Error(ErrorEntity(message = "Ошибка загрузки SalarySetting: ${NetworkErrorMapper.humanMessage(e)}"))) }
            .collect { loadState ->
                if (loadState is ResultState.Success) {
                    salarySettingUseCase.saveSalarySetting(loadState.data).collect {}
                    result.salarySettingsSynced = true
                    emit(ResultState.Success(result.copy()))
                }
            }

        // 1.3 Тарифные ставки (monthOfYear / /year/) — push если менялось, pull-merge.
        val localMonths = calendarUseCase.loadFlowMonthOfYearListState().first()
        if (settingsPending && localMonths.isNotEmpty()) {
            settingManager.saveMonthOfYearListInRemote(localMonths.map { it.copy(days = emptyList()) }, bearerToken)
                .catch { settingsUploadSucceeded = false }
                .collect { if (it is ResultState.Error) settingsUploadSucceeded = false }
        }
        settingManager.getMonthOfYearListFromRemote(bearerToken)
            .catch { }
            .collect { loadState ->
                if (loadState is ResultState.Success && loadState.data.isNotEmpty()) {
                    val serverMonths = loadState.data
                    val local = calendarUseCase.loadFlowMonthOfYearListState().first()
                    val toSave = if (local.isEmpty()) serverMonths else {
                        val byKey = serverMonths.associateBy { it.year to it.month }
                        local.map { l ->
                            val s = byKey[l.year to l.month]
                            if (s != null) l.copy(tariffRate = s.tariffRate, dateSetTariffRate = s.dateSetTariffRate) else l
                        }
                    }
                    calendarUseCase.saveCalendar(toSave).collect {}
                }
            }

        // 1.4 Дни отвлечений (ReleaseDay) — push если менялось, затем pull (full-replace).
        if (settingsPending) {
            val localReleaseDays = releaseDayUseCase.getAll()
            settingManager.saveReleaseDaysInRemote(localReleaseDays, bearerToken)
                .catch { settingsUploadSucceeded = false }
                .collect { if (it is ResultState.Error) settingsUploadSucceeded = false }
        }
        settingManager.getReleaseDaysFromRemote(bearerToken)
            .catch { e -> emit(ResultState.Error(ErrorEntity(message = "Ошибка загрузки дней отвлечений: ${NetworkErrorMapper.humanMessage(e)}"))) }
            .collect { loadState ->
                if (loadState is ResultState.Success) {
                    releaseDayUseCase.replaceAllFromRemote(loadState.data).collect {}
                    result.releaseDaysSynced = true
                    emit(ResultState.Success(result.copy()))
                }
            }

        // 1.5 Нормы времени и напарники (full-replace; merge не поддерживается контрактом).
        // Напарники: при локальном изменении сервер получает полный список (включая
        // пустой — это удаление всех записей). На остальных устройствах серверный
        // список всегда заменяет локальный, даже если локальный справочник не пуст.
        val localLocoSeries = locomotiveSeriesRepository.getAll()
        if (settingsPending && localLocoSeries.isNotEmpty()) {
            settingManager.saveNormaTimeLocomotivesInRemote(localLocoSeries, bearerToken)
                .catch { settingsUploadSucceeded = false }
                .collect { if (it is ResultState.Error) settingsUploadSucceeded = false }
        } else if (localLocoSeries.isEmpty()) {
            settingManager.getNormaTimeLocomotivesFromRemote(bearerToken).catch { }.collect { s ->
                if (s is ResultState.Success && s.data.isNotEmpty()) locomotiveSeriesRepository.replaceAll(s.data).collect {}
            }
        }
        val localStationNorms = stationNormRepository.getAll()
        if (settingsPending && localStationNorms.isNotEmpty()) {
            settingManager.saveNormaTimeStationsInRemote(localStationNorms, bearerToken)
                .catch { settingsUploadSucceeded = false }
                .collect { if (it is ResultState.Error) settingsUploadSucceeded = false }
        } else if (localStationNorms.isEmpty()) {
            settingManager.getNormaTimeStationsFromRemote(bearerToken).catch { }.collect { s ->
                if (s is ResultState.Success && s.data.isNotEmpty()) stationNormRepository.replaceAll(s.data).collect {}
            }
        }
        val localPartners = partnerRepository.getAll()
        if (settingsPending) {
            settingManager.savePartnersInRemote(localPartners, bearerToken)
                .catch { settingsUploadSucceeded = false }
                .collect { if (it is ResultState.Error) settingsUploadSucceeded = false }
        } else {
            settingManager.getPartnersFromRemote(bearerToken).catch { }.collect { s ->
                if (s is ResultState.Success) partnerRepository.replaceAll(s.data).collect {}
            }
        }

        // Настройки выгружены — снимаем флаг «есть несинхронизированные настройки».
        if (settingsPending && settingsUploadSucceeded) {
            sharedPrefs.setSettingsSyncPending(false)
        }

        // ============ ЧАСТЬ 2. МАРШРУТЫ (fetch → merge → apply) ============
        val allErrors = mutableListOf<String>()
        val allWarnings = mutableListOf<String>()

        // 2.1 Тянем актуальный список маршрутов с сервера.
        val serverRoutes: List<Route> = try {
            val state = routesManager.getRoutesFromRemote(bearerToken)
                .first { it is ResultState.Success || it is ResultState.Error }
            when (state) {
                is ResultState.Success -> state.data
                is ResultState.Error -> {
                    emit(ResultState.Error(ErrorEntity(message = "Ошибка загрузки маршрутов: ${state.entity.message ?: NetworkErrorMapper.humanMessage(state.entity.throwable)}")))
                    return@flow
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            emit(ResultState.Error(ErrorEntity(message = "Ошибка загрузки маршрутов: ${NetworkErrorMapper.humanMessage(e)}")))
            return@flow
        }
        val serverById = serverRoutes.associateBy { it.basicData.id }

        val localAll = routeUseCase.listRouteWithDeleting()
        val localById = localAll.associateBy { it.basicData.id }

        // 2.2 Удаления, сделанные локально → удалить на сервере, затем жёстко локально.
        // (Раньше блок был мёртв из-за guard'а remoteRouteId, который нигде не заполнялся,
        //  поэтому удаления не доходили до сервера.)
        for (route in localAll.filter { it.basicData.isDeleted }) {
            val routeId = route.basicData.id
            val label = routeLabel(route)
            if (routeId !in serverById) {
                // Сервер уже не знает о маршруте — просто убираем локально.
                routeUseCase.removeRoute(route).collect {}
                result.routesDeletedLocal++
                continue
            }
            try {
                var handled = false
                routesManager.deleteRouteInRemote(routeId, bearerToken).collect { del ->
                    when (del) {
                        is ResultState.Success -> {
                            routeUseCase.removeRoute(route).collect {}
                            result.routesDeletedRemote++
                            handled = true
                        }
                        is ResultState.Error -> {
                            val msg = del.entity.message ?: del.entity.throwable?.message ?: "Ошибка"
                            // 404 — на сервере уже нет: считаем удаление успешным.
                            if (msg.contains("404") || msg.contains("not found", ignoreCase = true)) {
                                routeUseCase.removeRoute(route).collect {}
                                result.routesDeletedRemote++
                            } else {
                                allErrors.add("[$routeId] Удаление $label: $msg")
                            }
                            handled = true
                        }
                        else -> {}
                    }
                }
                if (!handled) { /* Loading only */ }
            } catch (e: Exception) {
                allErrors.add("[$routeId] Удаление $label: ${NetworkErrorMapper.humanMessage(e)}")
                e.sendToSentry("SyncManager", "syncBidirectional_delete")
            }
        }

        val locallyDeletedIds = localAll.filter { it.basicData.isDeleted }.map { it.basicData.id }.toSet()

        // 2.3 Серверные маршруты → merge в локальную БД (LWW по updatedAt).
        for (server in serverRoutes) {
            val id = server.basicData.id
            if (id in locallyDeletedIds) continue // локальное удаление в приоритете
            val local = localById[id]
            // Миграция старых установок: раньше remoteRouteId не заполнялся.
            // Сам факт наличия id на сервере надёжно доказывает, что маршрут уже
            // был облачным; сохраняем локальный маркер до дальнейших правок.
            if (local != null && local.basicData.remoteRouteId.isNullOrBlank()) {
                routeUseCase.setRemoteRouteIdRoute(id, id).collect {}
            }
            when {
                local == null -> {
                    // Новый маршрут с другого устройства.
                    saveDownloadedRoute(server, local); result.routesDownloaded++
                }
                local.basicData.isSynchronized -> {
                    // Локально не менялся: берём серверный, если он свежее.
                    if (server.basicData.updatedAt > local.basicData.updatedAt) {
                        saveDownloadedRoute(server, local); result.routesDownloaded++
                    }
                }
                else -> {
                    // Локально есть несохранённые правки → LWW.
                    if (local.basicData.updatedAt >= server.basicData.updatedAt) {
                        pushRoute(local, bearerToken, allWarnings, allErrors)?.let { if (it) result.routesUploaded++ }
                    } else {
                        saveDownloadedRoute(server); result.routesDownloaded++
                    }
                }
            }
        }

        // 2.4 Локальные маршруты, которых нет на сервере.
        // Кандидаты на удаление сначала собираем, не удаляя сразу: если их доля
        // окажется подозрительно большой (см. isSignificantRouteDeletion), это может
        // быть не «удалили на другом устройстве», а пустой/усечённый ответ сервера
        // из-за бага или сбоя — тогда лучше спросить пользователя, чем стереть историю.
        val deletionCandidates = mutableListOf<Route>()
        for (local in localAll) {
            if (local.basicData.isDeleted) continue
            val id = local.basicData.id
            if (id in serverById) continue
            val wasEverUploaded = local.basicData.isSynchronized ||
                !local.basicData.remoteRouteId.isNullOrBlank()
            if (wasEverUploaded) {
                // Уже существовал в облаке и пропал с сервера → похоже на удаление
                // на другом устройстве. Финальное решение — ниже, после оценки объёма.
                deletionCandidates.add(local)
            } else {
                // Новый/правленый локально, ещё не выгружен → push.
                pushRoute(local, bearerToken, allWarnings, allErrors)?.let { if (it) result.routesUploaded++ }
            }
        }

        val totalSyncedLocal = localAll.count {
            !it.basicData.isDeleted &&
                (it.basicData.isSynchronized || !it.basicData.remoteRouteId.isNullOrBlank())
        }
        if (deletionCandidates.isNotEmpty() &&
            isSignificantRouteDeletion(deletionCandidates.size, totalSyncedLocal)
        ) {
            // Удаление побеждает даже более позднюю локальную правку в обычном случае,
            // но не когда объём подозрительно большой — тут решение за пользователем.
            result.pendingDeletionRouteIds = deletionCandidates.map { it.basicData.id }
            result.pendingDeletionLabels = deletionCandidates.map { routeLabel(it) }
        } else {
            for (local in deletionCandidates) {
                // Маршрут не должен самопроизвольно воскресать — удаление побеждает.
                routeUseCase.removeRoute(local).collect {}
                result.routesDeletedLocal++
            }
        }

        result.routesDone = true
        result.routeWarnings = allWarnings
        result.routeErrors = allErrors
        emit(ResultState.Success(result.copy()))

        // ============ ФИНАЛ ============
        if (allErrors.isEmpty()) {
            val timestamp = Clock.System.now().toEpochMilliseconds()
            sharedPrefs.setLastSyncTimestamp(timestamp)
            emit(ResultState.Success(result.copy(timestamp = timestamp)))
        } else {
            emit(ResultState.Error(ErrorEntity(message = allErrors.joinToString("\n"))))
        }
        } finally {
            endSync()
        }
    }.flowOn(Dispatchers.Default).withSyncDeadline()

    /**
     * «Значительное» удаление — либо абсолютно большое количество маршрутов, либо
     * заметная доля от всех ранее синхронизированных локальных маршрутов. Порог
     * специально не реагирует на удаление 1 маршрута с другого устройства — это
     * нормальный частый сценарий, спрашивать подтверждение на него было бы шумно.
     */
    private fun isSignificantRouteDeletion(deletionCount: Int, totalSyncedLocal: Int): Boolean {
        if (deletionCount >= SIGNIFICANT_DELETION_MIN_COUNT) return true
        if (deletionCount < 2) return false
        val ratio = deletionCount.toDouble() / totalSyncedLocal.coerceAtLeast(1)
        return ratio >= SIGNIFICANT_DELETION_MIN_RATIO
    }

    /**
     * Применяет удаление маршрутов, отложенное в syncBidirectional из-за того, что оно
     * было признано значительным (см. [SyncBidirectionalResult.pendingDeletionRouteIds]).
     * Вызывается ТОЛЬКО после явного подтверждения пользователем в UI. Ничего не шлёт
     * на сервер — маршруты там уже отсутствуют, только чистит локальную БД.
     */
    fun applyPendingRouteDeletions(routeIds: List<String>): Flow<ResultState<Int>> = flow {
        emit(ResultState.Loading())
        val localById = routeUseCase.listRouteWithDeleting().associateBy { it.basicData.id }
        var deletedCount = 0
        for (id in routeIds) {
            val local = localById[id] ?: continue
            routeUseCase.removeRoute(local).collect {}
            deletedCount++
        }
        emit(ResultState.Success(deletedCount))
    }.flowOn(Dispatchers.Default)

    /** Сохранить маршрут, пришедший с сервера (пометив синхронизированным). */
    private suspend fun saveDownloadedRoute(server: Route, local: Route? = null) {
        val orderedServer = server.preserveSectionOrderFrom(local)
        val r = orderedServer.copy(
            basicData = server.basicData.copy(
                isSynchronized = true,
                // Локальный маркер того, что маршрут уже существовал в облаке.
                // Нужен, чтобы отличить его от нового локального маршрута, если
                // серверный список больше не содержит этот id.
                remoteRouteId = server.basicData.remoteRouteId ?: server.basicData.id
            )
        )
        routeUseCase.saveRouteAfterLoading(r).collect {}
    }

    /**
     * Выгрузить один локальный маршрут на сервер и пометить синхронизированным.
     * @return true — успех, false — ошибка (добавлена в [errors]), null — не выполнялось.
     */
    private suspend fun pushRoute(
        route: Route,
        bearerToken: String,
        warnings: MutableList<String>,
        errors: MutableList<String>
    ): Boolean? {
        val routeId = route.basicData.id
        val label = routeLabel(route)
        var ok: Boolean? = null
        routesManager.saveRouteInRemote(route, bearerToken)
            .catch { e -> errors.add("[$routeId] $label: ${NetworkErrorMapper.humanMessage(e)}"); ok = false }
            .collect { saveResult ->
                when (saveResult) {
                    is ResultState.Success -> {
                        saveResult.data.warnings.forEach { w -> warnings.add("[$routeId] $label: $w") }
                        routeUseCase.setSynchronizedRoute(routeId).collect {}
                        routeUseCase.setRemoteRouteIdRoute(routeId, routeId).collect {}
                        ok = true
                    }
                    is ResultState.Error -> {
                        errors.add("[$routeId] $label: ${saveResult.entity.message ?: saveResult.entity.throwable?.message ?: "Ошибка"}")
                        ok = false
                    }
                    else -> {}
                }
            }
        return ok
    }

    /**
     * Выгрузить настройки на сервер сразу после локального сохранения
     * (маршруты уже так делают через syncRoute). Fire-and-forget: тихо взводит флаг
     * [SharedPreferencesRepositories.setSettingsSyncPending] и снимает его при успехе,
     * чтобы двусторонняя синхронизация знала, нужно ли пушить настройки.
     * Гейт по подписке — как у остальной синхронизации.
     */
    fun autoPushSettings(bearerToken: String): Flow<ResultState<Unit>> = flow {
        beginSync()
        try {
            emit(ResultState.Loading())
        sharedPrefs.setSettingsSyncPending(true)

        val localUserSettingsState = settingsUseCase.getFlowCurrentSettingsState()
            .first { it is ResultState.Success || it is ResultState.Error }
        if (localUserSettingsState !is ResultState.Success) { emit(ResultState.Success(Unit)); return@flow }
        val local = localUserSettingsState.data
        if (local.subscriptionPeriod <= Clock.System.now().toEpochMilliseconds()) {
            // Без подписки синхронизация недоступна — просто выходим (флаг останется взведён).
            emit(ResultState.Success(Unit)); return@flow
        }

        var allOk = true
        val remoteSub = try {
            (settingManager.getUserSettingFromRemote(bearerToken)
                .first { it is ResultState.Success || it is ResultState.Error } as? ResultState.Success)?.data?.subscriptionPeriod ?: 0L
        } catch (e: Exception) { 0L }
        val mergedSub = maxOf(local.subscriptionPeriod, remoteSub)
        settingManager.saveUserSettingInRemote(local.copy(subscriptionPeriod = mergedSub), bearerToken)
            .catch { allOk = false }.collect { if (it is ResultState.Error) allOk = false }

        val localSalary = salarySettingUseCase.salarySettingFlow().first()
        settingManager.saveSalarySettingInRemote(localSalary, bearerToken)
            .catch { allOk = false }.collect { if (it is ResultState.Error) allOk = false }

        val localMonths = calendarUseCase.loadFlowMonthOfYearListState().first()
        if (localMonths.isNotEmpty()) {
            settingManager.saveMonthOfYearListInRemote(localMonths.map { it.copy(days = emptyList()) }, bearerToken)
                .catch { allOk = false }.collect {}
        }
        val localReleaseDays = releaseDayUseCase.getAll()
        settingManager.saveReleaseDaysInRemote(localReleaseDays, bearerToken)
            .catch { allOk = false }.collect { if (it is ResultState.Error) allOk = false }

        val localWorkSchedule = sharedPrefs.getWorkScheduleProfile()
        settingManager.saveWorkScheduleProfileInRemote(localWorkSchedule, bearerToken)
            .catch { allOk = false }
            .collect { state ->
                if (state is ResultState.Success && state.data.updatedAt >= localWorkSchedule.updatedAt) {
                    sharedPrefs.setWorkScheduleProfile(state.data)
                }
                if (state is ResultState.Error) allOk = false
            }

        val localLocoSeries = locomotiveSeriesRepository.getAll()
        if (localLocoSeries.isNotEmpty()) {
            settingManager.saveNormaTimeLocomotivesInRemote(localLocoSeries, bearerToken)
                .catch { allOk = false }
                .collect { if (it is ResultState.Error) allOk = false }
        }
        val localStationNorms = stationNormRepository.getAll()
        if (localStationNorms.isNotEmpty()) {
            settingManager.saveNormaTimeStationsInRemote(localStationNorms, bearerToken)
                .catch { allOk = false }
                .collect { if (it is ResultState.Error) allOk = false }
        }
        val localPartners = partnerRepository.getAll()
        if (localPartners.isNotEmpty()) {
            settingManager.savePartnersInRemote(localPartners, bearerToken)
                .catch { allOk = false }
                .collect { if (it is ResultState.Error) allOk = false }
        }

        if (allOk) {
            sharedPrefs.setSettingsSyncPending(false)
            sharedPrefs.setLastSyncTimestamp(Clock.System.now().toEpochMilliseconds())
        }
        emit(ResultState.Success(Unit))
        } finally {
            endSync()
        }
    }.flowOn(Dispatchers.Default).withSyncDeadline()

    fun firstSyncAfterRegistration(bearerToken: String): Flow<ResultState<SyncUploadResult>> = flow {
        beginSync()
        try {
            emit(ResultState.Loading())

        val result = SyncUploadResult()

        // 1. Сохранение UserSettings
        val localUserSettingsState = settingsUseCase.getFlowCurrentSettingsState()
            .first { it is ResultState.Success || it is ResultState.Error }
        if (localUserSettingsState is ResultState.Success) {
            val localUserSettings = localUserSettingsState.data
            val endTimeSubscription = sharedPrefs.getSubscriptionExpiration()
            // Защита подписки при первой выгрузке: берём максимум из SharedPrefs и сервера,
            // чтобы не перезаписать более длинную подписку, ранее сохранённую на сервере.
            val remoteSubscriptionPeriodFirst = try {
                val remoteState = settingManager.getUserSettingFromRemote(bearerToken)
                    .first { it is ResultState.Success || it is ResultState.Error }
                (remoteState as? ResultState.Success)?.data?.subscriptionPeriod ?: 0L
            } catch (e: Exception) {
                0L
            }
            val mergedSubscriptionPeriodFirst = maxOf(endTimeSubscription, remoteSubscriptionPeriodFirst)
            val l = localUserSettings.copy(subscriptionPeriod = mergedSubscriptionPeriodFirst)
            settingManager.saveUserSettingInRemote(l, bearerToken)
                .catch { e ->
                    emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения UserSettings: ${NetworkErrorMapper.humanMessage(e)}")))
                    return@catch
                }
                .collect { saveState ->
                    if (saveState is ResultState.Success) {
                        result.userSettingsSaved = true
                        emit(ResultState.Success(result.copy()))
                    } else if (saveState is ResultState.Error) {
                        emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения UserSettings: ${saveState.entity.message ?: NetworkErrorMapper.humanMessage(saveState.entity.throwable)}")))
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
                emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения SalarySetting: ${NetworkErrorMapper.humanMessage(e)}")))
                return@catch
            }
            .collect { saveState ->
                if (saveState is ResultState.Success) {
                    result.salarySettingsSaved = true
                    emit(ResultState.Success(result.copy()))
                } else if (saveState is ResultState.Error) {
                    emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения SalarySetting: ${saveState.entity.message ?: NetworkErrorMapper.humanMessage(saveState.entity.throwable)}")))
                    return@collect
                }
            }

        // 2.5. Сохранение тарифных ставок в Calendar (эндпоинт /v1/year/).
        // В Calendar хранятся ТОЛЬКО tariffRate и dateSetTariffRate для каждого месяца.
        // Производственный календарь (days) хранится отдельно в ProductionCalendarDay
        // и в /year/ не попадает — поэтому зануляем days перед отправкой.
        val localMonthOfYearListFirst = calendarUseCase.loadFlowMonthOfYearListState().first()
        if (localMonthOfYearListFirst.isNotEmpty()) {
            val tariffOnlyListFirst = localMonthOfYearListFirst.map { it.copy(days = emptyList()) }
            settingManager.saveMonthOfYearListInRemote(tariffOnlyListFirst, bearerToken)
                .catch { /* Не прерываем синхронизацию */ }
                .collect {}
        }

        // 3. Сохранение дней отвлечений (ReleaseDay)
        val localReleaseDaysFirst = releaseDayUseCase.getAll()
        settingManager.saveReleaseDaysInRemote(localReleaseDaysFirst, bearerToken)
            .catch { e ->
                emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения дней отвлечений: ${NetworkErrorMapper.humanMessage(e)}")))
                return@catch
            }
            .collect { saveState ->
                if (saveState is ResultState.Success) {
                    result.releaseDaysSaved = true
                    emit(ResultState.Success(result.copy()))
                } else if (saveState is ResultState.Error) {
                    emit(ResultState.Error(ErrorEntity(message = "Ошибка сохранения дней отвлечений: ${saveState.entity.message ?: NetworkErrorMapper.humanMessage(saveState.entity.throwable)}")))
                    return@collect
                }
            }

        syncWorkScheduleProfile(bearerToken)

        // 3.5. Синхронизация норм времени (серии локомотивов и станции).
        val localLocoSeriesFirst = locomotiveSeriesRepository.getAll()
        if (localLocoSeriesFirst.isNotEmpty()) {
            settingManager.saveNormaTimeLocomotivesInRemote(localLocoSeriesFirst, bearerToken)
                .catch { /* Не прерываем основную синхронизацию */ }
                .collect {}
        }
        val localStationNormsFirst = stationNormRepository.getAll()
        if (localStationNormsFirst.isNotEmpty()) {
            settingManager.saveNormaTimeStationsInRemote(localStationNormsFirst, bearerToken)
                .catch { /* Не прерываем основную синхронизацию */ }
                .collect {}
        }
        val localPartnersFirst = partnerRepository.getAll()
        if (localPartnersFirst.isNotEmpty()) {
            settingManager.savePartnersInRemote(localPartnersFirst, bearerToken)
                .catch { /* Не прерываем основную синхронизацию */ }
                .collect {}
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
                            routeUseCase.setRemoteRouteIdRoute(
                                route.basicData.id,
                                route.basicData.id
                            ).collect {}
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
        } finally {
            endSync()
        }
    }.flowOn(Dispatchers.Default).withSyncDeadline()

    companion object {
        const val AUTOMATIC_SYNC_COOLDOWN_MILLIS: Long = 5 * 60 * 1000L
        const val SYNC_OPERATION_TIMEOUT_MILLIS: Long = 25_000L

        // Пороги "значительного" удаления маршрутов при синхронизации — см. isSignificantRouteDeletion.
        private const val SIGNIFICANT_DELETION_MIN_COUNT = 3
        private const val SIGNIFICANT_DELETION_MIN_RATIO = 0.5

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
