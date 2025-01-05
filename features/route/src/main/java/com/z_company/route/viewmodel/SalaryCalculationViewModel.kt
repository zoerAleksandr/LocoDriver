package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.core.util.DateAndTimeConverter.getMonthFullText
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.SalarySetting
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHours
import com.z_company.domain.entities.UtilForMonthOfYear.getStandardNormaHours
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.getNightTime
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getTimeInServicePhase
import com.z_company.domain.entities.route.UtilsForEntities.setWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkingTimeOnAHoliday
import com.z_company.domain.entities.route.UtilsForEntities.isHeavyLongDistanceTrain
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
import com.z_company.domain.util.sum
import com.z_company.domain.util.toDoubleOrZero

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
                    setWorkTime()
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

    private fun setWorkTime() {
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

                        val totalWorkTime = getTotalWorkTime(routeList, currentMonthOfYear)
                        val passengerTime = getPassengerTime(routeList, currentMonthOfYear)
                        val singleLocoTime = getSingleLocomotiveTime(routeList)
                        val personalNormaHoursInLong = getPersonalNormaInLong(settings)
                        val paymentHolidayHours = getHolidayTime(routeList, currentMonthOfYear)

                        val workTimeAtTariff = getWorkTimeAtTariff(totalWorkTime, passengerTime, singleLocoTime, paymentHolidayHours)

                        val overTime = getOvertime(totalWorkTime, personalNormaHoursInLong)
                        val surchargeAtOvertime05Hour = getOvertime05Surcharge(routeList, overTime)
                        val surchargeAtOvertimeHour = getOvertimeSurcharge(overTime, surchargeAtOvertime05Hour)

                        val nightTimeHours = getNightTime(routeList, settings)
                        val normaHours = currentMonthOfYear.getStandardNormaHours()

                        // calculation surcharge Extended Service Phase
                        setSurchargeExtendedServicePhase(routeList)

                        val totalTimeHeavyLongDistance = getHeavyLongDistanceTime(routeList)

                        _uiState.update {
                            it.copy(
                                month = currentMonthOfYear.month.getMonthFullText(),
                                normaHours = normaHours,
                                totalWorkTime = totalWorkTime,
                                paymentAtTariffHours = workTimeAtTariff,
                                paymentAtPassengerHours = passengerTime,
                                paymentAtSingleLocomotiveHours = singleLocoTime,
                                paymentAtOvertimeHours = overTime,
                                surchargeAtOvertime05Hours = surchargeAtOvertime05Hour,
                                surchargeAtOvertimeHours = surchargeAtOvertimeHour,
                                paymentHolidayHours = paymentHolidayHours,
                                surchargeHolidayHours = paymentHolidayHours,
//                                paymentNightTimeHours = nightTimeHours,
//                                surchargeHeavyLongDistanceTrainsHour = totalTimeHeavyLongDistance
                            )
                        }
                        calculationMoney()
                    }
                }
            }
        }
    }

    private fun setSurchargeHeavyLongDistanceData(routeList: List<Route>, salarySetting: SalarySetting){
        val totalTimeHeavyLongDistance = getHeavyLongDistanceTime(routeList)
        val surchargeHeavyLongDistanceTrainsPercent = salarySetting.surchargeHeavyLongDistanceTrains
        val surchargeHeavyLongDistanceTrainsMoney =
            totalTimeHeavyLongDistance.times(salarySetting.tariffRate * (surchargeHeavyLongDistanceTrainsPercent / 100))

        _uiState.update {
            it.copy(
                surchargeHeavyLongDistanceTrainsHour = totalTimeHeavyLongDistance,
                surchargeHeavyLongDistanceTrainsPercent = surchargeHeavyLongDistanceTrainsPercent,
                surchargeHeavyLongDistanceTrainsMoney = surchargeHeavyLongDistanceTrainsMoney,
            )
        }
    }

    private fun setNightTimeData(routeList: List<Route>, userSettings: UserSettings, salarySetting: SalarySetting){
        val nightTimeHours = getNightTime(routeList, userSettings)
        val paymentNightTimePercent = 40.0
        val paymentNightTimeMoney =
            nightTimeHours.times(salarySetting.tariffRate * (paymentNightTimePercent / 100))

        _uiState.update {
            it.copy(
                paymentNightTimeHours = nightTimeHours,
                paymentNightTimePercent = paymentNightTimePercent,
                paymentNightTimeMoney = paymentNightTimeMoney,
            )
        }
    }

    private fun setSurchargeExtendedServicePhase(routeList: List<Route>) {
        salarySetting?.let { salarySetting ->
            val phaseList =
                salarySetting.surchargeExtendedServicePhaseList.sortedBy {
                    it.distance
                }
            val timeList: MutableList<Long> = mutableListOf()
            val percentList = phaseList.map {
                it.percentSurcharge
            }
            val moneyList: MutableList<Double> = mutableListOf()
            phaseList.forEachIndexed { index, _ ->
                var totalTimeInServicePhase = 0L
                routeList.forEach { route ->
                    totalTimeInServicePhase += route.getTimeInServicePhase(
                        phaseList.map { it.distance.toIntOrNull() ?: 0 },
                        index
                    )
                }
                timeList.add(totalTimeInServicePhase)
                moneyList.add(totalTimeInServicePhase.times(salarySetting.tariffRate * (percentList[index].toDoubleOrZero() / 100)))
            }
            _uiState.update {
                it.copy(
                    surchargeExtendedServicePhaseHour = timeList,
                    surchargeExtendedServicePhasePercent = percentList,
                    surchargeExtendedServicePhaseMoney = moneyList
                )
            }
        }
    }

    private fun getHeavyLongDistanceTime(
        routeList: List<Route>,
    ): Long {
        var totalTimeHeavyLongDistance = 0L
        routeList.forEach { route ->
            if (route.isHeavyLongDistanceTrain())
                totalTimeHeavyLongDistance += route.getWorkTime() ?: 0L
        }
        return totalTimeHeavyLongDistance
    }

    private fun getNightTime(
        routeList: List<Route>,
        settings: UserSettings
    ) = routeList.getNightTime(settings)

    private fun getWorkTimeAtTariff(
        totalWorkTime: Long,
        passengerTime: Long,
        singleLocoTime: Long,
        paymentHolidayHours: Long
    ) = totalWorkTime - passengerTime - singleLocoTime - paymentHolidayHours

    private fun getOvertimeSurcharge(overTime: Long, surchargeAtOvertime05Hour: Long) =
        if (overTime > surchargeAtOvertime05Hour) overTime - surchargeAtOvertime05Hour else 0L

    private fun getOvertime05Surcharge(routeList: List<Route>, overTime: Long): Long {
        val routeCount = routeList.size
        return if (routeCount != 0 && overTime / routeCount < 7_200_000) {
            overTime
        } else {
            routeCount.toLong() * 7_200_000L
        }
    }
    private fun getOvertime(totalWorkTime: Long, personalNormaHoursInLong: Int) =
        if (totalWorkTime > personalNormaHoursInLong) totalWorkTime - personalNormaHoursInLong else 0L

    private fun getHolidayTime(
        routeList: List<Route>,
        currentMonthOfYear: MonthOfYear
    ) = routeList.getWorkingTimeOnAHoliday(currentMonthOfYear)

    private fun getPersonalNormaInLong(settings: UserSettings) =
        settings.selectMonthOfYear.getPersonalNormaHours() * 3_600_000

    private fun getSingleLocomotiveTime(routeList: List<Route>): Long {
        var singleLocoTimeFollowing = 0L
        routeList.forEach { route ->
            route.trains.forEach { train ->
                singleLocoTimeFollowing += train.timeFollowingSingleLocomotive()
            }
        }
        return singleLocoTimeFollowing
    }

    private fun getPassengerTime(
        routeList: List<Route>,
        currentMonthOfYear: MonthOfYear
    ) = routeList.getPassengerTime(currentMonthOfYear)

    private fun getTotalWorkTime(
        routeList: List<Route>,
        currentMonthOfYear: MonthOfYear
    ) = routeList.setWorkTime(currentMonthOfYear)

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
//            val paymentNightTimePercent = 40.0
//            val paymentNightTimeMoney =
//                paymentNightTimeHours?.times(setting.tariffRate * (paymentNightTimePercent / 100))
            val surchargeQualificationClassPercent = setting.surchargeQualificationClass
            val surchargeQualificationClassMoney =
                (totalWorkTime - paymentAtPassengerHours)?.times(setting.tariffRate * (surchargeQualificationClassPercent / 100))
            val surchargeHeavyLongDistanceTrainsHour =
                uiState.value.surchargeHeavyLongDistanceTrainsHour
