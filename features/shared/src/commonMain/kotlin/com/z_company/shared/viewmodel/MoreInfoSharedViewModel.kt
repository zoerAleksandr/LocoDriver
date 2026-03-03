package com.z_company.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.route.UtilsForEntities.getBreakDuration
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shared ViewModel for the "more info" monthly statistics screen.
 */
class MoreInfoSharedViewModel(
    private val routeUseCase: RouteUseCase,
    private val settingsUseCase: SettingsUseCase,
) : ViewModel() {

    data class RouteInfo(
        val routeId: String,
        val number: String,
        val startMs: Long?,
        val workMs: Long,
        val breakMs: Long,
    )

    data class MonthStats(
        val monthLabel: String,
        val routeCount: Int,
        val totalWorkMs: Long,
        val totalBreakMs: Long,
        val averageWorkMs: Long,
        val routes: List<RouteInfo>,
    )

    private val _stats = MutableStateFlow<MonthStats?>(null)
    val stats: StateFlow<MonthStats?> = _stats.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadForMonth(monthId: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            settingsUseCase.getUserSettingFlow().collect { settings ->
                val targetMonth = resolveMonth(settings, monthId)
                loadRoutesForMonth(settings, targetMonth)
            }
        }
    }

    private fun resolveMonth(settings: UserSettings, monthId: String?): MonthOfYear {
        if (monthId == null) return settings.selectMonthOfYear
        val selected = settings.selectMonthOfYear
        return if (selected.id == monthId) selected else settings.selectMonthOfYear
    }

    private suspend fun loadRoutesForMonth(settings: UserSettings, month: MonthOfYear) {
        routeUseCase.routeListByMonthFlow(
            monthOfYear = month,
            offsetInMoscow = settings.timeZone,
        ).collect { routes ->
            val routeInfos = routes.map { route ->
                RouteInfo(
                    routeId = route.basicData.id,
                    number = route.basicData.number?.takeIf { it.isNotBlank() } ?: "Без номера",
                    startMs = route.basicData.timeStartWork,
                    workMs = route.getWorkTime() ?: 0L,
                    breakMs = route.getBreakDuration(),
                )
            }
            val totalWork = routeInfos.sumOf { it.workMs }
            val totalBreak = routeInfos.sumOf { it.breakMs }
            val avg = if (routeInfos.isNotEmpty()) totalWork / routeInfos.size else 0L

            _stats.value = MonthStats(
                monthLabel = formatMonth(month),
                routeCount = routes.size,
                totalWorkMs = totalWork,
                totalBreakMs = totalBreak,
                averageWorkMs = avg,
                routes = routeInfos.sortedByDescending { it.startMs ?: 0L },
            )
            _isLoading.value = false
        }
    }

    private fun formatMonth(moy: MonthOfYear): String {
        val names = listOf(
            "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь",
        )
        return "${names.getOrElse(moy.month) { "?" }} ${moy.year}"
    }
}
