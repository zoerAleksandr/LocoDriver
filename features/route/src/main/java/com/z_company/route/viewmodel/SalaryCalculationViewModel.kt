package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getTotalWorkTime
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar

class SalaryCalculationViewModel : ViewModel(), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()

    private var userSettings: UserSettings? = null
    private fun loadUserSetting() {
        viewModelScope.launch {
            settingsUseCase.getCurrentSettings().collect { result ->
                if (result is ResultState.Success) {
                    userSettings = result.data
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

                    }
                }
            }
        }
    }

    init {
        loadUserSetting()
    }
}