package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.entities.route.UtilsForEntities.getNightTime
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getTotalWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkingTimeOnAHoliday
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MoreInfoViewModel(private val monthOfYearId: String) : ViewModel(), KoinComponent {
    private val calendarUseCase: CalendarUseCase by inject()
    private val routeUseCase: RouteUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()

    private var loadSettingJob: Job? = null
    private val _uiState = MutableStateFlow(MoreInfoUiState())
    val uiState = _uiState.asStateFlow()

    private var currentMonthOfYear: MonthOfYear?
        get() {
            return _uiState.value.currentMonthOfYearState.let {
                if (it is ResultState.Success) it.data else null
            }
        }
        private set(value) {
            _uiState.update {
                it.copy(currentMonthOfYearState = ResultState.Success(value))
            }
            value?.let {
                getTotalWorkTime()
            }
        }

    private var userSettings: UserSettings? = null
        private set(value) {
            value?.let {
                currentMonthOfYear = value.selectMonthOfYear
            }
            field = value
        }

    init {
        loadSetting()
    }

    private fun loadSetting() {
        loadSettingJob?.cancel()
        loadSettingJob = viewModelScope.launch {
            settingsUseCase.getCurrentSettings().collect { result ->
                if (result is ResultState.Success) {
                    userSettings = result.data

                }
            }
        }
    }

    private fun getTotalWorkTime() {
        currentMonthOfYear?.let { monthOfYear ->
            viewModelScope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        routeUseCase.listRoutesByMonth(monthOfYear).collect { result ->
                            if (result is ResultState.Success) {
                                val totalWorkTime = result.data.getTotalWorkTime(monthOfYear)
                                userSettings?.let { settings ->
                                    val nightTime = result.data.getNightTime(settings)
                                    val passengerTime = result.data.getPassengerTime(monthOfYear)
                                    val holidayWorkTime = result.data.getWorkingTimeOnAHoliday(monthOfYear)
                                    withContext(Dispatchers.Main) {
                                        _uiState.update {
                                            it.copy(
                                                totalWorkTimeState = ResultState.Success(totalWorkTime),
                                                nightTimeState = ResultState.Success(nightTime),
                                                passengerTimeState = ResultState.Success(passengerTime),
                                                holidayWorkTimeState = ResultState.Success(holidayWorkTime)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }.onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            totalWorkTimeState = ResultState.Error(ErrorEntity(throwable))
                        )
                    }
                }
            }
        }
    }
}