//            val surchargeHeavyLongDistanceTrainsPercent = setting.surchargeHeavyLongDistanceTrains
//            val surchargeHeavyLongDistanceTrainsMoney =
//                surchargeHeavyLongDistanceTrainsHour?.times(setting.tariffRate * (surchargeHeavyLongDistanceTrainsPercent / 100))
            val surchargeExtendedServicePhase =
                uiState.value.surchargeExtendedServicePhaseMoney.sum()
            val totalChargedMoney =
                paymentAtTariffMoney + paymentAtPassengerMoney + paymentAtSingleLocomotiveMoney + paymentAtOvertimeMoney +
                        surchargeAtOvertime05Money + surchargeAtOvertimeMoney + paymentHolidayMoney + surchargeHolidayMoney +
                        zonalSurchargeMoney + paymentNightTimeMoney + surchargeQualificationClassMoney + surchargeHeavyLongDistanceTrainsMoney +
                        surchargeExtendedServicePhase


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
//                    paymentNightTimePercent = paymentNightTimePercent,
//                    paymentNightTimeMoney = paymentNightTimeMoney,
                    surchargeQualificationClassPercent = surchargeQualificationClassPercent,
                    surchargeQualificationClassMoney = surchargeQualificationClassMoney,
//                    surchargeHeavyLongDistanceTrainsPercent = surchargeHeavyLongDistanceTrainsPercent,
//                    surchargeHeavyLongDistanceTrainsMoney = surchargeHeavyLongDistanceTrainsMoney,
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