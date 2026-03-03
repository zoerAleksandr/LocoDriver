package com.z_company.iosapp.viewmodel

import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.getBreakDuration
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.getNightTime
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

/**
 * KMP ViewModel for the salary calculation screen.
 *
 * Exposes monthly summary with total work/night/break time,
 * plus a per-route breakdown list.
 */
class SalaryCalculationIosViewModel(
    private val routeUseCase: RouteUseCase,
    private val settingsUseCase: SettingsUseCase,
) : ViewModel() {

    /** Per-route breakdown info. */
    data class RouteBreakdown(
        val routeId: String,
        val number: String,
        val workMs: Long,
        val breakMs: Long,
    )

    data class MonthlySummary(
        val month: String,
        val routeCount: Int,
        val totalWorkMs: Long,
        val totalBreakMs: Long,
        val totalNightMs: Long,
        val routes: List<RouteBreakdown>,
    ) {
        val totalWorkHours: Long get() = totalWorkMs / (1000L * 60 * 60)
        val totalWorkMinutes: Long get() = (totalWorkMs / (1000L * 60)) % 60
    }

    private val _summary = MutableStateFlow<MonthlySummary?>(null)
    val summary: StateFlow<MonthlySummary?> = _summary.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var routesJob: Job? = null

    init {
        viewModelScope.launch {
            settingsUseCase.getUserSettingFlow().collect { settings ->
                loadSummary(settings)
            }
        }
    }

    private fun loadSummary(settings: UserSettings) {
        routesJob?.cancel()
        routesJob = viewModelScope.launch {
            _isLoading.value = true
            routeUseCase.routeListByMonthFlow(
                monthOfYear = settings.selectMonthOfYear,
                offsetInMoscow = settings.timeZone,
            ).collect { routes ->
                val breakdowns = routes.map { route ->
                    val workMs = route.getWorkTime() ?: 0L
                    val breakMs = route.getBreakDuration()
                    RouteBreakdown(
                        routeId = route.basicData.id,
                        number = route.basicData.number?.takeIf { it.isNotBlank() } ?: "Без номера",
                        workMs = workMs,
                        breakMs = breakMs,
                    )
                }

                val totalWorkMs = breakdowns.sumOf { it.workMs }
                val totalBreakMs = breakdowns.sumOf { it.breakMs }

                // Calculate night time
                val totalNightMs = try {
                    routes.getNightTime(settings)
                } catch (_: Exception) {
                    0L
                }

                _summary.value = MonthlySummary(
                    month = formatMonth(settings),
                    routeCount = routes.size,
                    totalWorkMs = totalWorkMs,
                    totalBreakMs = totalBreakMs,
                    totalNightMs = totalNightMs,
                    routes = breakdowns,
                )
                _isLoading.value = false
            }
        }
    }

    private fun formatMonth(settings: UserSettings): String {
        val monthNames = listOf(
            "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь",
        )
        val moy = settings.selectMonthOfYear
        val name = monthNames.getOrElse(moy.month) { "?" }
        return "$name ${moy.year}"
    }
}
