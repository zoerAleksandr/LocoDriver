package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.core.util.DateAndTimeConverter.getMonthFullText
import com.z_company.domain.entities.SalarySetting
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHours
import com.z_company.domain.entities.UtilForMonthOfYear.getStandardNormaHours
import com.z_company.domain.entities.route.UtilsForEntities.getNightTime
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getTotalWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkingTimeOnAHoliday
import com.z_company.domain.entities.route.UtilsForEntities.timeFollowingSingleLocomotive
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar
import com.z_company.domain.util.minus
import com.z_company.domain.util.plus

class SalaryCalculationViewModel : ViewModel(), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val salarySettingUseCase: SalarySettingUseCase by inject()

    private var userSettings: UserSettings? = null
    private var salarySetting: SalarySetting? = null

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

    private fun loadSalarySetting() {
        viewModelScope.launch {
            salarySettingUseCase.getSalarySetting().collect { result ->
                if (result is ResultState.Success) {
                    salarySetting = result.data
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
                        val personalNormaHoursInLong =
                            settings.selectMonthOfYear.getPersonalNormaHours() * 3_600_000
                        val paymentHolidayHours =
                            routeList.getWorkingTimeOnAHoliday(currentMonthOfYear)

                        val workTimeAtTariff =
                            totalWorkTime - passengerTime - singleLocoTimeFollowing - paymentHolidayHours
                        val overTime =
                            if (totalWorkTime > personalNormaHoursInLong) totalWorkTime - personalNormaHoursInLong else 0L
                        val routeCount = routeList.size
                        val surchargeAtOvertime05Hour = if (routeCount != 0 && overTime / routeCount < 7_200_000) {
                            overTime
                        } else {
                            routeCount.toLong() * 7_200_000L
                        }
                        val surchargeAtOvertimeHour =
                            if (overTime > surchargeAtOvertime05Hour) overTime - surchargeAtOvertime05Hour else 0L
                        val paymentNightTimeHours = routeList.getNightTime(settings)
                        val normaHours = currentMonthOfYear.getStandardNormaHours()

                        _uiState.update {
                            it.copy(
                                month = currentMonthOfYear.month.getMonthFullText(),
                                totalWorkTime = totalWorkTime,
                                paymentAtTariffHours = workTimeAtTariff,
                                paymentAtPassengerHours = passengerTime,
                                paymentAtSingleLocomotiveHours = singleLocoTimeFollowing,
                                paymentAtOvertimeHours = overTime,
                                surchargeAtOvertime05Hours = surchargeAtOvertime05Hour,
                                surchargeAtOvertimeHours = surchargeAtOvertimeHour,
                                paymentHolidayHours = paymentHolidayHours,
                                surchargeHolidayHours = paymentHolidayHours,
                                paymentNightTimeHours = paymentNightTimeHours,
                                normaHours = normaHours
                            )
                        }
                        calculationMoney()
                    }
                }
            }
        }
    }

    private fun calculationMoney() {
        salarySetting?.let { setting ->
            val paymentAtTariffHours = uiState.value.paymentAtTariffHours
            val paymentAtPassengerHours = uiState.value.paymentAtPassengerHours
            val paymentAtSingleLocomotiveHours = uiState.value.paymentAtSingleLocomotiveHours
            val overtimeHour = uiState.value.paymentAtOvertimeHours
            val surchargeAtOvertime05Hours = uiState.value.surchargeAtOvertime05Hours
            val surchargeAtOvertimeHours = uiState.value.surchargeAtOvertimeHours
            val paymentHolidayHours = uiState.value.paymentHolidayHours
            val totalWorkTime = uiState.value.totalWorkTime
            val paymentNightTimeHours = uiState.value.paymentNightTimeHours

            val paymentAtTariffMoney = paymentAtTariffHours?.times(setting.tariffRate)
            val paymentAtPassengerMoney = paymentAtPassengerHours?.times(setting.tariffRate)
            val paymentAtSingleLocomotiveMoney =
                paymentAtSingleLocomotiveHours?.times(setting.tariffRate)
            val paymentAtOvertimeMoney = overtimeHour?.times(setting.tariffRate)
            val surchargeAtOvertime05Money =
                surchargeAtOvertime05Hours?.times(setting.tariffRate * 0.5)
            val surchargeAtOvertimeMoney = surchargeAtOvertimeHours?.times(setting.tariffRate)
            val paymentHolidayMoney = paymentHolidayHours?.times(setting.tariffRate)
            val surchargeHolidayMoney = paymentHolidayMoney
            val zonalSurchargePercent = setting.zonalSurcharge
            val zonalSurchargeMoney =
                (totalWorkTime - paymentAtPassengerHours)?.times(setting.tariffRate * (zonalSurchargePercent / 100))
            val paymentNightTimePercent = 40.0
            val paymentNightTimeMoney =
                paymentNightTimeHours?.times(setting.tariffRate * (paymentNightTimePercent / 100))
            val surchargeQualificationClassPercent = setting.surchargeQualificationClass
            val surchargeQualificationClassMoney =
                (totalWorkTime - paymentAtPassengerHours)?.times(setting.tariffRate * (surchargeQualificationClassPercent / 100))

            val totalChargedMoney =
                paymentAtTariffMoney + paymentAtPassengerMoney + paymentAtSingleLocomotiveMoney + paymentAtOvertimeMoney +
                        surchargeAtOvertime05Money + surchargeAtOvertimeMoney + paymentHolidayMoney + surchargeHolidayMoney +
                        zonalSurchargeMoney + paymentNightTimeMoney + surchargeQualificationClassMoney


            val retentionNdfl = totalChargedMoney?.times(0.13)
            val otherRetentionPercent = setting.otherRetention
            val otherRetention = totalChargedMoney?.times(otherRetentionPercent / 100)
            val unionistsRetention = totalChargedMoney?.times(setting.unionistsRetention / 100)
            val totalRetention = retentionNdfl + unionistsRetention + otherRetention

            val toBeCredited = totalChargedMoney - totalRetention
            val tariffRate = setting.tariffRate

            _uiState.update {
                it.copy(
                    paymentAtTariffMoney = paymentAtTariffMoney,
                    paymentAtPassengerMoney = paymentAtPassengerMoney,
                    paymentAtSingleLocomotiveMoney = paymentAtSingleLocomotiveMoney,
                    paymentAtOvertimeMoney = paymentAtOvertimeMoney,
                    surchargeAtOvertime05Money = surchargeAtOvertime05Money,
                    surchargeAtOvertimeMoney = surchargeAtOvertimeMoney,
                    paymentHolidayMoney = paymentHolidayMoney,
                    surchargeHolidayMoney = surchargeHolidayMoney,
                    zonalSurchargePercent = zonalSurchargePercent,
                    zonalSurchargeMoney = zonalSurchargeMoney,
                    paymentNightTimePercent = paymentNightTimePercent,
                    paymentNightTimeMoney = paymentNightTimeMoney,
                    surchargeQualificationClassPercent = surchargeQualificationClassPercent,
                    surchargeQualificationClassMoney = surchargeQualificationClassMoney,
                    totalChargedMoney = totalChargedMoney,
                    retentionNdfl = retentionNdfl,
                    otherRetention = otherRetention,
                    unionistsRetention = unionistsRetention,
                    totalRetention = totalRetention,
                    toBeCredited = toBeCredited,
                    tariffRate = tariffRate
                )
            }
        }
    }

    fun loadData() {
        loadUserSetting()
        loadSalarySetting()
    }
}