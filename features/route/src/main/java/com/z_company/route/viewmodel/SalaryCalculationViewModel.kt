package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.core.util.DateAndTimeConverter.getMonthFullText
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.SalarySetting
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.entities.UtilForMonthOfYear.getDayoffHours
import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHours
import com.z_company.domain.entities.UtilForMonthOfYear.getStandardNormaHours
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.getLongDistanceTime
import com.z_company.domain.entities.route.UtilsForEntities.getNightTime
import com.z_company.domain.entities.route.UtilsForEntities.getOnePersonOperationTime
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getTimeInHeavyTrain
import com.z_company.domain.entities.route.UtilsForEntities.getTimeInServicePhase
import com.z_company.domain.entities.route.UtilsForEntities.setWorkTime
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
import com.z_company.domain.util.sum
import com.z_company.domain.util.toDoubleOrZero
import com.z_company.domain.util.toIntOrZero

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

                            setSurchargeOnePersonOperationData(
                                routeList,
                                userSettings,
                                salarySetting
                            )
                            setSurchargeHarmfulnessData(routeList, userSettings, salarySetting)
                            setSurchargeLongDistanceData(routeList, salarySetting)
                            setSurchargeHeavyTransData(routeList, salarySetting)
                            setZonalSurchargeData(routeList, userSettings, salarySetting)
                            setSurchargeOvertimeData(routeList, userSettings, salarySetting)
                            setDistrictSurchargeData(routeList, userSettings, salarySetting)
                            setNordicSurchargeData(routeList, userSettings, salarySetting)
                            setAveragePaymentData(userSettings, salarySetting)
                            setTotalCharged(routeList, userSettings, salarySetting)
                            setRetentionData(routeList, userSettings, salarySetting)
                        }
                    }
                }
            }
        }
    }

    private fun setRetentionData(routeList: List<Route>, userSettings: UserSettings, salarySetting: SalarySetting) {
        val retentionNDFL = getRetentionNDFLMoney(routeList, userSettings, salarySetting)
        val unionistsRetention = getUnionistsRetentionMoney(routeList, userSettings, salarySetting)
        val otherRetention = getOtherRetentionMoney(routeList, userSettings, salarySetting)
        val totalRetention = getTotalRetention(routeList, userSettings, salarySetting)
        val toBeCredited = getToBeCredited(routeList, userSettings, salarySetting)

        _uiState.update {
            it.copy(
                retentionNdfl = retentionNDFL,
                unionistsRetention = unionistsRetention,
                otherRetention = otherRetention,
                totalRetention = totalRetention,
                toBeCredited = toBeCredited,
            )
        }
    }

    private fun getToBeCredited(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting
    ): Double {
        val totalCharged = getTotalCharged(routeList, userSettings, salarySetting)
        val totalRetention = getTotalRetention(routeList, userSettings, salarySetting)
        return totalCharged - totalRetention
    }

    private fun getTotalRetention(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting
    ): Double {
        val retentionNDFL = getRetentionNDFLMoney(routeList, userSettings, salarySetting)
        val unionistsRetention = getUnionistsRetentionMoney(routeList, userSettings, salarySetting)
        val otherRetention = getOtherRetentionMoney(routeList, userSettings, salarySetting)
        return retentionNDFL + unionistsRetention + otherRetention
    }
    private fun getOtherRetentionMoney(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting
    ):Double {
        val otherRetentionPercent = salarySetting.otherRetention
        val baseForCalculation = getTotalCharged(routeList, userSettings, salarySetting)
        return baseForCalculation.times(otherRetentionPercent / 100)
    }

    private fun getUnionistsRetentionMoney(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting
    ):Double {
        val unionistsRetentionPercent = salarySetting.unionistsRetention
        val baseForCalculation = getTotalCharged(routeList, userSettings, salarySetting)
        return baseForCalculation.times(unionistsRetentionPercent / 100)
    }

    private fun getRetentionNDFLMoney(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting
    ): Double {
        val percentNDFL = salarySetting.ndfl
        val baseForCalculation = getTotalCharged(routeList, userSettings, salarySetting)
        return baseForCalculation.times(percentNDFL / 100)
    }

    private fun setTotalCharged(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting
    ) {
        val totalChargedMoney = getTotalCharged(routeList, userSettings, salarySetting)
        _uiState.update {
            it.copy(
                totalChargedMoney = totalChargedMoney
            )
        }
    }

    private fun getTotalCharged(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting
    ): Double {
        val baseMoney = getBaseMoney(routeList, userSettings, salarySetting)
        val averageMoney = getAverageMoney(userSettings, salarySetting)
        val nordicSurcharge = getMoneyNordicSurcharge(routeList, salarySetting, userSettings)
        val districtSurcharge = getMoneyDistrictSurcharge(routeList, salarySetting, userSettings)
        return baseMoney + averageMoney + nordicSurcharge + districtSurcharge
    }

    private fun getBaseMoney(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting
    ): Double {
        val paymentAtTariffMoney =
            getMoneyAtWorkTimeAtTariff(routeList, userSettings, salarySetting)
        val paymentAtPassengerMoney = getMoneyAtPassenger(routeList, userSettings, salarySetting)
        val paymentAtSingleLocomotiveMoney = getMoneyAtSingleLocomotive(routeList, salarySetting)
        val paymentHolidayMoney = getMoneyAtHoliday(routeList, userSettings, salarySetting)
        val surchargeHolidayMoney = getMoneyAtHoliday(routeList, userSettings, salarySetting)
        val zonalSurchargeMoney = getMoneyAtZonalSurcharge(routeList, userSettings, salarySetting)
        val paymentNightTimeMoney = getMoneyAtNightTime(routeList, userSettings, salarySetting)
        val surchargeQualificationClassMoney =
            getMoneyAtQualificationClass(routeList, userSettings, salarySetting)
        val surchargeExtendedServicePhaseMoney =
            getMoneyListSurchargeExtendedServicePhase(routeList, salarySetting).sum()

        val paymentAtOvertimeMoney =
            getMoneyAtPaymentOvertime(routeList, userSettings, salarySetting)

        val surchargeAtOvertime05Money =
            getMoneyAtSurchargeOvertime05(routeList, userSettings, salarySetting)

        val surchargeAtOvertimeMoney =
            getMoneyAtSurchargeOvertime(routeList, userSettings, salarySetting)

        val surchargeOnePersonOperationMoney =
            getMoneyAtOnePersonOperation(routeList, userSettings, salarySetting)

        val surchargeHarmfulnessSurchargeMoney =
            getMoneyAtHarmfulness(routeList, userSettings, salarySetting)

        val surchargeLongDistanceTrainsMoney = getMoneyAtLongDistanceTrain(routeList, salarySetting)

        val surchargeHeavyTrains = getMoneyListSurchargeHeavyTrains(routeList, salarySetting).sum()

        return paymentAtTariffMoney + paymentAtPassengerMoney +
                paymentAtSingleLocomotiveMoney + paymentAtOvertimeMoney +
                surchargeAtOvertime05Money + surchargeAtOvertimeMoney +
                paymentHolidayMoney + surchargeHolidayMoney +
                zonalSurchargeMoney + paymentNightTimeMoney +
                surchargeQualificationClassMoney + surchargeExtendedServicePhaseMoney +
                surchargeOnePersonOperationMoney + surchargeHarmfulnessSurchargeMoney +
                surchargeLongDistanceTrainsMoney + surchargeHeavyTrains
    }

    private fun setAveragePaymentData(userSettings: UserSettings, salarySetting: SalarySetting) {
        val currentMonthOfYear = userSettings.selectMonthOfYear
        val dayoffHours = currentMonthOfYear.getDayoffHours()
        val averagePaymentMoney = getAverageMoney(userSettings, salarySetting)
        _uiState.update {
            it.copy(
                averagePaymentHours = dayoffHours,
                averagePaymentMoney = averagePaymentMoney
            )
        }
    }

    private fun getAverageMoney(userSettings: UserSettings, salarySetting: SalarySetting): Double {
        val currentMonthOfYear = userSettings.selectMonthOfYear
        val dayoffHours = currentMonthOfYear.getDayoffHours()
        val averagePaymentHour = salarySetting.averagePaymentHour
        return averagePaymentHour.times(dayoffHours.toDouble())
    }

    private fun setSurchargeHeavyTransData(routeList: List<Route>, salarySetting: SalarySetting) {
        val surchargeListSorted = salarySetting.surchargeHeavyTrainsList.sortedBy {
            it.weight
        }
        val timeList: MutableList<Long> = getTimeListSurchargeHeavyTrain(routeList, salarySetting)
        val percentList = surchargeListSorted.map {
            it.percentSurcharge
        }
        val moneyList: MutableList<Double> =
            getMoneyListSurchargeHeavyTrains(routeList, salarySetting)

        _uiState.update {
            it.copy(
                surchargeHeavyTransHour = timeList,
                surchargeHeavyTransPercent = percentList,
                surchargeHeavyTransMoney = moneyList
            )
        }
    }

    private fun getMoneyListSurchargeHeavyTrains(
        routeList: List<Route>,
        salarySetting: SalarySetting
    ): MutableList<Double> {

        val surchargeListSorted = salarySetting.surchargeHeavyTrainsList.sortedBy {
            it.weight
        }
        val percentList = surchargeListSorted.map {
            it.percentSurcharge
        }
        val moneyList: MutableList<Double> = mutableListOf()
        surchargeListSorted.forEachIndexed { index, _ ->
            var totalTimeHeavyTrain = 0L
            routeList.forEach { route ->
                totalTimeHeavyTrain += route.getTimeInHeavyTrain(
                    surchargeListSorted.map { it.weight.toIntOrZero() },
                    index
                )
            }
            moneyList.add(totalTimeHeavyTrain.times(salarySetting.tariffRate * (percentList[index].toDoubleOrZero() / 100)))
        }
        return moneyList

    }


    private fun getTimeListSurchargeHeavyTrain(
        routeList: List<Route>,
        salarySetting: SalarySetting
    ): MutableList<Long> {
        val surchargeListSorted = salarySetting.surchargeHeavyTrainsList.sortedBy {
            it.weight
        }
        val timeList: MutableList<Long> = mutableListOf()
        surchargeListSorted.forEachIndexed { index, _ ->
            var totalTimeHeavyTrain = 0L
            routeList.forEach { route ->
                totalTimeHeavyTrain += route.getTimeInHeavyTrain(
                    surchargeListSorted.map { it.weight.toIntOrZero() },
                    index
                )
            }
            timeList.add(totalTimeHeavyTrain)
        }
        return timeList
    }

    private fun setSurchargeLongDistanceData(routeList: List<Route>, salarySetting: SalarySetting) {
        val surchargeLongDistanceTrainsPercent = salarySetting.surchargeLongDistanceTrain
        val timeLongDistanceTrain =
            routeList.getLongDistanceTime(salarySetting.lengthLongDistanceTrain)
        val surchargeLongDistanceTrainsMoney = getMoneyAtLongDistanceTrain(routeList, salarySetting)
        _uiState.update {
            it.copy(
                surchargeLongDistanceTrainsHours = timeLongDistanceTrain,
                surchargeLongDistanceTrainsPercent = surchargeLongDistanceTrainsPercent,
                surchargeLongDistanceTrainsMoney = surchargeLongDistanceTrainsMoney
            )
        }
    }

    private fun getMoneyAtLongDistanceTrain(
        routeList: List<Route>,
        salarySetting: SalarySetting
    ): Double {
        val surchargeLongDistanceTrainsPercent = salarySetting.surchargeLongDistanceTrain
        val timeLongDistanceTrain =
            routeList.getLongDistanceTime(salarySetting.lengthLongDistanceTrain)
        return timeLongDistanceTrain.times(salarySetting.tariffRate * (surchargeLongDistanceTrainsPercent / 100))
    }

    private fun setSurchargeHarmfulnessData(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting
    ) {
        val harmfulnessSurchargePercent = salarySetting.harmfulnessPercent
        val harmfulnessSurchargeMoney =
            getMoneyAtHarmfulness(routeList, userSettings, salarySetting)
        _uiState.update {
            it.copy(
                harmfulnessSurchargePercent = harmfulnessSurchargePercent,
                harmfulnessSurchargeMoney = harmfulnessSurchargeMoney
            )
        }
    }

    private fun getMoneyAtHarmfulness(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting
    ): Double {
        val harmfulnessSurchargePercent = salarySetting.harmfulnessPercent
        val basicForCalculation = getBasicTimeForCalculationSurcharge(routeList, userSettings)
        return basicForCalculation.times(salarySetting.tariffRate * (harmfulnessSurchargePercent / 100))
    }

    private fun setSurchargeOnePersonOperationData(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting
    ) {
        val onePersonOperationPercent = salarySetting.onePersonOperationPercent
        val onePersonOperationMoney =
            getMoneyAtOnePersonOperation(routeList, userSettings, salarySetting)
        _uiState.update {
            it.copy(
                onePersonOperationPercent = onePersonOperationPercent,
                onePersonOperationMoney = onePersonOperationMoney
            )
        }
    }

    private fun getMoneyAtOnePersonOperation(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting
    ): Double {
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

    private fun setNordicSurchargeData(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting,
    ) {
        val nordicSurchargePercent = salarySetting.nordicPercent
        val nordicMoney = getMoneyNordicSurcharge(routeList, salarySetting, userSettings)
        _uiState.update {
            it.copy(
                nordicSurchargePercent = nordicSurchargePercent,
                nordicSurchargeMoney = nordicMoney
            )
        }
    }

    private fun getMoneyNordicSurcharge(
        routeList: List<Route>,
        salarySetting: SalarySetting,
        userSettings: UserSettings
    ): Double {
        val baseForCalculation = getBaseMoney(routeList, userSettings, salarySetting)
        val nordicSurchargePercent = salarySetting.nordicPercent
        return baseForCalculation.times(nordicSurchargePercent / 100)
    }

    private fun setDistrictSurchargeData(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting,
    ) {
        val districtSurchargeCoefficient = salarySetting.districtCoefficient
        val districtSurchargeMoney =
            getMoneyDistrictSurcharge(routeList, salarySetting, userSettings)
        _uiState.update {
            it.copy(
                districtSurchargeCoefficient = districtSurchargeCoefficient,
                districtSurchargeMoney = districtSurchargeMoney
            )
        }
    }

    private fun getMoneyDistrictSurcharge(
        routeList: List<Route>,
        salarySetting: SalarySetting,
        userSettings: UserSettings
    ): Double {
        val baseForCalculation = getBaseMoney(routeList, userSettings, salarySetting)
        val districtCoefficient = salarySetting.districtCoefficient
        return baseForCalculation.times(districtCoefficient / 100)
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
        val surchargeExtendedServicePhaseMoney =
            getMoneyListSurchargeExtendedServicePhase(routeList, salarySetting).sum()
        val surchargeOnePersonOperationMoney =
            getMoneyAtOnePersonOperation(routeList, userSettings, salarySetting)
        val surchargeHarmfulnessSurchargeMoney =
            getMoneyAtHarmfulness(routeList, userSettings, salarySetting)
        val surchargeLongDistanceTrainsMoney = getMoneyAtLongDistanceTrain(routeList, salarySetting)
        val surchargeHeavyTrains = getMoneyListSurchargeHeavyTrains(routeList, salarySetting).sum()
        return paymentAtTariffMoney + paymentAtPassengerMoney +
                paymentAtSingleLocomotiveMoney + zonalSurchargeMoney +
                paymentNightTimeMoney + surchargeQualificationClassMoney +
                surchargeExtendedServicePhaseMoney + surchargeOnePersonOperationMoney +
                surchargeHarmfulnessSurchargeMoney + surchargeLongDistanceTrainsMoney +
                surchargeHeavyTrains
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

    fun loadData() {
        loadSalarySetting()
    }
}