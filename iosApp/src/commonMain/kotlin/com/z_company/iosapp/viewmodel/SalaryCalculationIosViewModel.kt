package com.z_company.iosapp.viewmodel

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
 * KMP ViewModel для экрана «Расчёт зарплаты».
 *
 * Вместо неиспользуемого SalaryCalculationUseCase (отмечен "НЕ ИСПОЛЬЗУЕТСЯ !!!") использует
 * RouteUseCase + SettingsUseCase — как это сделано в Android SalaryCalculationViewModel.
 */
class SalaryCalculationIosViewModel(
    private val routeUseCase: RouteUseCase,
    private val settingsUseCase: SettingsUseCase,
) : ViewModel() {

    data class MonthlySummary(
        val month: String,
        val routeCount: Int,
        val totalWorkMs: Long,
    ) {
        /** Общее время работы в часах (целая часть). */
        val totalWorkHours: Long get() = totalWorkMs / (1000L * 60 * 60)

        /** Остаток минут после вычитания полных часов. */
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
                val totalWorkMs = routes.sumOf { route ->
                    val start = route.basicData.timeStartWork ?: 0L
                    val end = route.basicData.timeEndWork ?: 0L
                    if (end > start) end - start else 0L
                }
                _summary.value = MonthlySummary(
                    month = formatMonth(settings),
                    routeCount = routes.size,
                    totalWorkMs = totalWorkMs,
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
