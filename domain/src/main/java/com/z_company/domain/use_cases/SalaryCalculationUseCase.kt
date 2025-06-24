package com.z_company.domain.use_cases

import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.SalarySetting
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHours
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.getLongDistanceTime
import com.z_company.domain.entities.route.UtilsForEntities.getNightTime
import com.z_company.domain.entities.route.UtilsForEntities.getOnePersonOperationTime
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getTimeInHeavyTrain
import com.z_company.domain.entities.route.UtilsForEntities.getTimeInServicePhase
import com.z_company.domain.entities.route.UtilsForEntities.getWorkingTimeOnAHoliday
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.timeFollowingSingleLocomotive
import com.z_company.domain.util.sum
import com.z_company.domain.util.toDoubleOrZero
import com.z_company.domain.util.toIntOrZero
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SalaryCalculationUseCase : KoinComponent {
    private val salarySettingUseCase: SalarySettingUseCase by inject()
    private fun getWorkTimeAtTariff(
        routeList: List<Route>,
        userSettings: UserSettings
    ): Long {
        val currentMonthOfYear = userSettings.selectMonthOfYear
        val personalNormaHoursInLong = getPersonalNormaInLong(userSettings)

        val totalWorkTime = getTotalWorkTime(routeList, userSettings, currentMonthOfYear)
        val passengerTime = getPassengerTime(routeList, userSettings, currentMonthOfYear)
        val singleLocoTime = getSingleLocomotiveTime(routeList)
        val paymentHolidayHours = getHolidayTime(routeList, userSettings, currentMonthOfYear)
        val overtime = getOvertime(totalWorkTime, personalNormaHoursInLong)

        return totalWorkTime - passengerTime - singleLocoTime - paymentHolidayHours - overtime
    }

    fun getMoneyAtSingleLocomotive(
        routeList: List<Route>,
        userSettings: UserSettings
    ): Double {
        val singleLocoTime = getSingleLocomotiveTime(routeList)
        return singleLocoTime.times(userSettings.selectMonthOfYear.tariffRate)
    }

    private fun getSingleLocomotiveTime(routeList: List<Route>): Long {
        var singleLocoTimeFollowing = 0L
        routeList.forEach { route ->
            route.trains.forEach { train ->
                singleLocoTimeFollowing += train.timeFollowingSingleLocomotive(
                    startWork = route.basicData.timeStartWork,
                    endWork = route.basicData.timeEndWork
                )
            }
        }
        return singleLocoTimeFollowing
    }

    private fun getHolidayTime(
        routeList: List<Route>,
        userSettings: UserSettings,
        currentMonthOfYear: MonthOfYear
    ) = routeList.getWorkingTimeOnAHoliday(currentMonthOfYear, userSettings.timeZone)

    fun getMoneyAtHoliday(
        routeList: List<Route>,
        userSettings: UserSettings,
    ): Double {
        val currentMonthOfYear = userSettings.selectMonthOfYear
        val paymentHolidayHours = getHolidayTime(routeList, userSettings, currentMonthOfYear)

        return paymentHolidayHours.times(userSettings.selectMonthOfYear.tariffRate)
    }

    private fun getPassengerTime(
        routeList: List<Route>,
        userSettings: UserSettings,
        currentMonthOfYear: MonthOfYear
    ) = routeList.getPassengerTime(currentMonthOfYear, userSettings.timeZone)

    fun getMoneyAtPassenger(
        routeList: List<Route>,
        userSettings: UserSettings,
    ): Double {

        val currentMonthOfYear = userSettings.selectMonthOfYear
        val passengerTime = getPassengerTime(routeList, userSettings, currentMonthOfYear)
        return passengerTime.times(userSettings.selectMonthOfYear.tariffRate)
    }

    fun getMoneyAtNightTime(
        routeList: List<Route>,
        userSettings: UserSettings,
    ): Double {
        val nightTimeHours = getNightTime(routeList, userSettings)
        val paymentNightTimePercent = 40.0
        return nightTimeHours.times(userSettings.selectMonthOfYear.tariffRate * (paymentNightTimePercent / 100))
    }

    private fun getNightTime(
        routeList: List<Route>,
        settings: UserSettings
    ) = routeList.getNightTime(settings)

    private fun getOvertime(totalWorkTime: Long, personalNormaHoursInLong: Int) =
        if (totalWorkTime > personalNormaHoursInLong) totalWorkTime - personalNormaHoursInLong else 0L

    private fun getPersonalNormaInLong(settings: UserSettings) =
        settings.selectMonthOfYear.getPersonalNormaHours() * 3_600_000

    private fun getTotalWorkTime(
        routeList: List<Route>,
        userSettings: UserSettings,
        currentMonthOfYear: MonthOfYear
    ) = routeList.getWorkTime(currentMonthOfYear, userSettings.timeZone)

    fun getMoneyAtWorkTimeAtTariff(
        routeList: List<Route>,
        userSettings: UserSettings,
    ): Double {
        val workTimeAtTariffToLong = getWorkTimeAtTariff(routeList, userSettings)
        return workTimeAtTariffToLong.times(userSettings.selectMonthOfYear.tariffRate)
    }

    fun getMoneyAtZonalSurcharge(
        routeList: List<Route>,
        userSettings: UserSettings,
    ): Double {
        val salarySetting = salarySettingUseCase.getSalarySetting()
        val zonalSurchargePercent = salarySetting.zonalSurcharge
        val baseForZonalSurcharge = getBasicTimeForCalculationSurcharge(routeList, userSettings)
        return baseForZonalSurcharge.times(userSettings.selectMonthOfYear.tariffRate * (zonalSurchargePercent / 100))
    }

    private fun getBasicTimeForCalculationSurcharge(
        routeList: List<Route>,
        userSettings: UserSettings
    ): Long {
        val currentMonthOfYear = userSettings.selectMonthOfYear
        val totalWorkTime = getTotalWorkTime(routeList, userSettings, currentMonthOfYear)
        val passengerTime = getPassengerTime(routeList, userSettings, currentMonthOfYear)

        return totalWorkTime - passengerTime
    }

     fun getMoneyListSurchargeExtendedServicePhase(
        routeList: List<Route>,
        userSettings: UserSettings
    ): MutableList<Double> {
        val salarySetting = salarySettingUseCase.getSalarySetting()
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
            moneyList.add(totalTimeInServicePhase.times(userSettings.selectMonthOfYear.tariffRate * (percentList[index].toDoubleOrZero() / 100)))
        }
        return moneyList
    }

    fun getMoneyAtLongDistanceTrain(
        routeList: List<Route>,
        userSettings: UserSettings
    ): Double {
        val salarySetting = salarySettingUseCase.getSalarySetting()
        val surchargeLongDistanceTrainsPercent = salarySetting.percentLongDistanceTrain
        val timeLongDistanceTrain =
            routeList.getLongDistanceTime(salarySetting.lengthLongDistanceTrain)
        return timeLongDistanceTrain.times(userSettings.selectMonthOfYear.tariffRate * (surchargeLongDistanceTrainsPercent / 100))
    }

    fun getMoneyListSurchargeHeavyTrains(
        routeList: List<Route>,
        userSettings: UserSettings
    ): MutableList<Double> {
        val salarySetting = salarySettingUseCase.getSalarySetting()

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
            moneyList.add(totalTimeHeavyTrain.times(userSettings.selectMonthOfYear.tariffRate * (percentList[index].toDoubleOrZero() / 100)))
        }
        return moneyList
    }

    fun getMoneyAtOnePersonOperation(
        routeList: List<Route>,
        userSettings: UserSettings,
    ): Double {
        val salarySetting = salarySettingUseCase.getSalarySetting()
        val currentMonthOfYear = userSettings.selectMonthOfYear
        val onePersonOperationTime =
            routeList.getOnePersonOperationTime(currentMonthOfYear, userSettings.timeZone)
        return onePersonOperationTime.times(userSettings.selectMonthOfYear.tariffRate * (salarySetting.onePersonOperationPercent / 100))
    }

    fun getMoneyNordicSurcharge(
        routeList: List<Route>,
        userSettings: UserSettings
    ): Double {
        val salarySetting = salarySettingUseCase.getSalarySetting()
        val baseForCalculation = getBaseMoney(routeList, userSettings, salarySetting)
        val nordicSurchargePercent = salarySetting.nordicPercent
        return baseForCalculation.times(nordicSurchargePercent / 100)
    }


    fun getMoneyDistrictSurcharge(
        routeList: List<Route>,
        userSettings: UserSettings
    ): Double {
        val salarySetting = salarySettingUseCase.getSalarySetting()
        val baseForCalculation = getBaseMoney(routeList, userSettings, salarySetting)
        val districtCoefficient = salarySetting.districtCoefficient
        return baseForCalculation.times(districtCoefficient / 100)
    }

    fun getMoneyAtQualificationClass(
        routeList: List<Route>,
        userSettings: UserSettings,
    ): Double {
        val salarySetting = salarySettingUseCase.getSalarySetting()
        val baseForZonalSurcharge = getBasicTimeForCalculationSurcharge(routeList, userSettings)
        val surchargeQualificationClassPercent = salarySetting.surchargeQualificationClass
        return baseForZonalSurcharge.times(userSettings.selectMonthOfYear.tariffRate * (surchargeQualificationClassPercent / 100))
    }


    private fun getMoneyAtPaymentOvertime(
        routeList: List<Route>,
        userSettings: UserSettings,
    ): Double {
        val baseMoneyForOvertime =
            getBasicMoneyForOvertimeCalculation(routeList, userSettings)
        val currentMonthOfYear = userSettings.selectMonthOfYear
        val totalWorkTime = getTotalWorkTime(routeList, userSettings, currentMonthOfYear)
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
            getBasicMoneyForOvertimeCalculation(routeList, userSettings)
        val currentMonthOfYear = userSettings.selectMonthOfYear
        val totalWorkTime = getTotalWorkTime(routeList, userSettings, currentMonthOfYear)
        val personalNormaHoursInLong = getPersonalNormaInLong(userSettings)
        val overTime = getOvertime(totalWorkTime, personalNormaHoursInLong)
        val surchargeAtOvertime05Hour = getOvertime05Surcharge(routeList, overTime)
        val overtimeMoneyOfOneHour = (baseMoneyForOvertime / totalWorkTime) / 2
        return surchargeAtOvertime05Hour.times(overtimeMoneyOfOneHour)
    }

    private fun getOvertime05Surcharge(routeList: List<Route>, overTime: Long): Long {
        val routeCount = routeList.size
        val twoHourInMillis = 7_200_000L
        return if (routeCount != 0 && overTime / routeCount < twoHourInMillis) {
            overTime
        } else {
            routeCount.toLong() * twoHourInMillis
        }
    }

    private fun getMoneyAtSurchargeOvertime(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting
    ): Double {
        val baseMoneyForOvertime =
            getBasicMoneyForOvertimeCalculation(routeList, userSettings)
        val currentMonthOfYear = userSettings.selectMonthOfYear
        val totalWorkTime = getTotalWorkTime(routeList, userSettings, currentMonthOfYear)
        val personalNormaHoursInLong = getPersonalNormaInLong(userSettings)
        val overTime = getOvertime(totalWorkTime, personalNormaHoursInLong)
        val surchargeAtOvertime05Hour = getOvertime05Surcharge(routeList, overTime)
        val surchargeAtOvertimeHour = getOvertimeSurcharge(overTime, surchargeAtOvertime05Hour)
        val overtimeMoneyOfOneHour = baseMoneyForOvertime / totalWorkTime
        return surchargeAtOvertimeHour.times(overtimeMoneyOfOneHour)
    }

    private fun getOvertimeSurcharge(overTime: Long, surchargeAtOvertime05Hour: Long) =
        if (overTime > surchargeAtOvertime05Hour) overTime - surchargeAtOvertime05Hour else 0L


    private fun getBasicMoneyForOvertimeCalculation(
        routeList: List<Route>,
        userSettings: UserSettings,
    ): Double {
        val paymentAtTariffMoney =
            getMoneyAtWorkTimeAtTariff(routeList, userSettings)
        val paymentAtPassengerMoney = getMoneyAtPassenger(routeList, userSettings)
        val paymentAtSingleLocomotiveMoney = getMoneyAtSingleLocomotive(routeList, userSettings)
        val zonalSurchargeMoney = getMoneyAtZonalSurcharge(routeList, userSettings)
        val paymentNightTimeMoney = getMoneyAtNightTime(routeList, userSettings)
        val surchargeQualificationClassMoney =
            getMoneyAtQualificationClass(routeList, userSettings)
        val surchargeExtendedServicePhaseMoney =
            getMoneyListSurchargeExtendedServicePhase(routeList, userSettings).sum()
        val surchargeOnePersonOperationMoney =
            getMoneyAtOnePersonOperation(routeList, userSettings)
        val surchargeHarmfulnessSurchargeMoney =
            getMoneyAtHarmfulness(routeList, userSettings)
        val surchargeLongDistanceTrainsMoney = getMoneyAtLongDistanceTrain(routeList, userSettings)
        val surchargeHeavyTrains = getMoneyListSurchargeHeavyTrains(routeList, userSettings).sum()
        return paymentAtTariffMoney + paymentAtPassengerMoney +
                paymentAtSingleLocomotiveMoney + zonalSurchargeMoney +
                paymentNightTimeMoney + surchargeQualificationClassMoney +
                surchargeExtendedServicePhaseMoney + surchargeOnePersonOperationMoney +
                surchargeHarmfulnessSurchargeMoney + surchargeLongDistanceTrainsMoney +
                surchargeHeavyTrains
    }

    fun getMoneyAtHarmfulness(
        routeList: List<Route>,
        userSettings: UserSettings,
    ): Double {
        val salarySetting = salarySettingUseCase.getSalarySetting()
        val harmfulnessSurchargePercent = salarySetting.harmfulnessPercent
        val basicForCalculation = getBasicTimeForCalculationSurcharge(routeList, userSettings)
        return basicForCalculation.times(userSettings.selectMonthOfYear.tariffRate * (harmfulnessSurchargePercent / 100))
    }

    fun getOtherSurchargeMoney(
        routeList: List<Route>,
        userSettings: UserSettings,
    ): Double{
        val salarySetting = salarySettingUseCase.getSalarySetting()
        val otherSurchargePercent = salarySetting.otherSurcharge
        val baseForZonalSurcharge = getBasicTimeForCalculationSurcharge(routeList, userSettings)
        return baseForZonalSurcharge.times(userSettings.selectMonthOfYear.tariffRate * (otherSurchargePercent / 100))
    }

    private fun getBaseMoney(
        routeList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting
    ): Double {
        val paymentAtTariffMoney =
            getMoneyAtWorkTimeAtTariff(routeList, userSettings)
        val paymentAtPassengerMoney = getMoneyAtPassenger(routeList, userSettings)
        val paymentAtSingleLocomotiveMoney = getMoneyAtSingleLocomotive(routeList, userSettings)
        val paymentHolidayMoney = getMoneyAtHoliday(routeList, userSettings)
        val surchargeHolidayMoney = getMoneyAtHoliday(routeList, userSettings)
        val zonalSurchargeMoney = getMoneyAtZonalSurcharge(routeList, userSettings)
        val paymentNightTimeMoney = getMoneyAtNightTime(routeList, userSettings)
        val surchargeQualificationClassMoney =
            getMoneyAtQualificationClass(routeList, userSettings)
        val surchargeExtendedServicePhaseMoney =
            getMoneyListSurchargeExtendedServicePhase(routeList, userSettings).sum()

        val paymentAtOvertimeMoney =
            getMoneyAtPaymentOvertime(routeList, userSettings)

        val surchargeAtOvertime05Money =
            getMoneyAtSurchargeOvertime05(routeList, userSettings, salarySetting)

        val surchargeAtOvertimeMoney =
            getMoneyAtSurchargeOvertime(routeList, userSettings, salarySetting)

        val surchargeOnePersonOperationMoney =
            getMoneyAtOnePersonOperation(routeList, userSettings)

        val surchargeHarmfulnessSurchargeMoney =
            getMoneyAtHarmfulness(routeList, userSettings)

        val surchargeLongDistanceTrainsMoney = getMoneyAtLongDistanceTrain(routeList, userSettings)

        val surchargeHeavyTrains = getMoneyListSurchargeHeavyTrains(routeList, userSettings).sum()

        val otherSurcharge = getOtherSurchargeMoney(routeList, userSettings)

        return paymentAtTariffMoney + paymentAtPassengerMoney +
                paymentAtSingleLocomotiveMoney + paymentAtOvertimeMoney +
                surchargeAtOvertime05Money + surchargeAtOvertimeMoney +
                paymentHolidayMoney + surchargeHolidayMoney +
                zonalSurchargeMoney + paymentNightTimeMoney +
                surchargeQualificationClassMoney + surchargeExtendedServicePhaseMoney +
                surchargeOnePersonOperationMoney + surchargeHarmfulnessSurchargeMoney +
                surchargeLongDistanceTrainsMoney + surchargeHeavyTrains + otherSurcharge
    }
}