package com.z_company.route.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHours
import com.z_company.domain.entities.UtilForMonthOfYear.getTodayNormaHours
import com.z_company.domain.entities.route.UtilsForEntities.getNightTime
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getTotalWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTimeWithHoliday
import com.z_company.domain.entities.route.UtilsForEntities.getWorkingTimeOnAHoliday
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
                getTodayNormaHours()
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

    private fun getTodayNormaHours() {

        // исправить
        currentMonthOfYear?.let { monthOfYear ->
            _uiState.update {
               it.copy(
                   todayNormaHours = ResultState.Success(monthOfYear.getTodayNormaHours())
               )
            }
        }
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
                                    val holidayWorkTime =
                                        result.data.getWorkingTimeOnAHoliday(monthOfYear)
                                    val workTimeWithHoliday =
                                        result.data.getWorkTimeWithHoliday(monthOfYear)
                                    val timeBalance = workTimeWithHoliday - monthOfYear.getPersonalNormaHours().times(3_600_000)
                                    withContext(Dispatchers.Main) {
                                        _uiState.update {
                                            it.copy(
                                                totalWorkTimeState = ResultState.Success(
                                                    totalWorkTime
                                                ),
                                                nightTimeState = ResultState.Success(nightTime),
                                                passengerTimeState = ResultState.Success(
                                                    passengerTime
                                                ),
                                                holidayWorkTimeState = ResultState.Success(
                                                    holidayWorkTime
                                                ),
                                                workTimeWithHoliday = ResultState.Success(
                                                    workTimeWithHoliday
                                                ),
                                                timeBalanceState = ResultState.Success(
                                                    timeBalance
                                                )
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
