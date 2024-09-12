package com.z_company.route.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getTotalWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.timeFollowingSingleLocomotive
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar

class SalaryCalculationViewModel : ViewModel(), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()

    private var userSettings: UserSettings? = null

    private val _uiState = MutableStateFlow(SalaryCalculationUIState())
    val uiState = _uiState.asStateFlow()
    private fun loadUserSetting() {
        viewModelScope.launch {
            settingsUseCase.getCurrentSettings().collect { result ->
                if (result is ResultState.Success) {
                    userSettings = result.data
                    getTotalWorkTime()
                }
            }
        }
    }

    private fun getTotalWorkTime() {
        val currentTimeInMillis = Calendar.getInstance().timeInMillis
        userSettings?.let { settings ->
            viewModelScope.launch {
                val currentMonthOfYear = settings.selectMonthOfYear
                routeUseCase.listRoutesByMonth(currentMonthOfYear).collect { loadRouteState ->
                    if (loadRouteState is ResultState.Success) {
                        val routeList = if (settings.isConsiderFutureRoute) {
                            loadRouteState.data
                        } else {
                            loadRouteState.data.filter { it.basicData.timeStartWork!! < currentTimeInMillis }
                        }

                        val totalWorkTime = routeList.getTotalWorkTime(currentMonthOfYear)
                        val passengerTime = routeList.getPassengerTime(currentMonthOfYear)
                        var singleLocoTimeFollowing = 0L
                        routeList.forEach { route ->
                            route.trains.forEach { train ->
                                singleLocoTimeFollowing += train.timeFollowingSingleLocomotive()
                            }
                        }
                        val workTimeAtTariff = totalWorkTime - passengerTime - singleLocoTimeFollowing
                        Log.d("ZZZ", "workTimeAtTariff - $workTimeAtTariff")
                        Log.d("ZZZ", "passengerTime - $passengerTime")
                        Log.d("ZZZ", "singleLocoTimeFollowing - $singleLocoTimeFollowing")

                        _uiState.update {
                            it.copy(
                                paymentAtTariffHours = workTimeAtTariff,
                                paymentAtPassengerHours = passengerTime,
                                paymentAtSingleLocomotiveHours = singleLocoTimeFollowing
                            )
                        }
                    }
                }
            }
        }
    }

    init {
        loadUserSetting()
    }
}