@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.util.generateId
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Clock
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
) : ViewModel() {

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

    private var routesJob: Job? = null

    init {
        viewModelScope.launch {
            settingsUseCase.getUserSettingFlow().collect { userSettings ->
                _settings.value = userSettings
                // Sync currentMonth/Year from settings on first load
                val moy = userSettings.selectMonthOfYear
                _currentMonth.value = moy.month
                _currentYear.value = moy.year
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
            }
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

    /** Удаляет маршрут по его basicData.id. */
    fun deleteRoute(routeId: String) {
        viewModelScope.launch {
            val route = _routes.value.firstOrNull { it.basicData.id == routeId } ?: return@launch
            routeUseCase.markAsRemoved(route).first()
            // After marking as removed, remove from local list immediately for responsiveness
            _routes.value = _routes.value.filter { it.basicData.id != routeId }
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
}
