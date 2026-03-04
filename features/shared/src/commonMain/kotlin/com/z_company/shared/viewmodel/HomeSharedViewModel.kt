package com.z_company.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Shared KMP ViewModel for the Home screen.
 *
 * Based on Android HomeViewModel with constructor injection.
 * Supports routes by month, month navigation, remove/favorite route.
 */
class HomeSharedViewModel(
    private val routeUseCase: RouteUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val calendarUseCase: CalendarUseCase,
) : ViewModel() {

    private val _routes = MutableStateFlow<List<Route>>(emptyList())
    val routes: StateFlow<List<Route>> = _routes.asStateFlow()

    private val _settings = MutableStateFlow<UserSettings?>(null)
    val settings: StateFlow<UserSettings?> = _settings.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _monthList = MutableStateFlow<List<Int>>(emptyList())
    val monthList: StateFlow<List<Int>> = _monthList.asStateFlow()

    private val _yearList = MutableStateFlow<List<Int>>(emptyList())
    val yearList: StateFlow<List<Int>> = _yearList.asStateFlow()

    private var routesJob: Job? = null

    init {
        // Load calendar months/years
        viewModelScope.launch {
            calendarUseCase.loadFlowMonthOfYearListState().collect { list ->
                _monthList.value = list.map { it.month }.distinct().sorted()
                _yearList.value = list.map { it.year }.distinct().sorted()
            }
        }

        // Settings → routes reactivity
        viewModelScope.launch {
            settingsUseCase.getUserSettingFlow().collectLatest { userSettings ->
                _settings.value = userSettings
                loadRoutesForMonth(userSettings)
            }
        }
    }

    private fun loadRoutesForMonth(userSettings: UserSettings) {
        routesJob?.cancel()
        routesJob = viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            routeUseCase.routeListByMonthFlow(
                monthOfYear = userSettings.selectMonthOfYear,
                offsetInMoscow = userSettings.timeZone,
            ).collect { routes ->
                _routes.value = routes.sortedByDescending { it.basicData.timeStartWork ?: 0L }
                _isLoading.value = false
            }
        }
    }

    fun removeRoute(route: Route) {
        viewModelScope.launch {
            routeUseCase.markAsRemoved(route).collect { result ->
                if (result is ResultState.Error) {
                    _errorMessage.value = result.entity.message ?: "Ошибка удаления"
                }
            }
        }
    }

    fun setFavoriteRoute(route: Route) {
        viewModelScope.launch {
            val updated = route.copy(
                basicData = route.basicData.copy(isFavorite = !route.basicData.isFavorite)
            )
            routeUseCase.saveRoute(updated).collect { result ->
                if (result is ResultState.Error) {
                    _errorMessage.value = result.entity.message ?: "Ошибка"
                }
            }
        }
    }

    fun setNextMonth() {
        val current = _settings.value ?: return
        val moy = current.selectMonthOfYear
        val newMonth = if (moy.month >= 11) 0 else moy.month + 1
        val newYear = if (moy.month >= 11) moy.year + 1 else moy.year
        updateMonth(newYear, newMonth)
    }

    fun setPreviousMonth() {
        val current = _settings.value ?: return
        val moy = current.selectMonthOfYear
        val newMonth = if (moy.month <= 0) 11 else moy.month - 1
        val newYear = if (moy.month <= 0) moy.year - 1 else moy.year
        updateMonth(newYear, newMonth)
    }

    fun setCurrentMonth(year: Int, month: Int) {
        updateMonth(year, month)
    }

    private fun updateMonth(year: Int, month: Int) {
        viewModelScope.launch {
            settingsUseCase.updateMonthOfYearInUserSetting(
                MonthOfYear(year = year, month = month)
            ).collect { /* result */ }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
