package com.z_company.route.viewmodel

import android.util.Log
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
import com.z_company.domain.entities.route.UtilsForEntities.getOnePersonOperationTime
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
import com.z_company.domain.util.str2decimalSign
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar
import com.z_company.domain.util.sum
import com.z_company.domain.util.toDoubleOrZero

// TODO
// северные, районные, одно лицо, по весу разные доплаты, по удлинненым отдельная доплата, переотдых, за вредность
class SalaryCalculationViewModel : ViewModel(), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val salarySettingUseCase: SalarySettingUseCase by inject()

    private var userSettings: UserSettings? = null
    private var salarySetting: SalarySetting? = null

    private val _uiState = MutableStateFlow(SalaryCalculationUIState())
    val uiState = _uiState.asStateFlow()

    private fun loadSalarySetting() {
        viewModelScope.launch {
            salarySettingUseCase.getSalarySetting().collect { result ->
                if (result is ResultState.Success) {
                    salarySetting = result.data
                    loadUserSetting()
                }
            }
        }
    }

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

    private fun setWorkTime() {
        val currentTimeInMillis = Calendar.getInstance().timeInMillis
        userSettings?.let { userSettings ->
            salarySetting?.let { salarySetting ->
                viewModelScope.launch {
                    val currentMonthOfYear = userSettings.selectMonthOfYear
                    routeUseCase.listRoutesByMonth(currentMonthOfYear).collect { loadRouteState ->
                        if (loadRouteState is ResultState.Success) {
                            val routeList = if (userSettings.isConsiderFutureRoute) {
                                loadRouteState.data
                            } else {
                                loadRouteState.data.filter { it.basicData.timeStartWork!! < currentTimeInMillis }
                            }
                            setNormaHours(currentMonthOfYear)
                            setToTariffTimeData(routeList, userSettings, salarySetting)
                            setNightTimeData(routeList, userSettings, salarySetting)
                            setSingleLocomotiveData(routeList, salarySetting)
                            setPassengerData(routeList, userSettings, salarySetting)
                            setHolidayData(routeList, userSettings, salarySetting)
                            setQualificationClassSurchargeData(
                                routeList,
                                userSettings,
                                salarySetting
                            )
                            setSurchargeExtendedServicePhase(routeList)

                            setSurchargeOnePersonOperation(routeList, userSettings, salarySetting)
//                            setSurchargeHarmfulness()
//                            setSurchargeLongDistanceData(routeList, salarySetting)
//                            setSurchargeHeavyTransData()
                            setZonalSurchargeData(routeList, userSettings, salarySetting)
                            setSurchargeOvertimeData(routeList, userSettings, salarySetting)
//                            setDistrictSurchargeData()
//                            setNordicSurchargeData()

                            setTotalCharged(routeList, userSettings, salarySetting)
                        }
                    }
                }
            }
        }
    }

    private fun setTotalCharged(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting
    ) {
        val paymentAtTariffMoney =
            getMoneyAtWorkTimeAtTariff(routeList, userSettings, salarySetting)
        Log.d("ZZZ", "paymentAtTariffMoney = ${paymentAtTariffMoney.str2decimalSign()}")
        val paymentAtPassengerMoney = getMoneyAtPassenger(routeList, userSettings, salarySetting)
        Log.d("ZZZ", "paymentAtPassengerMoney = ${paymentAtPassengerMoney.str2decimalSign()}")
        val paymentAtSingleLocomotiveMoney = getMoneyAtSingleLocomotive(routeList, salarySetting)
        Log.d("ZZZ", "paymentAtSingleLocomotiveMoney = ${paymentAtSingleLocomotiveMoney.str2decimalSign()}")
        val paymentHolidayMoney = getMoneyAtHoliday(routeList, userSettings, salarySetting)
        Log.d("ZZZ", "paymentHolidayMoney = ${paymentHolidayMoney.str2decimalSign()}")
        val surchargeHolidayMoney = getMoneyAtHoliday(routeList, userSettings, salarySetting)
        Log.d("ZZZ", "surchargeHolidayMoney = ${surchargeHolidayMoney.str2decimalSign()}")
        val zonalSurchargeMoney = getMoneyAtZonalSurcharge(routeList, userSettings, salarySetting)
        Log.d("ZZZ", "zonalSurchargeMoney = ${zonalSurchargeMoney.str2decimalSign()}")
        val paymentNightTimeMoney = getMoneyAtNightTime(routeList, userSettings, salarySetting)
        Log.d("ZZZ", "paymentNightTimeMoney = ${paymentNightTimeMoney.str2decimalSign()}")
        val surchargeQualificationClassMoney =
            getMoneyAtQualificationClass(routeList, userSettings, salarySetting)
        Log.d("ZZZ", "surchargeQualificationClassMoney = ${surchargeQualificationClassMoney.str2decimalSign()}")
        val surchargeExtendedServicePhaseMoney =
            getMoneyListSurchargeExtendedServicePhase(routeList, salarySetting).sum()
        Log.d("ZZZ", "surchargeExtendedServicePhaseMoney = ${surchargeExtendedServicePhaseMoney.str2decimalSign()}")

        val paymentAtOvertimeMoney =
            getMoneyAtPaymentOvertime(routeList, userSettings, salarySetting)
        Log.d("ZZZ", "paymentAtOvertimeMoney = ${paymentAtOvertimeMoney.str2decimalSign()}")
        val surchargeAtOvertime05Money =
            getMoneyAtSurchargeOvertime05(routeList, userSettings, salarySetting)
        Log.d("ZZZ", "surchargeAtOvertime05Money = ${surchargeAtOvertime05Money.str2decimalSign()}")
        val surchargeAtOvertimeMoney =
            getMoneyAtSurchargeOvertime(routeList, userSettings, salarySetting)
        Log.d("ZZZ", "surchargeAtOvertimeMoney = ${surchargeAtOvertimeMoney.str2decimalSign()}")

        val surchargeOnePersonOperationMoney = getMoneyAtOnePersonOperation(routeList, userSettings, salarySetting)
        Log.d("ZZZ", "surchargeOnePersonOperationMoney = ${surchargeOnePersonOperationMoney.str2decimalSign()}")
        // TODO new surcharge

        val totalChargedMoney =
            paymentAtTariffMoney + paymentAtPassengerMoney + paymentAtSingleLocomotiveMoney + paymentAtOvertimeMoney +
                    surchargeAtOvertime05Money + surchargeAtOvertimeMoney + paymentHolidayMoney + surchargeHolidayMoney +
                    zonalSurchargeMoney + paymentNightTimeMoney + surchargeQualificationClassMoney +
                    surchargeExtendedServicePhaseMoney +
                    surchargeOnePersonOperationMoney

        _uiState.update {
            it.copy(
                totalChargedMoney = totalChargedMoney
            )
        }
    }

    private fun setSurchargeOnePersonOperation(routeList: List<Route>, userSettings: UserSettings, salarySetting: SalarySetting) {
        val onePersonOperationPercent = salarySetting.onePersonOperationPercent
        val onePersonOperationMoney = getMoneyAtOnePersonOperation(routeList, userSettings, salarySetting)
        _uiState.update {
            it.copy(
                onePersonOperationPercent = onePersonOperationPercent,
                onePersonOperationMoney = onePersonOperationMoney
            )
        }
    }

    private fun getMoneyAtOnePersonOperation(routeList: List<Route>, userSettings: UserSettings, salarySetting: SalarySetting): Double {
        val currentMonthOfYear = userSettings.selectMonthOfYear
        val onePersonOperationTime = routeList.getOnePersonOperationTime(currentMonthOfYear)
        return onePersonOperationTime.times(salarySetting.tariffRate * (salarySetting.onePersonOperationPercent / 100))
    }


    private fun getBasicTimeForCalculationSurcharge(
        routeList: List<Route>,
        userSettings: UserSettings
    ): Long {
        val currentMonthOfYear = userSettings.selectMonthOfYear
        val totalWorkTime = getTotalWorkTime(routeList, currentMonthOfYear)
        val passengerTime = getPassengerTime(routeList, currentMonthOfYear)

        return totalWorkTime - passengerTime
    }

    private fun setQualificationClassSurchargeData(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting
    ) {
        val surchargeQualificationClassPercent = salarySetting.surchargeQualificationClass
        val surchargeQualificationClassMoney =
            getMoneyAtQualificationClass(routeList, userSettings, salarySetting)
        _uiState.update {
            it.copy(
                surchargeQualificationClassPercent = surchargeQualificationClassPercent,
                surchargeQualificationClassMoney = surchargeQualificationClassMoney
            )
        }
    }

    private fun getMoneyAtQualificationClass(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting
    ): Double {
        val baseForZonalSurcharge = getBasicTimeForCalculationSurcharge(routeList, userSettings)
        val surchargeQualificationClassPercent = salarySetting.surchargeQualificationClass
        return baseForZonalSurcharge.times(salarySetting.tariffRate * (surchargeQualificationClassPercent / 100))
    }

    private fun setZonalSurchargeData(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting
    ) {
        val zonalSurchargePercent = salarySetting.zonalSurcharge
        val zonalSurchargeMoney = getMoneyAtZonalSurcharge(routeList, userSettings, salarySetting)

        _uiState.update {
            it.copy(
                zonalSurchargePercent = zonalSurchargePercent,
                zonalSurchargeMoney = zonalSurchargeMoney,
            )
        }
    }

    private fun getMoneyAtZonalSurcharge(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting
    ): Double {
        val zonalSurchargePercent = salarySetting.zonalSurcharge
        val baseForZonalSurcharge = getBasicTimeForCalculationSurcharge(routeList, userSettings)
        return baseForZonalSurcharge.times(salarySetting.tariffRate * (zonalSurchargePercent / 100))
    }

    private fun setToTariffTimeData(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting
    ) {
        val currentMonthOfYear = userSettings.selectMonthOfYear
        val totalWorkTime = getTotalWorkTime(routeList, currentMonthOfYear)
        val workTimeAtTariff = getWorkTimeAtTariff(routeList, userSettings)
        val paymentAtTariffMoney =
            getMoneyAtWorkTimeAtTariff(routeList, userSettings, salarySetting)

        _uiState.update {
            it.copy(
                tariffRate = salarySetting.tariffRate,
                totalWorkTime = totalWorkTime,
                paymentAtTariffHours = workTimeAtTariff,
                paymentAtTariffMoney = paymentAtTariffMoney
            )
        }
    }

    private fun getMoneyAtWorkTimeAtTariff(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting
    ): Double {
        val workTimeAtTariff = getWorkTimeAtTariff(routeList, userSettings)
        return workTimeAtTariff.times(salarySetting.tariffRate)
    }

    private fun getWorkTimeAtTariff(
        routeList: List<Route>,
        userSettings: UserSettings
    ): Long {
        val currentMonthOfYear = userSettings.selectMonthOfYear
        val personalNormaHoursInLong = getPersonalNormaInLong(userSettings)

        val totalWorkTime = getTotalWorkTime(routeList, currentMonthOfYear)
        val passengerTime = getPassengerTime(routeList, currentMonthOfYear)
        val singleLocoTime = getSingleLocomotiveTime(routeList)
        val paymentHolidayHours = getHolidayTime(routeList, currentMonthOfYear)
        val overtime = getOvertime(totalWorkTime, personalNormaHoursInLong)

        return totalWorkTime - passengerTime - singleLocoTime - paymentHolidayHours - overtime
    }

    private fun setSingleLocomotiveData(
        routeList: List<Route>,
        salarySetting: SalarySetting
    ) {
        val singleLocoTime = getSingleLocomotiveTime(routeList)
        val paymentAtSingleLocomotiveMoney = getMoneyAtSingleLocomotive(routeList, salarySetting)

        _uiState.update {
            it.copy(
                paymentAtSingleLocomotiveHours = singleLocoTime,
                paymentAtSingleLocomotiveMoney = paymentAtSingleLocomotiveMoney
            )
        }
    }

    private fun getMoneyAtSingleLocomotive(
        routeList: List<Route>,
        salarySetting: SalarySetting
    ): Double {
        val singleLocoTime = getSingleLocomotiveTime(routeList)
        return singleLocoTime.times(salarySetting.tariffRate)
    }

    private fun getSingleLocomotiveTime(routeList: List<Route>): Long {
        var singleLocoTimeFollowing = 0L
        routeList.forEach { route ->
            route.trains.forEach { train ->
                singleLocoTimeFollowing += train.timeFollowingSingleLocomotive()
            }
        }
        return singleLocoTimeFollowing
    }

    private fun setPassengerData(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting
    ) {
        val currentMonthOfYear = userSettings.selectMonthOfYear
        val passengerTime = getPassengerTime(routeList, currentMonthOfYear)
        val paymentAtPassengerMoney = getMoneyAtPassenger(routeList, userSettings, salarySetting)

        _uiState.update {
            it.copy(
                paymentAtPassengerHours = passengerTime,
                paymentAtPassengerMoney = paymentAtPassengerMoney,
            )
        }
    }

    private fun getPassengerTime(
        routeList: List<Route>,
        currentMonthOfYear: MonthOfYear
    ) = routeList.getPassengerTime(currentMonthOfYear)

    private fun getMoneyAtPassenger(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting
    ): Double {

        val currentMonthOfYear = userSettings.selectMonthOfYear
        val passengerTime = getPassengerTime(routeList, currentMonthOfYear)
        return passengerTime.times(salarySetting.tariffRate)
    }

    private fun setHolidayData(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting
    ) {
        val currentMonthOfYear = userSettings.selectMonthOfYear
        val paymentHolidayHours = getHolidayTime(routeList, currentMonthOfYear)

        val paymentHolidayMoney = getMoneyAtHoliday(
            routeList, userSettings, salarySetting
        )

        _uiState.update {
            it.copy(
                paymentHolidayHours = paymentHolidayHours,
                surchargeHolidayHours = paymentHolidayHours,
                paymentHolidayMoney = paymentHolidayMoney,
                surchargeHolidayMoney = paymentHolidayMoney,
            )
        }
    }

    private fun getHolidayTime(
        routeList: List<Route>,
        currentMonthOfYear: MonthOfYear
    ) = routeList.getWorkingTimeOnAHoliday(currentMonthOfYear)

    private fun getMoneyAtHoliday(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting
    ): Double {
        val currentMonthOfYear = userSettings.selectMonthOfYear
        val paymentHolidayHours = getHolidayTime(routeList, currentMonthOfYear)

        return paymentHolidayHours.times(salarySetting.tariffRate)
    }

    private fun setSurchargeOvertimeData(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting
    ) {
        val currentMonthOfYear = userSettings.selectMonthOfYear
        val totalWorkTime = getTotalWorkTime(routeList, currentMonthOfYear)
        val personalNormaHoursInLong = getPersonalNormaInLong(userSettings)
        val overtimeHours = getOvertime(totalWorkTime, personalNormaHoursInLong)
        val overtimeMoney = getMoneyAtPaymentOvertime(routeList, userSettings, salarySetting)
        val surchargeAtOvertime05Hour = getOvertime05Surcharge(routeList, overtimeHours)
        val surchargeAtOvertimeHour = getOvertimeSurcharge(overtimeHours, surchargeAtOvertime05Hour)

        val surchargeAtOvertime05Money =
            surchargeAtOvertime05Hour.times(salarySetting.tariffRate * 0.5)
        val surchargeAtOvertimeMoney =
            getMoneyAtSurchargeOvertime(routeList, userSettings, salarySetting)

        _uiState.update {
            it.copy(
                paymentAtOvertimeHours = overtimeHours,
                paymentAtOvertimeMoney = overtimeMoney,
                surchargeAtOvertime05Hours = surchargeAtOvertime05Hour,
                surchargeAtOvertimeHours = surchargeAtOvertimeHour,
                surchargeAtOvertime05Money = surchargeAtOvertime05Money,
                surchargeAtOvertimeMoney = surchargeAtOvertimeMoney
            )
        }
    }

    private fun setNordicSurchargeData(){
        _uiState.update {
            it.copy(

            )
        }
    }

    private fun getMoneyAtPaymentOvertime(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting
    ): Double {
        val baseMoneyForOvertime =
            getBasicMoneyForOvertimeCalculation(routeList, userSettings, salarySetting)
        val currentMonthOfYear = userSettings.selectMonthOfYear
        val totalWorkTime = getTotalWorkTime(routeList, currentMonthOfYear)
        val personalNormaHoursInLong = getPersonalNormaInLong(userSettings)
        val overTime = getOvertime(totalWorkTime, personalNormaHoursInLong)
        val overtimeMoneyOfOneHour = baseMoneyForOvertime / totalWorkTime
        return overTime.times(overtimeMoneyOfOneHour)
    }

    private fun getMoneyAtSurchargeOvertime05(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting
    ): Double {
        val baseMoneyForOvertime =
            getBasicMoneyForOvertimeCalculation(routeList, userSettings, salarySetting)
        val currentMonthOfYear = userSettings.selectMonthOfYear
        val totalWorkTime = getTotalWorkTime(routeList, currentMonthOfYear)
        val personalNormaHoursInLong = getPersonalNormaInLong(userSettings)
        val overTime = getOvertime(totalWorkTime, personalNormaHoursInLong)
        val surchargeAtOvertime05Hour = getOvertime05Surcharge(routeList, overTime)
        val overtimeMoneyOfOneHour = (baseMoneyForOvertime / totalWorkTime) / 2
        return surchargeAtOvertime05Hour.times(overtimeMoneyOfOneHour)
    }

    private fun getMoneyAtSurchargeOvertime(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting
    ): Double {
        val baseMoneyForOvertime =
            getBasicMoneyForOvertimeCalculation(routeList, userSettings, salarySetting)
        val currentMonthOfYear = userSettings.selectMonthOfYear
        val totalWorkTime = getTotalWorkTime(routeList, currentMonthOfYear)
        val personalNormaHoursInLong = getPersonalNormaInLong(userSettings)
        val overTime = getOvertime(totalWorkTime, personalNormaHoursInLong)
        val surchargeAtOvertime05Hour = getOvertime05Surcharge(routeList, overTime)
        val surchargeAtOvertimeHour = getOvertimeSurcharge(overTime, surchargeAtOvertime05Hour)
        val overtimeMoneyOfOneHour = baseMoneyForOvertime / totalWorkTime
        return surchargeAtOvertimeHour.times(overtimeMoneyOfOneHour)
    }

    private fun getBasicMoneyForOvertimeCalculation(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting
    ): Double {
        val paymentAtTariffMoney =
            getMoneyAtWorkTimeAtTariff(routeList, userSettings, salarySetting)
        val paymentAtPassengerMoney = getMoneyAtPassenger(routeList, userSettings, salarySetting)
        val paymentAtSingleLocomotiveMoney = getMoneyAtSingleLocomotive(routeList, salarySetting)
        val zonalSurchargeMoney = getMoneyAtZonalSurcharge(routeList, userSettings, salarySetting)
        val paymentNightTimeMoney = getMoneyAtNightTime(routeList, userSettings, salarySetting)
        val surchargeQualificationClassMoney =
            getMoneyAtQualificationClass(routeList, userSettings, salarySetting)
//        val surchargeHeavyLongDistanceTrainsMoney =
//            getMoneyAtHeavyLongDistance(routeList, salarySetting)
        val surchargeExtendedServicePhaseMoney =
            getMoneyListSurchargeExtendedServicePhase(routeList, salarySetting).sum()

        return paymentAtTariffMoney + paymentAtPassengerMoney +
                paymentAtSingleLocomotiveMoney + zonalSurchargeMoney +
                paymentNightTimeMoney + surchargeQualificationClassMoney +
//                surchargeHeavyLongDistanceTrainsMoney +
                surchargeExtendedServicePhaseMoney
    }

    private fun setNormaHours(currentMonthOfYear: MonthOfYear) {
        val normaHours = currentMonthOfYear.getStandardNormaHours()
        _uiState.update {
            it.copy(
                month = currentMonthOfYear.month.getMonthFullText(),
                normaHours = normaHours,
            )
        }
    }

