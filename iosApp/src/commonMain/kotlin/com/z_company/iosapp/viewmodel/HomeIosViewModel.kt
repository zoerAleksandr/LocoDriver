package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    private var routesJob: Job? = null

    init {
        viewModelScope.launch {
            settingsUseCase.getUserSettingFlow().collect { userSettings ->
                _settings.value = userSettings
                loadRoutesForMonth(userSettings)
            }
        }
    }

    private fun loadRoutesForMonth(userSettings: UserSettings) {
        routesJob?.cancel()
        routesJob = viewModelScope.launch {
            _isLoading.value = true
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
            routeUseCase.markAsRemoved(route).collect { /* result */ }
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

    private fun updateMonth(year: Int, month: Int) {
        viewModelScope.launch {
            settingsUseCase.updateMonthOfYearInUserSetting(
                MonthOfYear(year = year, month = month)
            ).collect { /* result */ }
        }
    }
}
