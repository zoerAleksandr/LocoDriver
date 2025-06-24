package com.z_company.route.viewmodel

import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Path.Companion.combine
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.DateSetTariffRate
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.SalarySetting
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.entities.route.Route
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue
import kotlin.time.Duration.Companion.seconds

class SalaryCalculationTestViewModel : ViewModel(), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val salarySettingUseCase: SalarySettingUseCase by inject()
    private val calendarUseCase: CalendarUseCase by inject()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Сообщение статуса загрузки
    private val _loadingMessage = MutableStateFlow("Загрузка данных...")
    val loadingMessage: StateFlow<String> = _loadingMessage.asStateFlow()

    private val _uiState = MutableStateFlow(SalaryCalculationUIState())
    val uiState = _uiState.asStateFlow()

    // переменные
    var routeList: List<Route> = emptyList()
    var salarySetting = SalarySetting()
    var userSettings = UserSettings()
    var currentMonthOfYear = MonthOfYear()
    var offsetInMoscow = 0L
    var dateSetTariffRate: DateSetTariffRate? = null
    private lateinit var salaryCalculationHelper: SalaryCalculationHelper

    init {
        loadSetting()
    }

    private fun loadSetting() {
        viewModelScope.launch {
            val salarySettingFlow = salarySettingUseCase.salarySettingFlow()
            val userSettingFlow = settingsUseCase.getUserSettingFlow()

            // flow c настройками оставить открытыми
            salarySettingFlow.combine(userSettingFlow) { salSetting, usSetting ->
                salarySetting = salSetting
                userSettings = usSetting
                currentMonthOfYear = userSettings.selectMonthOfYear
                offsetInMoscow = userSettings.timeZone
                dateSetTariffRate = currentMonthOfYear.dateSetTariffRate
            }.collectLatest {
                Log.d("ZZZ", "loadSettingFlow userSettings ${userSettings}")
                Log.d("ZZZ", "loadSettingFlow salarySetting ${salarySetting}")
                calculationSalary()
            }
        }
    }

    private suspend fun calculationSalary() {
        _loadingMessage.value = "Выполнение расчетов..."
        val routeListFlow = routeUseCase.routeListByMonthFlow(
            monthOfYear = currentMonthOfYear,
            offsetInMoscow = offsetInMoscow
        )

        routeListFlow.first()
            .let { routes ->
                routeList = routes
            }

        salaryCalculationHelper = SalaryCalculationHelper(
            userSettings = userSettings,
            routeList = routeList,
            salarySetting = salarySetting
        )

        delay(1500L)

        combine(
            setToTariffRate(),
            setNightTime(),
        ) { result ->
            result.all { it == true }
        }.collect {
            _isLoading.value = false
        }
    }

    private fun setToTariffRate(): Flow<Boolean> {
        return flow {
            combine(
                salaryCalculationHelper.getWorkTimeAtTariffFlow(),
                salaryCalculationHelper.getMoneyAtWorkTimeAtTariff()
            ) { workTime, money ->
                _uiState.update {
                    it.copy(
                        paymentAtTariffHours = workTime,
                        paymentAtTariffMoney = money
                    )
                }
                emit(true)
            }.collect()
        }
    }

    private suspend fun setNightTime(): Flow<Boolean> {
        return flow {
            emit(true)
        }
    }
}