//    private fun setSurchargeHeavyLongDistanceData(
//        routeList: List<Route>,
//        salarySetting: SalarySetting
//    ) {
//        val totalTimeHeavyLongDistance = getHeavyLongDistanceTime(routeList)
//        val surchargeHeavyLongDistanceTrainsPercent = salarySetting.surchargeHeavyLongDistanceTrains
//        val surchargeHeavyLongDistanceTrainsMoney =
//            getMoneyAtHeavyLongDistance(routeList, salarySetting)
//        _uiState.update {
//            it.copy(
//                surchargeHeavyLongDistanceTrainsHour = totalTimeHeavyLongDistance,
//                surchargeHeavyLongDistanceTrainsPercent = surchargeHeavyLongDistanceTrainsPercent,
//                surchargeHeavyLongDistanceTrainsMoney = surchargeHeavyLongDistanceTrainsMoney,
//            )
//        }
//    }

//    private fun getMoneyAtHeavyLongDistance(
//        routeList: List<Route>,
//        salarySetting: SalarySetting
//    ): Double {
//        val totalTimeHeavyLongDistance = getHeavyLongDistanceTime(routeList)
//        val surchargeHeavyLongDistanceTrainsPercent = salarySetting.surchargeHeavyLongDistanceTrains
//        return totalTimeHeavyLongDistance.times(salarySetting.tariffRate * (surchargeHeavyLongDistanceTrainsPercent / 100))
//
//    }

    private fun setNightTimeData(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting
    ) {
        val nightTimeHours = getNightTime(routeList, userSettings)
        val paymentNightTimePercent = 40.0
        val paymentNightTimeMoney = getMoneyAtNightTime(routeList, userSettings, salarySetting)
        _uiState.update {
            it.copy(
                paymentNightTimeHours = nightTimeHours,
                paymentNightTimePercent = paymentNightTimePercent,
                paymentNightTimeMoney = paymentNightTimeMoney,
            )
        }
    }

    private fun getMoneyAtNightTime(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting
    ): Double {
        val nightTimeHours = getNightTime(routeList, userSettings)
        val paymentNightTimePercent = 40.0
        return nightTimeHours.times(salarySetting.tariffRate * (paymentNightTimePercent / 100))
    }

    private fun getNightTime(
        routeList: List<Route>,
        settings: UserSettings
    ) = routeList.getNightTime(settings)

    private fun setSurchargeExtendedServicePhase(routeList: List<Route>) {
        salarySetting?.let { salarySetting ->
            val phaseList =
                salarySetting.surchargeExtendedServicePhaseList.sortedBy {
                    it.distance
                }
            val timeList: MutableList<Long> =
                getTimeSurchargeExtendedServicePhase(routeList, salarySetting)
            val percentList = phaseList.map {
                it.percentSurcharge
            }
            val moneyList: MutableList<Double> =
                getMoneyListSurchargeExtendedServicePhase(routeList, salarySetting)
            _uiState.update {
                it.copy(
                    surchargeExtendedServicePhaseHour = timeList,
                    surchargeExtendedServicePhasePercent = percentList,
                    surchargeExtendedServicePhaseMoney = moneyList
                )
            }
        }
    }

    private fun getTimeSurchargeExtendedServicePhase(
        routeList: List<Route>,
        salarySetting: SalarySetting
    ): MutableList<Long> {
        val phaseList =
            salarySetting.surchargeExtendedServicePhaseList.sortedBy {
                it.distance
            }

        val timeList: MutableList<Long> = mutableListOf()
        phaseList.forEachIndexed { index, _ ->
            var totalTimeInServicePhase = 0L
            routeList.forEach { route ->
                totalTimeInServicePhase += route.getTimeInServicePhase(
                    phaseList.map { it.distance.toIntOrNull() ?: 0 },
                    index
                )
            }
            timeList.add(totalTimeInServicePhase)
        }
        return timeList
    }


    private fun getMoneyListSurchargeExtendedServicePhase(
        routeList: List<Route>,
        salarySetting: SalarySetting
    ): MutableList<Double> {
        val phaseList =
            salarySetting.surchargeExtendedServicePhaseList.sortedBy {
                it.distance
            }
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
            moneyList.add(totalTimeInServicePhase.times(salarySetting.tariffRate * (percentList[index].toDoubleOrZero() / 100)))
        }
        return moneyList
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


    private fun getOvertimeSurcharge(overTime: Long, surchargeAtOvertime05Hour: Long) =
        if (overTime > surchargeAtOvertime05Hour) overTime - surchargeAtOvertime05Hour else 0L

    private fun getOvertime05Surcharge(routeList: List<Route>, overTime: Long): Long {
        val routeCount = routeList.size
        val twoHourInMillis = 7_200_000L
        return if (routeCount != 0 && overTime / routeCount < twoHourInMillis) {
            overTime
        } else {
            routeCount.toLong() * twoHourInMillis
        }
    }

    private fun getOvertime(totalWorkTime: Long, personalNormaHoursInLong: Int) =
        if (totalWorkTime > personalNormaHoursInLong) totalWorkTime - personalNormaHoursInLong else 0L

    private fun getPersonalNormaInLong(settings: UserSettings) =
        settings.selectMonthOfYear.getPersonalNormaHours() * 3_600_000

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

