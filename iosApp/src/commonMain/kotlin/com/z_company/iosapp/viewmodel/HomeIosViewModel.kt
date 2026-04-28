@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.z_company.core.AppError
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.UtilForMonthOfYear.getNormaHoursInDate
import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHours
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.findCurrentRoute
import com.z_company.domain.entities.route.UtilsForEntities.findNextFutureRoute
import com.z_company.domain.entities.route.UtilsForEntities.getNightTime
import com.z_company.domain.entities.route.UtilsForEntities.getOnePersonOperationTime
import com.z_company.domain.entities.route.UtilsForEntities.getOnePersonOperationTimePassengerTrain
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getSingleLocomotiveTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.core.ResultState
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.util.TimeCalculationContext
import com.z_company.domain.util.generateId
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.RoutesManager
import com.z_company.repository.remote_rest.ShareRouteManager
import com.z_company.repository.remote_rest.SyncManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * KMP ViewModel для главного экрана.
 *
 * Поддерживает:
 *  - список маршрутов за выбранный месяц
 *  - переключение месяца (setCurrentMonth)
 *  - удаление маршрута (deleteRoute)
 *  - копирование маршрута (copyRoute)
 */
class HomeIosViewModel(
    private val routeUseCase: RouteUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val routesManager: RoutesManager,
    private val shareRouteManager: ShareRouteManager,
    private val secureTokenStorage: SecureTokenStorage,
    private val syncManager: SyncManager,
) : ViewModel() {

    // События UI (snackbar, share-sheet): используем SharedFlow,
    // чтобы Swift-обёртка могла коллектить без потери.
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _shareLinks = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val shareLinks: SharedFlow<String> = _shareLinks.asSharedFlow()

    private val _isSyncingRoute = MutableStateFlow(false)
    val isSyncingRoute: StateFlow<Boolean> = _isSyncingRoute.asStateFlow()

    private val _isCreatingShareLink = MutableStateFlow(false)
    val isCreatingShareLink: StateFlow<Boolean> = _isCreatingShareLink.asStateFlow()

    // Pull-to-refresh: отдельный флаг от _isSyncingRoute (тот для одиночного
    // маршрута). _isRefreshing активен на время syncFromRemote() в refresh().
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Типизированная ошибка для Шага 5 SwiftUI .alert + retry.
    // Публикуется только из explicit-действий (refresh / syncRoute / shareRoute).
    // Passive collect routesFlow не публикует (нет ResultState — Flow<List<Route>>).
    // deleteRoute Error — silent recovery by design (см. ниже).
    private val _error = MutableStateFlow<AppError?>(null)
    val error: StateFlow<AppError?> = _error.asStateFlow()

    private val _routes = MutableStateFlow<List<Route>>(emptyList())
    val routes: StateFlow<List<Route>> = _routes.asStateFlow()

    private val _settings = MutableStateFlow<UserSettings?>(null)
    val settings: StateFlow<UserSettings?> = _settings.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    private val _currentMonth = MutableStateFlow(now.monthNumber - 1) // 0-based, matches MonthOfYear
    val currentMonth: StateFlow<Int> = _currentMonth.asStateFlow()

    private val _currentYear = MutableStateFlow(now.year)
    val currentYear: StateFlow<Int> = _currentYear.asStateFlow()

    // Статистика
    private val _totalWorkMs = MutableStateFlow(0L)
    val totalWorkMs: StateFlow<Long> = _totalWorkMs.asStateFlow()

    private val _nightWorkMs = MutableStateFlow(0L)
    val nightWorkMs: StateFlow<Long> = _nightWorkMs.asStateFlow()

    private val _passengerWorkMs = MutableStateFlow(0L)
    val passengerWorkMs: StateFlow<Long> = _passengerWorkMs.asStateFlow()

    private val _reserveWorkMs = MutableStateFlow(0L)
    val reserveWorkMs: StateFlow<Long> = _reserveWorkMs.asStateFlow()

    private val _onePersonMs = MutableStateFlow(0L)
    val onePersonMs: StateFlow<Long> = _onePersonMs.asStateFlow()

    private val _normaHoursMonth = MutableStateFlow(165)
    val normaHoursMonth: StateFlow<Int> = _normaHoursMonth.asStateFlow()

    private val _normaHoursToday = MutableStateFlow(0)
    val normaHoursToday: StateFlow<Int> = _normaHoursToday.asStateFlow()

    private val _todayWorkMs = MutableStateFlow(0L)
    val todayWorkMs: StateFlow<Long> = _todayWorkMs.asStateFlow()

    private var routesJob: Job? = null

    init {
        viewModelScope.launch {
            settingsUseCase.getUserSettingFlow().collect { userSettings ->
                _settings.value = userSettings
                val moy = userSettings.selectMonthOfYear
                _currentMonth.value = moy.month
                _currentYear.value = moy.year
                _normaHoursMonth.value = moy.getPersonalNormaHours()
                val nowMs = Clock.System.now().toEpochMilliseconds()
                _normaHoursToday.value = moy.getNormaHoursInDate(nowMs)
                loadRoutesForMonth(moy, userSettings.timeZone)
            }
        }
    }

    private fun loadRoutesForMonth(monthOfYear: MonthOfYear, offsetInMoscow: Long) {
        routesJob?.cancel()
        routesJob = viewModelScope.launch {
            _isLoading.value = true
            routeUseCase.routeListByMonthFlow(
                monthOfYear = monthOfYear,
                offsetInMoscow = offsetInMoscow,
            ).collect { routes ->
                _routes.value = routes
                _isLoading.value = false
                recalculateStats(routes, monthOfYear, offsetInMoscow)
            }
        }
    }

    private fun recalculateStats(routes: List<Route>, monthOfYear: MonthOfYear, offsetInMoscow: Long) {
        viewModelScope.launch {
            val userSettings = _settings.value ?: return@launch
            val context = TimeCalculationContext.from(userSettings)

            // Общее рабочее время
            _totalWorkMs.value = routes.getWorkTime(monthOfYear, context)

            // Ночное время
            _nightWorkMs.value = routes.getNightTime(userSettings)

            // Пассажиром
            _passengerWorkMs.value = routes.getPassengerTime(monthOfYear, context)

            // Резервом (одиночное следование)
            _reserveWorkMs.value = routes.getSingleLocomotiveTime()

            // Одно лицо
            val opFreight = routes.getOnePersonOperationTime(monthOfYear, context)
            val opPassenger = routes.getOnePersonOperationTimePassengerTrain(monthOfYear, context)
            _onePersonMs.value = opFreight + opPassenger

            // Отработано сегодня
            val nowMs = Clock.System.now().toEpochMilliseconds()
            val completedRoutes = routes.filter { route ->
                val end = route.basicData.timeEndWork ?: return@filter false
                end <= nowMs
            }
            _todayWorkMs.value = completedRoutes.getWorkTime(monthOfYear, context)
        }
    }

    /** Переключает отображаемый месяц и сохраняет выбор в настройках. */
    fun setCurrentMonth(month: Int, year: Int) {
        val newMoy = MonthOfYear(month = month, year = year)
        _currentMonth.value = month
        _currentYear.value = year
        viewModelScope.launch {
            settingsUseCase.setCurrentMonthOfYear(newMoy).first()
        }
        val offset = _settings.value?.timeZone ?: 0L
        loadRoutesForMonth(newMoy, offset)
    }

    /**
     * Удаляет маршрут.
     *
     * 1) Soft-delete в локальной БД (isDeleted=true) — UI обновляется мгновенно,
     *    т.к. `getAll` фильтрует `isDeleted=0`, и мы сразу убираем запись из
     *    `_routes`.
     * 2) Если есть токен — пробуем тут же DELETE /v1/route/{id} на сервере.
     *    - Успех → физически удаляем запись из локальной БД (`removeRoute`).
     *    - Ошибка (нет сети, 5xx и т.п.) → запись остаётся помеченной
     *      isDeleted=true. Следующая синхронизация (`SyncManager.syncToRemote`
     *      шаг 4) повторит DELETE — идемпотентно.
     * 3) Если токена нет (оффлайн-пользователь) — оставляем помеченной,
     *    удаление произойдёт при первой синхронизации после входа.
     */
    fun deleteRoute(routeId: String) {
        viewModelScope.launch {
            val route = _routes.value.firstOrNull { it.basicData.id == routeId } ?: return@launch

            // 1. Мгновенный soft-delete: UI обновляется, запись исчезает из getAll.
            routeUseCase.markAsRemoved(route).first()
            _routes.value = _routes.value.filter { it.basicData.id != routeId }

            // 2. Попытка удалить на сервере прямо сейчас.
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            if (token.isNullOrBlank()) return@launch

            routesManager.deleteRouteInRemote(routeId, "Bearer $token").collect { state ->
                when (state) {
                    is ResultState.Success -> {
                        // Сервер подтвердил удаление — физически сносим локально.
                        routeUseCase.removeRoute(route).first()
                    }
                    is ResultState.Error -> {
                        // silent recovery by design: оставляем isDeleted=true.
                        // SyncManager.syncToRemote шаг 4 повторит DELETE при
                        // следующей синхронизации. Алерт пользователю не
                        // показываем — UI уже скрыл маршрут soft-delete'ом.
                        Logger.withTag("Home").i {
                            "Soft-delete pending — server delete failed, will retry on next sync: ${state.entity.message}"
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    /** Создаёт копию маршрута с новым ID и сохраняет её. */
    fun copyRoute(routeId: String) {
        viewModelScope.launch {
            val original = _routes.value.firstOrNull { it.basicData.id == routeId } ?: return@launch
            val newBasicId = generateId()
            val copiedRoute = original.copy(
                basicData = original.basicData.copy(
                    id = newBasicId,
                    remoteObjectId = null,
                    remoteRouteId = null,
                    isSynchronized = false,
                    isDeleted = false,
                ),
                locomotives = original.locomotives.map { loco ->
                    loco.copy(
                        locoId = generateId(),
                        basicId = newBasicId,
                        remoteObjectId = null,
                        electricSectionList = loco.electricSectionList.map { s ->
                            s.copy(sectionId = generateId())
                        }.toMutableList(),
                        dieselSectionList = loco.dieselSectionList.map { s ->
                            s.copy(sectionId = generateId())
                        }.toMutableList(),
                    )
                }.toMutableList(),
                trains = original.trains.map { train ->
                    val newTrainId = generateId()
                    train.copy(
                        trainId = newTrainId,
                        basicId = newBasicId,
                        stations = train.stations.map { st ->
                            st.copy(stationId = generateId(), trainId = newTrainId)
                        }.toMutableList(),
                    )
                }.toMutableList(),
                passengers = original.passengers.map { p ->
                    p.copy(passengerId = generateId(), basicId = newBasicId)
                }.toMutableList(),
                photos = original.photos.map { ph ->
                    ph.copy(photoId = generateId(), basicId = newBasicId)
                }.toMutableList(),
            )
            routeUseCase.saveRoute(copiedRoute).first()
        }
    }

    /** Переключает флаг "Избранное" у маршрута. */
    fun toggleFavorite(routeId: String) {
        viewModelScope.launch {
            val route = _routes.value.firstOrNull { it.basicData.id == routeId } ?: return@launch
            routeUseCase.setFavoriteRoute(
                routeId = route.basicData.id,
                isFavorite = !route.basicData.isFavorite,
            ).first()
        }
    }

    /**
     * Синхронизирует один маршрут в облако (mirror Android HomeViewModel.syncRoute).
     * Использует уже подключённые RoutesManager + SecureTokenStorage из Koin.
     */
    fun syncRoute(routeId: String) {
        if (_isSyncingRoute.value) return
        viewModelScope.launch {
            val route = _routes.value.firstOrNull { it.basicData.id == routeId } ?: return@launch
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            if (token.isNullOrBlank()) {
                _messages.emit("Неавторизованный пользователь")
                return@launch
            }
            _isSyncingRoute.value = true
            try {
                routesManager.saveRouteInRemote(route, "Bearer $token").collect { state ->
                    when (state) {
                        is ResultState.Success -> {
                            routeUseCase.setSynchronizedRoute(route.basicData.id).first()
                            _messages.emit("Маршрут сохранён в облаке")
                        }
                        is ResultState.Error -> {
                            // explicit publish: пользователь нажал «Синхронизировать».
                            _error.value = state.entity.appError
                            Logger.withTag("Home").e {
                                "syncRoute failed: ${state.entity.message ?: state.entity.throwable?.message}"
                            }
                            // НЕ emit в _messages — alert уже показан через _error.
                            // Stack trace из throwable.message не должен попасть в toast.
                        }
                        is ResultState.Loading -> { /* ignore */ }
                    }
                }
            } finally {
                _isSyncingRoute.value = false
            }
        }
    }

    /**
     * Создаёт публичную ссылку на маршрут и эмитит её в [shareLinks].
     * Swift показывает UIActivityViewController.
     */
    fun shareRoute(routeId: String) {
        if (_isCreatingShareLink.value) return
        viewModelScope.launch {
            val route = _routes.value.firstOrNull { it.basicData.id == routeId } ?: return@launch
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            if (token.isNullOrBlank()) {
                // Офлайн-режим: всё равно даём deep-link с локальным id
                _shareLinks.emit(buildLocalShareText(route))
                _messages.emit("Ссылка сгенерирована офлайн")
                return@launch
            }
            _isCreatingShareLink.value = true
            try {
                shareRouteManager.createShareLink(route, "Bearer $token").collect { state ->
                    when (state) {
                        is ResultState.Success -> {
                            val text = buildShareTextWithLink(route, state.data)
                            _shareLinks.emit(text)
                        }
                        is ResultState.Error -> {
                            // explicit publish: пользователь нажал «Поделиться».
                            _error.value = state.entity.appError
                            Logger.withTag("Home").e {
                                "shareRoute failed: ${state.entity.message ?: state.entity.throwable?.message}"
                            }
                            // НЕ emit в _messages — alert уже показан через _error.
                            // Stack trace из throwable.message не должен попасть в toast.
                        }
                        is ResultState.Loading -> { /* ignore */ }
                    }
                }
            } finally {
                _isCreatingShareLink.value = false
            }
        }
    }

    /**
     * Pull-to-refresh: принудительная синхронизация с сервера.
     *
     * Только pull (syncFromRemote), не push. Двусторонний sync — отдельная
     * кнопка в Profile. При успехе — тихо (пользователь видит обновлённый
     * список, toast не нужен). При ошибке — explicit publish в _error +
     * Kermit.e + сброс _isRefreshing.
     */
    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            if (token.isNullOrBlank()) {
                _messages.emit("Войдите для синхронизации")
                return@launch
            }
            _isRefreshing.value = true
            _error.value = null
            try {
                syncManager.syncFromRemote("Bearer $token").collect { state ->
                    when (state) {
                        is ResultState.Success -> { /* silent при успехе */ }
                        is ResultState.Error -> {
                            _error.value = state.entity.appError
                            Logger.withTag("Home").e {
                                "refresh failed: ${state.entity.message}"
                            }
                        }
                        is ResultState.Loading -> {}
                    }
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun clearError() { _error.value = null }

    private fun buildLocalShareText(route: com.z_company.domain.entities.route.Route): String {
        val number = route.basicData.number?.let { "№$it " } ?: ""
        return "Маршрут $number\nlocodriver://share/${route.basicData.id}"
    }

    private fun buildShareTextWithLink(
        route: com.z_company.domain.entities.route.Route,
        link: String
    ): String {
        val number = route.basicData.number?.let { "№$it " } ?: ""
        return "Маршрут $number\n$link"
    }

    // ── watchState helpers ────────────────────────────────────────────────────

    fun watchRoutes(callback: (List<Route>) -> Unit) {
        viewModelScope.launch { routes.collect { callback(it) } }
    }

    fun watchSettings(callback: (UserSettings?) -> Unit) {
        viewModelScope.launch { settings.collect { callback(it) } }
    }

    fun watchIsLoading(callback: (Boolean) -> Unit) {
        viewModelScope.launch { isLoading.collect { callback(it) } }
    }

    fun watchCurrentMonth(callback: (Int) -> Unit) {
        viewModelScope.launch { currentMonth.collect { callback(it) } }
    }

    fun watchCurrentYear(callback: (Int) -> Unit) {
        viewModelScope.launch { currentYear.collect { callback(it) } }
    }

    fun watchTotalWorkMs(callback: (Long) -> Unit) {
        viewModelScope.launch { totalWorkMs.collect { callback(it) } }
    }
    fun watchNightWorkMs(callback: (Long) -> Unit) {
        viewModelScope.launch { nightWorkMs.collect { callback(it) } }
    }
    fun watchPassengerWorkMs(callback: (Long) -> Unit) {
        viewModelScope.launch { passengerWorkMs.collect { callback(it) } }
    }
    fun watchReserveWorkMs(callback: (Long) -> Unit) {
        viewModelScope.launch { reserveWorkMs.collect { callback(it) } }
    }
    fun watchOnePersonMs(callback: (Long) -> Unit) {
        viewModelScope.launch { onePersonMs.collect { callback(it) } }
    }
    fun watchNormaHoursMonth(callback: (Int) -> Unit) {
        viewModelScope.launch { normaHoursMonth.collect { callback(it) } }
    }
    fun watchNormaHoursToday(callback: (Int) -> Unit) {
        viewModelScope.launch { normaHoursToday.collect { callback(it) } }
    }
    fun watchTodayWorkMs(callback: (Long) -> Unit) {
        viewModelScope.launch { todayWorkMs.collect { callback(it) } }
    }
    fun watchMessages(callback: (String) -> Unit) {
        viewModelScope.launch { messages.collect { callback(it) } }
    }
    fun watchShareLinks(callback: (String) -> Unit) {
        viewModelScope.launch { shareLinks.collect { callback(it) } }
    }
    fun watchIsSyncingRoute(callback: (Boolean) -> Unit) {
        viewModelScope.launch { isSyncingRoute.collect { callback(it) } }
    }
    fun watchIsCreatingShareLink(callback: (Boolean) -> Unit) {
        viewModelScope.launch { isCreatingShareLink.collect { callback(it) } }
    }
    fun watchIsRefreshing(callback: (Boolean) -> Unit) {
        viewModelScope.launch { isRefreshing.collect { callback(it) } }
    }
    fun watchError(callback: (AppError?) -> Unit) {
        viewModelScope.launch { error.collect { callback(it) } }
    }
}
