package com.z_company.route.viewmodel

import com.z_company.domain.entities.SalarySetting
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.entities.UtilForMonthOfYear.getDayoffHours
import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHoursInPeriod
import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHours
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.getLongDistanceTime
import com.z_company.domain.entities.route.UtilsForEntities.getNewRoutesToDayRange
import com.z_company.domain.entities.route.UtilsForEntities.getNightTime
import com.z_company.domain.entities.route.UtilsForEntities.getOnePersonOperationTime
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getSingleLocomotiveTime
import com.z_company.domain.entities.route.UtilsForEntities.getTimeInHeavyTrain
import com.z_company.domain.entities.route.UtilsForEntities.getTimeInServicePhase
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkingTimeOnAHoliday
import com.z_company.domain.util.sum
import com.z_company.domain.util.toDoubleOrZero
import com.z_company.domain.util.toIntOrZero
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class SalaryCalculationHelper(
    private val userSettings: UserSettings,
    private val salarySetting: SalarySetting,
    private val routeList: List<Route>,
) {
    val currentMonthOfYear = userSettings.selectMonthOfYear
    val dateSetTariffRate = currentMonthOfYear.dateSetTariffRate

    val date = userSettings.selectMonthOfYear.dateSetTariffRate?.dateNewRate ?: 1
    val firstDate = 1
    val lastDate = userSettings.selectMonthOfYear.days.last().dayOfMonth

    fun getWorkTimeAtTariffFlow(): Flow<Long> {
        return channelFlow {
            val personalNormaHoursInLong = getPersonalNormaInLong()
            val totalWorkTime = getTotalWorkTime().first()
            val passengerTime = getPassengerTime(routeList)
            val singleLocoTime = getSingleLocomotiveTime(routeList)
            val paymentHolidayHours = getHolidayTime(routeList)
            val overtime = getOvertime(totalWorkTime, personalNormaHoursInLong)

            val result =
                totalWorkTime - passengerTime - singleLocoTime - paymentHolidayHours - overtime
            trySend(result)
            awaitClose()
        }
    }

    fun getWorkTimeInPeriodAtTariffFlow(
        routeList: List<Route>,
        period: Pair<Int, Int>
    ): Flow<Long> {
        return channelFlow {
            val personalNormaHoursInLong = getPersonalNormaHoursToPeriod(period)
            val totalWorkTime = getTotalWorkTime().first()
            val passengerTime = getPassengerTime(routeList)
            val singleLocoTime = getSingleLocomotiveTime(routeList)
            val paymentHolidayHours = getHolidayTime(routeList)
            val overtime = getOvertime(totalWorkTime, personalNormaHoursInLong)

            var result =
                totalWorkTime - passengerTime - singleLocoTime - paymentHolidayHours - overtime
            if (result < 0) result = 0

            trySend(result)
        }
    }

    fun getMoneyAtWorkTimeAtTariff(): Flow<Double> {
        return channelFlow {
            if (dateSetTariffRate == null) {
                getWorkTimeAtTariffFlow().collect { timeInLong ->
                    val money =
                        timeInLong.times(currentMonthOfYear.tariffRate) / 3_600_000.toDouble()
                    trySend(money)
                }
            } else {
                val pairRoutes = getTwoRouteList(routeList).first()
                val firstRoutes = pairRoutes.first
                val secondRoutes = pairRoutes.second

                val firstRoutesTime = getWorkTimeInPeriodAtTariffFlow(
                    routeList = firstRoutes,
                    period = Pair(firstDate, date)
                ).first()
                val firstRoutesMoney =
                    firstRoutesTime.times(dateSetTariffRate.oldRate) / 3_600_000.toDouble()

                val secondRoutesTime = getWorkTimeInPeriodAtTariffFlow(
                    routeList = secondRoutes,
                    period = Pair(date, lastDate)
                ).first()
                val secondRoutesMoney =
                    secondRoutesTime.times(currentMonthOfYear.tariffRate) / 3_600_000.toDouble()
                val result = firstRoutesMoney + secondRoutesMoney
                trySend(result)
            }
            awaitClose()
        }
    }

    fun getNightTimeFlow(routes: List<Route> = routeList): Flow<Long> {
        return channelFlow {
            val time = routes.getNightTime(userSettings)
            trySend(time)
            awaitClose()
        }
    }

    fun getMoneyAtNightTimeFlow(): Flow<Double> {
        return channelFlow {
            if (dateSetTariffRate == null) {
                getNightTimeFlow().collect { timeInLong ->
                    val money =
                        timeInLong.times(currentMonthOfYear.tariffRate * (salarySetting.nightTimePercent / 100)) / 3_600_000.toDouble()
                    trySend(money)
                }
            } else {
                val pairRoutes = getTwoRouteList(routeList).first()
                val firstRoutes = pairRoutes.first
                val secondRoutes = pairRoutes.second

                combine(
                    getNightTimeFlow(firstRoutes),
                    getNightTimeFlow(secondRoutes)
                ) { firstTime, secondTime ->
                    val firstMoney =
                        firstTime.times(dateSetTariffRate.oldRate * (salarySetting.nightTimePercent / 100))
                    val secondMoney =
                        secondTime.times(currentMonthOfYear.tariffRate * (salarySetting.nightTimePercent / 100))

                    val result = (firstMoney + secondMoney) / 3_600_000.toDouble()
                    trySend(result)
                }.collect {}
            }
            awaitClose()
        }
    }

    fun getSingleLocomotiveTimeFlow(routes: List<Route> = routeList): Flow<Long> {
        return channelFlow {
            var singleLocoTimeFollowing = getSingleLocomotiveTime(routes)
            trySend(singleLocoTimeFollowing)
            awaitClose()
        }
    }

    fun getMoneyAtSingleLocomotiveFlow(): Flow<Double> {
        return channelFlow {
            if (dateSetTariffRate == null) {
                getSingleLocomotiveTimeFlow().collect { time ->
                    val money = time.times(currentMonthOfYear.tariffRate) / 3_600_000.toDouble()
                    trySend(money)
                }
            } else {
                val pairRoutes = getTwoRouteList(routeList).first()
                val firstRoutes = pairRoutes.first
                val secondRoutes = pairRoutes.second

                combine(
                    getSingleLocomotiveTimeFlow(firstRoutes),
                    getSingleLocomotiveTimeFlow(secondRoutes)
                ) { firstTime, secondTime ->
                    val firstMoney = firstTime.times(dateSetTariffRate.oldRate)
                    val secondMoney = secondTime.times(currentMonthOfYear.tariffRate)
                    val result = (firstMoney + secondMoney) / 3_600_000.toDouble()
                    trySend(result)
                }.collect {}
            }
            awaitClose()
        }
    }

    fun getPassengerTimeFlow(routes: List<Route> = routeList): Flow<Long> {
        return channelFlow {
            val time = getPassengerTime(routes)
            trySend(time)
            awaitClose()
        }
    }

    fun getMoneyAtPassengerFlow(): Flow<Double> {
        return channelFlow {
            if (dateSetTariffRate == null) {
                getPassengerTimeFlow().collect { time ->
                    val money = time.times(currentMonthOfYear.tariffRate) / 3_600_000.toDouble()
                    trySend(money)
                }
            } else {
                val pairRoutes = getTwoRouteList(routeList).first()
                val firstRoutes = pairRoutes.first
                val secondRoutes = pairRoutes.second

                combine(
                    getPassengerTimeFlow(firstRoutes),
                    getPassengerTimeFlow(secondRoutes)
                ) { firstTime, secondTime ->
                    val firstMoney = firstTime.times(dateSetTariffRate.oldRate)
                    val secondMoney = secondTime.times(currentMonthOfYear.tariffRate)
                    val result = (firstMoney + secondMoney) / 3_600_000.toDouble()
                    trySend(result)
                }.collect {}
            }
            awaitClose()
        }
    }

    fun getHolidayTimeFlow(routes: List<Route> = routeList): Flow<Long> {
        return channelFlow {
            val time = getHolidayTime(routes)
            trySend(time)
            awaitClose()
        }
    }

    fun getMoneyAtHolidayFlow(): Flow<Double> {
        return channelFlow {
            if (dateSetTariffRate == null) {
                getHolidayTimeFlow().collect { time ->
                    val money = time.times(currentMonthOfYear.tariffRate) / 3_600_000.toDouble()
                    trySend(money)
                }
            } else {
                val pairRoutes = getTwoRouteList(routeList).first()
                val firstRoutes = pairRoutes.first
                val secondRoutes = pairRoutes.second

                combine(
                    getHolidayTimeFlow(firstRoutes),
                    getHolidayTimeFlow(secondRoutes)
                ) { firstTime, secondTime ->
                    val firstMoney = firstTime.times(dateSetTariffRate.oldRate)
                    val secondMoney = secondTime.times(currentMonthOfYear.tariffRate)
                    val result = (firstMoney + secondMoney) / 3_600_000.toDouble()
                    trySend(result)
                }.collect {}
            }
            awaitClose()
        }
    }

    fun getMoneyAtQualificationClassFlow(): Flow<Double> {
        return channelFlow {
            val surchargeQualificationClassPercent = salarySetting.surchargeQualificationClass
            if (dateSetTariffRate == null) {
                getBasicTimeForCalculationSurcharge().collect { time ->
                    val money =
                        time.times(currentMonthOfYear.tariffRate * (surchargeQualificationClassPercent / 100)) / 3_600_000.toDouble()
                    trySend(money)
                }
            } else {
                val pairRoutes = getTwoRouteList(routeList).first()
                val firstRoutes = pairRoutes.first
                val secondRoutes = pairRoutes.second

                combine(
                    getBasicTimeForCalculationSurcharge(firstRoutes),
                    getBasicTimeForCalculationSurcharge(secondRoutes)
                ) { firstTime, secondTime ->
                    val firstMoney =
                        firstTime.times(dateSetTariffRate.oldRate * (surchargeQualificationClassPercent / 100))
                    val secondMoney =
                        secondTime.times(currentMonthOfYear.tariffRate * (surchargeQualificationClassPercent / 100))
                    val result = (firstMoney + secondMoney) / 3_600_000.toDouble()
                    trySend(result)
                }.collect {}
            }
            awaitClose()
        }
    }

    fun getTimeListSurchargeServicePhaseFlow(routes: List<Route> = routeList): Flow<List<Long>> {
        return channelFlow {
            val phaseList =
                salarySetting.surchargeExtendedServicePhaseList.sortedBy {
                    it.distance
                }

            val timeList: MutableList<Long> = mutableListOf()
            phaseList.forEachIndexed { index, _ ->
                var totalTimeInServicePhase = 0L
                routes.forEach { route ->
                    val timeInRoute = route.getTimeInServicePhase(
                        phaseList.map { it.distance.toIntOrNull() ?: 0 },
                        index
                    )
                    totalTimeInServicePhase += timeInRoute
                }
                timeList.add(totalTimeInServicePhase)
            }
            trySend(timeList)
            awaitClose()
        }
    }

    fun getPercentListSurchargeExtendedServicePhaseFlow(): Flow<List<String>> {
        return flow {
            val phaseList =
                salarySetting.surchargeExtendedServicePhaseList.sortedBy {
                    it.distance
                }
            val percentList = phaseList.map {
                it.percentSurcharge
            }
            emit(percentList)
        }
    }

    fun getMoneyListSurchargeExtendedServicePhaseFlow(): Flow<List<Double>> {
        return channelFlow {
            val percentList = getPercentListSurchargeExtendedServicePhaseFlow().first()
            val moneyList: MutableList<Double> = mutableListOf()

            if (dateSetTariffRate == null) {
                getTimeListSurchargeServicePhaseFlow().collect { timeList ->
                    timeList.forEachIndexed { index, timeInServicePhase ->
                        val money =
                            timeInServicePhase
                                .times(currentMonthOfYear.tariffRate * (percentList[index].toDoubleOrZero() / 100)) / 3_600_000.toDouble()
                        moneyList.add(money)
                    }
                    trySend(moneyList)
                }
            } else {
                val pairRoutes = getTwoRouteList(routeList).first()
                val firstRoutes = pairRoutes.first
                val secondRoutes = pairRoutes.second
                combine(
                    getTimeListSurchargeServicePhaseFlow(firstRoutes),
                    getTimeListSurchargeServicePhaseFlow(secondRoutes),
                ) { firstTimeList, secondTimeList ->
                    firstTimeList.forEachIndexed { index, timeInServicePhase ->
                        val money =
                            timeInServicePhase.times(dateSetTariffRate.oldRate * (percentList[index].toDoubleOrZero() / 100)) / 3_600_000.toDouble()
                        moneyList.add(money)
                    }
                    secondTimeList.forEachIndexed { index, timeInServicePhase ->
                        val money =
                            timeInServicePhase.times(currentMonthOfYear.tariffRate * (percentList[index].toDoubleOrZero() / 100)) / 3_600_000.toDouble()
                        val summaryMoney = money + moneyList[index]
                        moneyList[index] = summaryMoney
                    }
                    trySend(moneyList)
                }.collect {}
            }
            awaitClose()
        }
    }

    fun getPercentOnePersonOperationFlow(): Flow<Double> {
        return flow {
            val percent = salarySetting.onePersonOperationPercent
            emit(percent)
        }
    }

    fun getTimeOnePersonOperationFlow(routes: List<Route> = routeList): Flow<Long> {
        return channelFlow {
            val time = routes.getOnePersonOperationTime(
                currentMonthOfYear, userSettings.timeZone
            )
            trySend(time)
            awaitClose()
        }
    }

    fun getMoneyOnePersonOperationFlow(): Flow<Double> {
        return channelFlow {
            val percent = getPercentOnePersonOperationFlow().first()

            if (dateSetTariffRate == null) {
                getTimeOnePersonOperationFlow().collect { time ->
                    val money =
                        time.times(currentMonthOfYear.tariffRate * (percent / 100)) / 3_600_000.toDouble()
                    trySend(money)
                }
            } else {
                val pairRoutes = getTwoRouteList(routeList).first()
                val firstRoutes = pairRoutes.first
                val secondRoutes = pairRoutes.second

                combine(
                    getTimeOnePersonOperationFlow(firstRoutes),
                    getTimeOnePersonOperationFlow(secondRoutes)
                ) { firstTime, secondTime ->
                    val moneyFirstTime =
                        firstTime.times(dateSetTariffRate.oldRate * (percent / 100))
                    val moneySecondTime =
                        secondTime.times(currentMonthOfYear.tariffRate * (percent / 100))
                    val result = (moneyFirstTime + moneySecondTime) / 3_600_000.toDouble()
                    trySend(result)
                }.collect { }
            }
            awaitClose()
        }
    }

    fun getPercentHarmfulnessFlow(): Flow<Double> {
        return flow {
            val percent = salarySetting.harmfulnessPercent
            emit(percent)
        }
    }

    fun getTimeHarmfulnessFlow(routes: List<Route> = routeList): Flow<Long> {
        return channelFlow {
            getBasicTimeForCalculationSurcharge(routes).collect { time ->
                trySend(time)
            }
            awaitClose()
        }
    }

    fun getMoneyHarmfulnessFlow(): Flow<Double> {
        return channelFlow {
            val percent = getPercentHarmfulnessFlow().first()
            if (dateSetTariffRate == null) {
                getTimeHarmfulnessFlow().collect { time ->
                    val money =
                        time.times(currentMonthOfYear.tariffRate * (percent / 100)) / 3_600_000.toDouble()
                    trySend(money)
                }
            } else {
                val pairRoutes = getTwoRouteList(routeList).first()
                val firstRoutes = pairRoutes.first
                val secondRoutes = pairRoutes.second

                combine(
                    getTimeHarmfulnessFlow(firstRoutes),
                    getTimeHarmfulnessFlow(secondRoutes)
                ) { firstTime, secondTime ->
                    val moneyFirstTime =
                        firstTime.times(dateSetTariffRate.oldRate * (percent / 100))
                    val moneySecondTime =
                        secondTime.times(currentMonthOfYear.tariffRate * (percent / 100))
                    val result = (moneyFirstTime + moneySecondTime) / 3_600_000.toDouble()
                    trySend(result)
                }.collect { }
            }
            awaitClose()
        }
    }

    fun getPercentLongDistanceTrainFlow(): Flow<Double> {
        return flow {
            val percent = salarySetting.percentLongDistanceTrain
            emit(percent)
        }
    }

    fun getTimeLongDistanceTrainFlow(routes: List<Route> = routeList): Flow<Long> {
        return channelFlow {
            val time = routes.getLongDistanceTime(salarySetting.lengthLongDistanceTrain)
            trySend(time)
            awaitClose()
        }
    }

    fun getMoneyLongDistanceTrainFlow(): Flow<Double> {
        return channelFlow {
            val percent = getPercentLongDistanceTrainFlow().first()
            if (dateSetTariffRate == null) {
                getTimeLongDistanceTrainFlow().collect { time ->
                    val money =
                        time.times(currentMonthOfYear.tariffRate * (percent / 100)) / 3_600_000.toDouble()
                    trySend(money)
                }
            } else {
                val pairRoutes = getTwoRouteList(routeList).first()
                val firstRoutes = pairRoutes.first
                val secondRoutes = pairRoutes.second

                combine(
                    getTimeLongDistanceTrainFlow(firstRoutes),
                    getTimeLongDistanceTrainFlow(secondRoutes)
                ) { firstTime, secondTime ->
                    val moneyFirstTime =
                        firstTime.times(dateSetTariffRate.oldRate * (percent / 100))
                    val moneySecondTime =
                        secondTime.times(currentMonthOfYear.tariffRate * (percent / 100))
                    val result = (moneyFirstTime + moneySecondTime) / 3_600_000.toDouble()
                    trySend(result)
                }.collect { }
            }
            awaitClose()
        }
    }

    fun getPercentListSurchargeExtendedHeavyTrainsFlow(): Flow<List<String>> {
        return channelFlow {
            val percentList =
                salarySetting.surchargeHeavyTrainsList.sortedBy {
                    it.weight
                }.map {
                    it.percentSurcharge
                }
            trySend(percentList)
            awaitClose()
        }
    }

    fun getTimeListSurchargeHeavyTrainsFlow(routes: List<Route> = routeList): Flow<List<Long>> {
        return channelFlow {
            val surchargeListSorted = salarySetting.surchargeHeavyTrainsList.sortedBy {
                it.weight
            }
            val timeList: MutableList<Long> = mutableListOf()
            surchargeListSorted.forEachIndexed { index, _ ->
                var totalTimeHeavyTrain = 0L
                routes.forEach { route ->
                    totalTimeHeavyTrain += route.getTimeInHeavyTrain(
                        surchargeListSorted.map { it.weight.toIntOrZero() },
                        index
                    )
                }
                timeList.add(totalTimeHeavyTrain)
            }
            trySend(timeList)
            awaitClose()
        }
    }

    fun getMoneyListSurchargeExtendedHeavyTrainsFlow(): Flow<List<Double>> {
        return channelFlow {
            val percentList = getPercentListSurchargeExtendedHeavyTrainsFlow().first()
            val moneyList: MutableList<Double> = mutableListOf()

            if (dateSetTariffRate == null) {
                getTimeListSurchargeHeavyTrainsFlow().collect { timeList ->
                    timeList.forEachIndexed { index, timeInHeavyTrains ->
                        val money =
                            timeInHeavyTrains
                                .times(currentMonthOfYear.tariffRate * (percentList[index].toDoubleOrZero() / 100)) / 3_600_000.toDouble()
                        moneyList.add(money)
                    }
                    trySend(moneyList)
                }
            } else {
                val pairRoutes = getTwoRouteList(routeList).first()
                val firstRoutes = pairRoutes.first
                val secondRoutes = pairRoutes.second
                combine(
                    getTimeListSurchargeHeavyTrainsFlow(firstRoutes),
                    getTimeListSurchargeHeavyTrainsFlow(secondRoutes),
                ) { firstTimeList, secondTimeList ->
                    firstTimeList.forEachIndexed { index, timeInHeavyTrains ->
                        val money =
                            timeInHeavyTrains.times(dateSetTariffRate.oldRate * (percentList[index].toDoubleOrZero() / 100)) / 3_600_000.toDouble()
                        moneyList.add(money)
                    }
                    secondTimeList.forEachIndexed { index, timeInHeavyTrains ->
                        val money =
                            timeInHeavyTrains.times(currentMonthOfYear.tariffRate * (percentList[index].toDoubleOrZero() / 100)) / 3_600_000.toDouble()
                        val summaryMoney = money + moneyList[index]
                        moneyList[index] = summaryMoney
                    }
                    trySend(moneyList)
                }.collect {}
            }
            awaitClose()
        }
    }

    fun getPercentZonalSurchargeFlow(): Flow<Double> {
        return flow {
            val percent = salarySetting.zonalSurcharge
            emit(percent)
        }
    }

    fun getTimeZonalSurchargeFlow(routes: List<Route> = routeList): Flow<Long> {
        return channelFlow {
            getBasicTimeForCalculationSurcharge(routes).collect { time ->
                trySend(time)
            }
            awaitClose()
        }
    }

    fun getMoneyZonalSurchargeFlow(): Flow<Double> {
        return channelFlow {
            val percent = getPercentZonalSurchargeFlow().first()
            if (dateSetTariffRate == null) {
                getTimeZonalSurchargeFlow().collect { time ->
                    val money =
                        time.times(currentMonthOfYear.tariffRate * (percent / 100)) / 3_600_000.toDouble()
                    trySend(money)
                }
            } else {
                val pairRoutes = getTwoRouteList(routeList).first()
                val firstRoutes = pairRoutes.first
                val secondRoutes = pairRoutes.second

                combine(
                    getTimeZonalSurchargeFlow(firstRoutes),
                    getTimeZonalSurchargeFlow(secondRoutes)
                ) { firstTime, secondTime ->
                    val moneyFirstTime =
                        firstTime.times(dateSetTariffRate.oldRate * (percent / 100))
                    val moneySecondTime =
                        secondTime.times(currentMonthOfYear.tariffRate * (percent / 100))
                    val result = (moneyFirstTime + moneySecondTime) / 3_600_000.toDouble()
                    trySend(result)
                }.collect { }
            }
            awaitClose()
        }
    }

    fun getTimeOvertimeFlow(): Flow<Long> {
        return flow {
            val personalNormaHoursInLong = getPersonalNormaInLong()
            val totalWorkTime = getTotalWorkTime().first()
            val overtime = getOvertime(
                totalWorkTime = totalWorkTime,
                personalNormaHoursInLong = personalNormaHoursInLong
            )
            emit(overtime)
        }
    }

    fun getMoneyOvertimeFlow(): Flow<Double> {
        return flow {
            val baseMoneyForOvertime = getBasicMoneyForOvertimeCalculation().first()
            val totalWorkTime = getTotalWorkTime().first()
            val personalNormaHoursInLong = getPersonalNormaInLong()
            val overTime = getOvertime(totalWorkTime, personalNormaHoursInLong)
            val overtimeMoneyOfOneHour = baseMoneyForOvertime / totalWorkTime
            val result = overTime.times(overtimeMoneyOfOneHour)
            emit(result)
        }
    }

    fun getTimeSurchargeAtOvertime05Flow(): Flow<Long> {
        return flow {
            val overtime = getTimeOvertimeFlow().first()
            val routeCount = routeList.size
            val twoHourInMillis = 7_200_000L
            val time = if (routeCount != 0 && overtime / routeCount < twoHourInMillis) {
                overtime
            } else {
                routeCount.toLong() * twoHourInMillis
            }
            emit(time)
        }
    }

    fun getMoneySurchargeOvertime05Flow(): Flow<Double> {
        return flow {
            val time = getTimeSurchargeAtOvertime05Flow().first()
            val money = time.times(currentMonthOfYear.tariffRate * 0.5) / 3_600_000.toDouble()
            emit(money)
        }
    }

    fun getTimeSurchargeAtOvertimeFlow(): Flow<Long> {
        return flow {
            val overtime = getTimeOvertimeFlow().first()
            val timeSurchargeAtOvertime05 = getTimeSurchargeAtOvertime05Flow().first()
            val time =
                if (overtime > timeSurchargeAtOvertime05) overtime - timeSurchargeAtOvertime05
                else 0L
            emit(time)
        }
    }

    fun getMoneySurchargeOvertimeFlow(): Flow<Double> {
        return flow {
            val baseMoneyForOvertime = getBasicMoneyForOvertimeCalculation().first()
            val totalWorkTime = getTotalWorkTime().first()
            val surchargeAtOvertimeHour = getTimeSurchargeAtOvertimeFlow().first()
            val overtimeMoneyOfOneHour = baseMoneyForOvertime / totalWorkTime
            val money = surchargeAtOvertimeHour.times(overtimeMoneyOfOneHour)
            emit(money)
        }
    }

    fun getPercentDistrictSurcharge(): Flow<Double> {
        return flow {
            val percent = salarySetting.districtCoefficient
            emit(percent)
        }
    }

    fun getMoneyDistrictSurcharge(): Flow<Double> {
        return flow {
            val baseForCalculation = getBasicMoney().first()
            val districtCoefficient = getPercentDistrictSurcharge().first()
            val money = baseForCalculation.times(districtCoefficient / 100)
            emit(money)
        }
    }

    fun getPercentNordicSurcharge(): Flow<Double> {
        return flow {
            val percent = salarySetting.nordicPercent
            emit(percent)
        }
    }

    fun getMoneyNordicSurcharge(): Flow<Double> {
        return flow {
            val baseForCalculation = getBasicMoney().first()
            val nordicCoefficient = getPercentNordicSurcharge().first()
            val money = baseForCalculation.times(nordicCoefficient / 100)
            emit(money)
        }
    }

    fun getDayOffHoursFlow(): Flow<Int> {
        return flow {
            val hours = currentMonthOfYear.getDayoffHours()
            emit(hours)
        }
    }

    fun getMoneyAverageFlow(): Flow<Double> {
        return flow {
            val dayOffHours = getDayOffHoursFlow().first()
            val averagePaymentHour = salarySetting.averagePaymentHour
            val money = averagePaymentHour.times(dayOffHours)
            emit(money)
        }
    }

    fun getPercentOtherSurchargeFlow(): Flow<Double> {
        return flow {
            val percent = salarySetting.otherSurcharge
            emit(percent)
        }
    }

    fun getMoneyOtherSurchargeFlow(): Flow<Double> {
        return flow {
            val percent = getPercentOtherSurchargeFlow().first()
            if (dateSetTariffRate == null) {
                val baseForZonalSurcharge = getBasicTimeForCalculationSurcharge().first()
                val money = baseForZonalSurcharge
                    .times(currentMonthOfYear.tariffRate * (percent / 100)) / 3_600_000.toDouble()
                emit(money)
            } else {
                val pairRoutes = getTwoRouteList(routeList).first()
                val firstRoutes = pairRoutes.first
                val secondRoutes = pairRoutes.second

                combine(
                    getBasicTimeForCalculationSurcharge(firstRoutes),
                    getBasicTimeForCalculationSurcharge(secondRoutes)
                ) { firstTime, secondTime ->
                    val moneyFirstTime =
                        firstTime.times(dateSetTariffRate.oldRate * (percent / 100))
                    val moneySecondTime =
                        secondTime.times(currentMonthOfYear.tariffRate * (percent / 100))
                    val result = (moneyFirstTime + moneySecondTime) / 3_600_000.toDouble()
                    emit(result)
                }.collect { }
            }
        }
    }

    fun getMoneyTotalChargedFlow(): Flow<Double> {
        return flow {
            val baseMoney = getBasicMoney().first()
            val holidayMoney = getMoneyAtHolidayFlow().first() * 2
            val averageMoney = getMoneyAverageFlow().first()
            val nordicSurcharge = getMoneyNordicSurcharge().first()
            val districtSurcharge = getMoneyDistrictSurcharge().first()

            val totalMoney = baseMoney + holidayMoney + averageMoney + nordicSurcharge + districtSurcharge
            emit(totalMoney)
        }
    }

    fun getPercentNDFLRetentionFlow(): Flow<Double> {
        return flow {
            val percent = salarySetting.ndfl
            emit(percent)
        }
    }

    fun getMoneyNDFLRetentionFlow(): Flow<Double> {
        return flow {
            val percentNDFL = getPercentNDFLRetentionFlow().first()
            val baseForCalculation = getMoneyTotalChargedFlow().first()
            val money = baseForCalculation.times(percentNDFL / 100)
            emit(money)
        }
    }

    fun getPercentUnionistsRetentionFlow(): Flow<Double> {
        return flow {
            val percent = salarySetting.unionistsRetention
            emit(percent)
        }
    }

    fun getMoneyUnionistsRetentionFlow(): Flow<Double> {
        return flow {
            val percentUnionist = getPercentUnionistsRetentionFlow().first()
            val baseForCalculation = getMoneyTotalChargedFlow().first()
            val money = baseForCalculation.times(percentUnionist / 100)
            emit(money)
        }
    }

    fun getPercentOtherRetentionFlow(): Flow<Double> {
        return flow {
            val percent = salarySetting.otherRetention
            emit(percent)
        }
    }

    fun getMoneyOtherRetentionFlow(): Flow<Double> {
        return flow {
            val percentOther = getPercentOtherRetentionFlow().first()
            val baseForCalculation = getMoneyTotalChargedFlow().first()
            val money = baseForCalculation.times(percentOther / 100)
            emit(money)
        }
    }

    fun getMoneyTotalRetentionFlow(): Flow<Double> {
        return flow {
            val other = getMoneyOtherRetentionFlow().first()
            val unionists = getMoneyUnionistsRetentionFlow().first()
            val ndfl = getMoneyNDFLRetentionFlow().first()
            val total = other + unionists + ndfl
            emit(total)
        }
    }

    fun getMoneyToBeCredited(): Flow<Double>{
        return flow {
            val totalCharged = getMoneyTotalChargedFlow().first()
            val totalRetention = getMoneyTotalRetentionFlow().first()
            val result = totalCharged - totalRetention
            emit(result)
        }
    }

    fun getTwoRouteList(routeList: List<Route>): Flow<Pair<List<Route>, List<Route>>> {
        return flow {
            val firstRoutes = routeList.getNewRoutesToDayRange(
                days = firstDate..date,
                monthOfYear = userSettings.selectMonthOfYear,
                offsetInMoscow = userSettings.timeZone
            )

            val secondRoutes = routeList.getNewRoutesToDayRange(
                days = date..lastDate,
                monthOfYear = userSettings.selectMonthOfYear,
                offsetInMoscow = userSettings.timeZone
            )
            emit(Pair(firstRoutes, secondRoutes))
        }
    }

    private fun getBasicMoney(): Flow<Double> {
        return flow {
            val basicForOvertime = getBasicMoneyForOvertimeCalculation().first()
            val overtimeMoney = getMoneyOvertimeFlow().first()
            val overtimeMoneySurcharge05 = getMoneySurchargeOvertime05Flow().first()
            val overtimeMoneySurcharge = getMoneySurchargeOvertimeFlow().first()
            val basicMoney =
                basicForOvertime + overtimeMoney + overtimeMoneySurcharge05 + overtimeMoneySurcharge
            emit(basicMoney)
        }
    }

    private fun getBasicMoneyForOvertimeCalculation(): Flow<Double> {
        return flow {
            val paymentAtTariffMoney = getMoneyAtWorkTimeAtTariff().first()
            val paymentAtPassengerMoney = getMoneyAtPassengerFlow().first()
            val paymentAtSingleLocomotiveMoney = getMoneyAtSingleLocomotiveFlow().first()
            val zonalSurchargeMoney = getMoneyZonalSurchargeFlow().first()
            val paymentNightTimeMoney = getMoneyAtNightTimeFlow().first()
            val surchargeQualificationClassMoney = getMoneyAtQualificationClassFlow().first()
            val surchargeExtendedServicePhaseMoney =
                getMoneyListSurchargeExtendedServicePhaseFlow().first().sum()
            val surchargeOnePersonOperationMoney = getMoneyOnePersonOperationFlow().first()
            val surchargeHarmfulnessSurchargeMoney = getMoneyHarmfulnessFlow().first()
            val surchargeLongDistanceTrainsMoney = getMoneyLongDistanceTrainFlow().first()
            val surchargeHeavyTrains = getMoneyListSurchargeExtendedHeavyTrainsFlow().first().sum()
            val otherSurcharge = getMoneyOtherSurchargeFlow().first()
            val basicMoney = paymentAtTariffMoney + paymentAtPassengerMoney +
                    paymentAtSingleLocomotiveMoney + zonalSurchargeMoney +
                    paymentNightTimeMoney + surchargeQualificationClassMoney +
                    surchargeExtendedServicePhaseMoney + surchargeOnePersonOperationMoney +
                    surchargeHarmfulnessSurchargeMoney + surchargeLongDistanceTrainsMoney +
                    surchargeHeavyTrains + otherSurcharge
            emit(basicMoney)
        }
    }

    fun getTotalWorkTime(routes: List<Route> = routeList) = flow {
        val time = routes.getWorkTime(currentMonthOfYear, userSettings.timeZone)
        emit(time)
    }

    private fun getPassengerTime(routeList: List<Route>) =
        routeList.getPassengerTime(currentMonthOfYear, userSettings.timeZone)

    private fun getSingleLocomotiveTime(routeList: List<Route>) =
        routeList.getSingleLocomotiveTime()

    private fun getHolidayTime(routeList: List<Route>) =
        routeList.getWorkingTimeOnAHoliday(currentMonthOfYear, userSettings.timeZone)

    private fun getOvertime(totalWorkTime: Long, personalNormaHoursInLong: Int) =
        if (totalWorkTime > personalNormaHoursInLong) totalWorkTime - personalNormaHoursInLong else 0L

    private fun getPersonalNormaInLong(): Int {
        return userSettings.selectMonthOfYear.getPersonalNormaHours() * 3_600_000
    }

    private fun getPersonalNormaHoursToPeriod(period: Pair<Int, Int>): Int {
        return userSettings.selectMonthOfYear.getPersonalNormaHoursInPeriod(
            period,
            currentMonthOfYear
        ) * 3_600_000
    }

    private fun getBasicTimeForCalculationSurcharge(routes: List<Route> = routeList): Flow<Long> {
        return flow {
            val totalWorkTime = getTotalWorkTime(routes).first()
            val passengerTime = getPassengerTime(routes)

            emit(totalWorkTime - passengerTime)
        }
    }
}