//            val paymentAtTariffMoney = paymentAtTariffHours?.times(setting.tariffRate)
//            val paymentAtPassengerMoney = paymentAtPassengerHours?.times(setting.tariffRate)
//            val paymentAtSingleLocomotiveMoney =
//                paymentAtSingleLocomotiveHours?.times(setting.tariffRate)
//            val paymentAtOvertimeMoney = overtimeHour?.times(setting.tariffRate)
//            val surchargeAtOvertime05Money =
//                surchargeAtOvertime05Hours?.times(setting.tariffRate * 0.5)
//            val surchargeAtOvertimeMoney = surchargeAtOvertimeHours?.times(setting.tariffRate)
//            val paymentHolidayMoney = paymentHolidayHours?.times(setting.tariffRate)
//            val surchargeHolidayMoney = paymentHolidayMoney
//            val zonalSurchargePercent = setting.zonalSurcharge
//            val zonalSurchargeMoney =
//                (totalWorkTime - paymentAtPassengerHours)?.times(setting.tariffRate * (zonalSurchargePercent / 100))
//            val paymentNightTimePercent = 40.0
//            val paymentNightTimeMoney =
//                paymentNightTimeHours?.times(setting.tariffRate * (paymentNightTimePercent / 100))
//            val surchargeQualificationClassPercent = setting.surchargeQualificationClass
//            val surchargeQualificationClassMoney =
//                (totalWorkTime - paymentAtPassengerHours)?.times(setting.tariffRate * (surchargeQualificationClassPercent / 100))
//            val surchargeHeavyLongDistanceTrainsHour =
//                uiState.value.surchargeHeavyLongDistanceTrainsHour
//            val surchargeHeavyLongDistanceTrainsPercent = setting.surchargeHeavyLongDistanceTrains
//            val surchargeHeavyLongDistanceTrainsMoney =
//                surchargeHeavyLongDistanceTrainsHour?.times(setting.tariffRate * (surchargeHeavyLongDistanceTrainsPercent / 100))
//            val surchargeExtendedServicePhase =
//                uiState.value.surchargeExtendedServicePhaseMoney.sum()
//            val totalChargedMoney =
//                paymentAtTariffMoney + paymentAtPassengerMoney + paymentAtSingleLocomotiveMoney + paymentAtOvertimeMoney +
//                        surchargeAtOvertime05Money + surchargeAtOvertimeMoney + paymentHolidayMoney + surchargeHolidayMoney +
//                        zonalSurchargeMoney + paymentNightTimeMoney + surchargeQualificationClassMoney + surchargeHeavyLongDistanceTrainsMoney +
//                        surchargeExtendedServicePhase


