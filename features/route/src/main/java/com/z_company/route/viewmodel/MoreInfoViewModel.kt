package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHours
import com.z_company.domain.entities.UtilForMonthOfYear.getNormaHoursInDate
import com.z_company.domain.entities.route.UtilsForEntities.getNightTime
import com.z_company.domain.entities.route.UtilsForEntities.getOnePersonOperationTime
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTimeWithoutHoliday
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
import java.util.Calendar

class MoreInfoViewModel: ViewModel(), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()

    private var loadSettingJob: Job? = null
    private val _uiState = MutableStateFlow(MoreInfoUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadSetting()
    }

    private fun loadSetting() {
        loadSettingJob?.cancel()
        loadSettingJob = viewModelScope.launch {
            settingsUseCase.getFlowCurrentSettingsState().collect { result ->
                if (result is ResultState.Success) {
                    result.data?.let { settings ->
                        getTotalWorkTime(settings)
                    }
                }
            }
        }
    }

    private fun getTotalWorkTime(settings: UserSettings) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val currentTimeInMillis = Calendar.getInstance().timeInMillis
                    routeUseCase.listRoutesByMonth(settings.selectMonthOfYear, settings.timeZone)
                        .collect { result ->
                            if (result is ResultState.Success) {
                                val routeList = if (settings.isConsiderFutureRoute) {
                                    result.data
                                } else {
                                    result.data.filter { it.basicData.timeStartWork!! < currentTimeInMillis }
                                }

                                val totalWorkTime =
                                    routeList.getWorkTime(
                                        settings.selectMonthOfYear,
                                        settings.timeZone
                                    )
                                val nightTime = routeList.getNightTime(settings)
                                val passengerTime =
                                    routeList.getPassengerTime(
                                        settings.selectMonthOfYear,
                                        settings.timeZone
                                    )
                                val holidayWorkTime =
                                    routeList.getWorkingTimeOnAHoliday(
                                        settings.selectMonthOfYear,
                                        settings.timeZone
                                    )
                                val workTimeWithHoliday =
                                    routeList.getWorkTimeWithoutHoliday(
                                        settings.selectMonthOfYear,
                                        settings.timeZone
                                    )
                                val timeBalance =
                                    workTimeWithHoliday - settings.selectMonthOfYear.getPersonalNormaHours()
                                        .times(3_600_000)
                                val onePersonTime =
                                    routeList.getOnePersonOperationTime(
                                        settings.selectMonthOfYear,
                                        settings.timeZone
                                    )
                                withContext(Dispatchers.Main) {
                                    _uiState.update {
                                        it.copy(
                                            routesCount = ResultState.Success(routeList.size),
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
                                            ),
                                            onePersonTimeState = ResultState.Success(
                                                onePersonTime
                                            ),
                                            todayNormaHours = ResultState.Success(settings.selectMonthOfYear.getNormaHoursInDate(currentTimeInMillis)),
                                            currentMonthOfYearState = ResultState.Success(settings.selectMonthOfYear)
                                        )
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
