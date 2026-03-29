package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * KMP ViewModel для главного экрана.
 *
 * Упрощённая версия HomeViewModel из features/route (Android), без:
 *   - KoinComponent (используется конструкторная инжекция)
 *   - RuStore AppUpdate (Android-специфично)
 *   - java.util.Calendar (заменён kotlinx-datetime в UseCases)
 *   - Activity-констант
 *
 * Регистрируется в Koin через iosUseCaseModule.
 *
 * Шаг 17: добавить SalaryCalculationUseCase для отображения итоговой зарплаты.
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
                _routes.value = routes
                _isLoading.value = false
            }
        }
    }

    // ── watchState helpers ────────────────────────────────────────────────────
    // Позволяют Swift-коду подписаться на отдельные потоки через callback.

    /** Подписка на список маршрутов. */
    fun watchRoutes(callback: (List<Route>) -> Unit) {
        viewModelScope.launch { routes.collect { callback(it) } }
    }

    /** Подписка на настройки пользователя. */
    fun watchSettings(callback: (UserSettings?) -> Unit) {
        viewModelScope.launch { settings.collect { callback(it) } }
    }

    /** Подписка на флаг загрузки. */
    fun watchIsLoading(callback: (Boolean) -> Unit) {
        viewModelScope.launch { isLoading.collect { callback(it) } }
    }
}