//            val retentionNdfl = totalChargedMoney?.times(0.13)
//            val otherRetentionPercent = setting.otherRetention
//            val otherRetention = totalChargedMoney?.times(otherRetentionPercent / 100)
//            val unionistsRetention = totalChargedMoney?.times(setting.unionistsRetention / 100)
//            val totalRetention = retentionNdfl + unionistsRetention + otherRetention

//            val toBeCredited = totalChargedMoney - totalRetention
//            val tariffRate = setting.tariffRate

            _uiState.update {
                it.copy(
//                    paymentAtTariffMoney = paymentAtTariffMoney,
//                    paymentAtPassengerMoney = paymentAtPassengerMoney,
//                    paymentAtSingleLocomotiveMoney = paymentAtSingleLocomotiveMoney,
//                    paymentAtOvertimeMoney = paymentAtOvertimeMoney,
//                    surchargeAtOvertime05Money = surchargeAtOvertime05Money,
//                    surchargeAtOvertimeMoney = surchargeAtOvertimeMoney,
//                    paymentHolidayMoney = paymentHolidayMoney,
//                    surchargeHolidayMoney = surchargeHolidayMoney,
//                    zonalSurchargePercent = zonalSurchargePercent,
//                    zonalSurchargeMoney = zonalSurchargeMoney,
//                    paymentNightTimePercent = paymentNightTimePercent,
//                    paymentNightTimeMoney = paymentNightTimeMoney,
//                    surchargeQualificationClassPercent = surchargeQualificationClassPercent,
//                    surchargeQualificationClassMoney = surchargeQualificationClassMoney,
//                    surchargeHeavyLongDistanceTrainsPercent = surchargeHeavyLongDistanceTrainsPercent,
//                    surchargeHeavyLongDistanceTrainsMoney = surchargeHeavyLongDistanceTrainsMoney,
//                    totalChargedMoney = totalChargedMoney,
//                    retentionNdfl = retentionNdfl,
//                    otherRetention = otherRetention,
//                    unionistsRetention = unionistsRetention,
//                    totalRetention = totalRetention,
//                    toBeCredited = toBeCredited,
//                    tariffRate = tariffRate
                )
            }
        }
    }

    fun loadData() {
        loadSalarySetting()
    }
}