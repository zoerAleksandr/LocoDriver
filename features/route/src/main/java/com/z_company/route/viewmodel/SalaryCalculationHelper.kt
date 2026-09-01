package com.z_company.route.viewmodel

import com.z_company.domain.entities.ReleaseType
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.entities.WorkScheduleProfile
import com.z_company.domain.entities.UtilForMonthOfYear.getDayoffHoursExcludingWeekends
import com.z_company.domain.entities.UtilForMonthOfYear.getDayoffHoursIncludingWeekends
import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHoursInPeriod
import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHours
import com.z_company.domain.entities.UtilForMonthOfYear.getTechnicalStudyHours
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.getNewRoutesToDayRange
import com.z_company.domain.entities.route.UtilsForEntities.getNightTime
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTimeOutsideWork
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerOutsideWorkIntervals
import com.z_company.domain.entities.route.UtilsForEntities.getSingleLocomotiveTime
import com.z_company.domain.entities.route.UtilsForEntities.getOverRestInterval
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.getTravelTime
import com.z_company.domain.util.AccrualCondition
import com.z_company.domain.util.NightWindow
import com.z_company.domain.util.TariffChange
import com.z_company.domain.util.TimeCalculationContext
import com.z_company.domain.util.TimeInterval
import com.z_company.domain.util.applyTariffChanges
import com.z_company.domain.util.buildSalarySegments
import com.z_company.domain.util.buildTieredTrainSurchargeSegments
import com.z_company.domain.util.sum
import com.z_company.domain.util.subtractAll
import com.z_company.domain.util.toExactIntOrNull
import com.z_company.domain.util.toDoubleOrZero
import com.z_company.domain.util.toFiniteDoubleOrNull
import com.z_company.domain.util.toIntOrZero
import com.z_company.domain.util.toNonNegativeFiniteDoubleOrNull
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

data class LinearMileageAccrual(
    val phaseId: String,
    val phaseName: String,
    val distance: Double,
    val rate: Double,
    val money: Double,
)

private const val HOUR_IN_MILLIS = 3_600_000L
private const val TWO_HOURS_IN_MILLIS = 2 * HOUR_IN_MILLIS
private const val ANNUAL_OVERTIME_THRESHOLD_IN_MILLIS = 120 * HOUR_IN_MILLIS

private fun Double.nonNegativeFiniteOrZero(): Double =
    takeIf { it.isFinite() && it >= 0.0 } ?: 0.0

internal fun validHeavyTrainSurcharges(
    surcharges: List<com.z_company.domain.entities.setting.SurchargeHeavyTrains>,
) = surcharges.mapNotNull { surcharge ->
    surcharge.percentSurcharge.toNonNegativeFiniteDoubleOrNull() ?: return@mapNotNull null
    surcharge.weight.toExactIntOrNull()?.takeIf { it > 0 }?.let { it to surcharge }
}.groupBy { it.first }
    .map { (threshold, duplicates) -> threshold to duplicates.last().second }
    .sortedBy { it.first }
    .map { it.second }

internal fun validLongTrainSurcharges(
    surcharges: List<com.z_company.domain.entities.setting.SurchargeLongTrains>,
) = surcharges.mapNotNull { surcharge ->
    surcharge.percentSurcharge.toNonNegativeFiniteDoubleOrNull() ?: return@mapNotNull null
    surcharge.conditionalLength.toExactIntOrNull()?.takeIf { it > 0 }?.let { it to surcharge }
}.groupBy { it.first }
    .map { (threshold, duplicates) -> threshold to duplicates.last().second }
    .sortedBy { it.first }
    .map { it.second }

internal fun validExtendedServicePhaseSurcharges(
    surcharges: List<com.z_company.domain.entities.setting.SurchargeExtendedServicePhase>,
) = surcharges.mapNotNull { surcharge ->
    surcharge.percentSurcharge.toNonNegativeFiniteDoubleOrNull() ?: return@mapNotNull null
    surcharge.distance.toExactIntOrNull()?.takeIf { it > 0 }?.let { it to surcharge }
}.groupBy { it.first }
    .map { (threshold, duplicates) -> threshold to duplicates.last().second }
    .sortedBy { it.first }
    .map { it.second }

internal fun isFederalLaw144Effective(year: Int, month: Int): Boolean =
    year > 2026 || (year == 2026 && month >= 8)

/**
 * Часы сверхурочной работы, к которым применяется доплата 0,5.
 * Для локомотивных бригад агрегатное правило «первые 2 ч на поездку»
 * совпадает с отраслевой методикой, действовавшей до ФЗ №144-ФЗ. Годовой порог
 * 120 ч здесь не применяется: функция получает данные только одного месяца.
 */
internal fun calculateHalfRateOvertime(
    overtime: Long,
    shiftCount: Int,
    year: Int,
    month: Int,
    annualOvertimeBeforePeriod: Long = 0L,
): Long {
    if (overtime <= 0L || shiftCount <= 0) return 0L
    val firstTwoHoursPerShift = shiftCount.toLong() * TWO_HOURS_IN_MILLIS
    if (!isFederalLaw144Effective(year, month)) {
        return minOf(overtime, firstTwoHoursPerShift)
    }
    val hoursBeforeDoubleOnly = (
            ANNUAL_OVERTIME_THRESHOLD_IN_MILLIS - annualOvertimeBeforePeriod
            ).coerceAtLeast(0L)
    return minOf(overtime, firstTwoHoursPerShift, hoursBeforeDoubleOnly)
}

class SalaryCalculationHelper(
    private val userSettings: UserSettings,
    private val salarySetting: SalarySetting,
    allRoutes: List<Route>,
    // Индивидуальная норма (в часах) для расчёта оплаты недоработки: на текущую
    // дату для текущего месяца, полная — для завершённого. 0 → недоработка не
    // считается (расчёт одного маршрута / будущий месяц).
    private val effectiveNormaHoursForUnderwork: Int = 0,
    // Сумма сверхурочных с января до начала расчётного месяца.
    // Используется только после вступления ФЗ №144-ФЗ.
    private val annualOvertimeBeforePeriod: Long = 0L,
    private val workScheduleProfile: WorkScheduleProfile = WorkScheduleProfile.standard(),
) {
    private val allRoutes: List<Route> = allRoutes
    val currentMonthOfYear = userSettings.selectMonthOfYear
    val dateSetTariffRate = currentMonthOfYear.dateSetTariffRate
    private val timeCalculationContext = TimeCalculationContext.from(userSettings)
    private val currentTariffRate = currentMonthOfYear.tariffRate.nonNegativeFiniteOrZero()
    private val oldTariffRate = dateSetTariffRate?.oldRate?.nonNegativeFiniteOrZero()
        ?: currentTariffRate
    private val averagePaymentHour = salarySetting.averagePaymentHour.nonNegativeFiniteOrZero()

    val date = userSettings.selectMonthOfYear.dateSetTariffRate?.dateNewRate ?: 1
    val firstDate = 1
    val lastDate = userSettings.selectMonthOfYear.days.lastOrNull()?.dayOfMonth ?: 28

    // ── Командировка ──────────────────────────────────────────────────
    // Дни командировки (release-дни с типом BusinessTrip). Переходящий маршрут
    // делится по календарным границам: командировочная часть оплачивается только
    // по среднему часу, оставшаяся — обычным расчётом.
    private val businessTripDays: Set<Int> = currentMonthOfYear.days
        .filter { it.isReleaseDay && it.releaseType == ReleaseType.BusinessTrip }
        .map { it.dayOfMonth }
        .toSet()

    @OptIn(kotlin.time.ExperimentalTime::class)
    private fun businessTripIntervals(): List<TimeInterval> = businessTripDays.map { day ->
        val date = LocalDate(currentMonthOfYear.year, currentMonthOfYear.month + 1, day)
        TimeInterval(
            date.atStartOfDayIn(timeCalculationContext.localTZ).toEpochMilliseconds(),
            date.plus(1, DateTimeUnit.DAY)
                .atStartOfDayIn(timeCalculationContext.localTZ).toEpochMilliseconds(),
        )
    }

    private fun Route.fragments(businessTrip: Boolean): List<Route> {
        val work = TimeInterval(
            basicData.timeStartWork ?: return emptyList(),
            basicData.timeEndWork ?: return emptyList(),
        ).takeUnless(TimeInterval::isEmpty) ?: return emptyList()
        val tripParts = businessTripIntervals().mapNotNull(work::intersect)
        val intervals = if (businessTrip) tripParts else work.subtractAll(tripParts)
        return intervals.map { interval ->
            val clippedBreak = basicData.timeStartBreak?.let { breakStart ->
                basicData.timeEndBreak?.takeIf { it > breakStart }?.let { breakEnd ->
                    TimeInterval(breakStart, breakEnd).intersect(interval)
                }
            }
            copy(basicData = basicData.copy(
                timeStartWork = interval.startMillis,
                timeEndWork = interval.endMillis,
                timeStartBreak = clippedBreak?.startMillis,
                timeEndBreak = clippedBreak?.endMillis,
            ))
        }
    }

    @OptIn(kotlin.time.ExperimentalTime::class)
    private fun Route.startsInSelectedMonth(): Boolean {
        val startMs = basicData.timeStartWork ?: return false
        val date = Instant.fromEpochMilliseconds(startMs)
            .toLocalDateTime(timeCalculationContext.crossMonthTZ).date
        return date.year == currentMonthOfYear.year &&
                date.monthNumber == currentMonthOfYear.month + 1
    }

    private val businessTripRoutes: List<Route> = allRoutes.flatMap { it.fragments(businessTrip = true) }

    // Обычные тарифы и надбавки считаются только по маршрутам вне
    // командировки. Для нормы, недоработки и сверхурочных используется
    // allRoutes: командировочные часы тоже закрывают норму.
    private val routeList: List<Route> = allRoutes.flatMap { it.fragments(businessTrip = false) }

    @OptIn(kotlin.time.ExperimentalTime::class)
    private fun salarySegments(routes: List<Route> = routeList) = routes.flatMap { route ->
        val tariffChange = dateSetTariffRate
        val initialRate = if (tariffChange != null) oldTariffRate else currentTariffRate
        val changes = tariffChange?.let {
            val effectiveAt = LocalDate(
                year = currentMonthOfYear.year,
                month = currentMonthOfYear.month + 1,
                day = it.dateNewRate,
            ).atStartOfDayIn(timeCalculationContext.crossMonthTZ).toEpochMilliseconds()
            listOf(TariffChange(effectiveAt, currentTariffRate))
        }.orEmpty()
        val night = userSettings.nightTime
        route.buildSalarySegments(
            monthOfYear = currentMonthOfYear,
            context = timeCalculationContext,
            initialTariffRatePerHour = initialRate,
            tariffChanges = changes,
            nightWindow = NightWindow(
                startHour = night.startNightHour,
                startMinute = night.startNightMinute,
                endHour = night.endNightHour,
                endMinute = night.endNightMinute,
                offsetFromMoscowMillis = userSettings.timeZone,
            ),
        )
    }

    @OptIn(kotlin.time.ExperimentalTime::class)
    private fun tieredTrainSurchargeSegments(
        routes: List<Route>,
        thresholds: List<Int>,
        condition: AccrualCondition,
        valueOf: (com.z_company.domain.entities.route.Train) -> Int?,
    ): List<List<com.z_company.domain.util.SalarySegment>> {
        val result = MutableList(thresholds.size) {
            mutableListOf<com.z_company.domain.util.SalarySegment>()
        }
        routes.forEach { route ->
            val tariffChange = dateSetTariffRate
            val initialRate = if (tariffChange != null) oldTariffRate else currentTariffRate
            val changes = tariffChange?.let {
                val effectiveAt = LocalDate(
                    year = currentMonthOfYear.year,
                    month = currentMonthOfYear.month + 1,
                    day = it.dateNewRate,
                ).atStartOfDayIn(timeCalculationContext.crossMonthTZ).toEpochMilliseconds()
                listOf(TariffChange(effectiveAt, currentTariffRate))
            }.orEmpty()
            route.buildTieredTrainSurchargeSegments(
                monthOfYear = currentMonthOfYear,
                context = timeCalculationContext,
                initialTariffRatePerHour = initialRate,
                thresholds = thresholds,
                condition = condition,
                tariffChanges = changes,
                valueOf = valueOf,
            ).forEachIndexed { index, segments -> result[index].addAll(segments) }
        }
        return result
    }

    private fun extendedServicePhaseSegments(
        routes: List<Route>,
        surcharges: List<com.z_company.domain.entities.setting.SurchargeExtendedServicePhase>,
    ): List<List<com.z_company.domain.util.SalarySegment>> {
        val thresholds = surcharges.mapNotNull { it.distance.toExactIntOrNull() }
        val result = MutableList(thresholds.size) {
            mutableListOf<com.z_company.domain.util.SalarySegment>()
        }
        routes.forEach { route ->
            val routeDistance = route.trains.sumOf { train ->
                train.distance?.toFiniteDoubleOrNull()?.takeIf { it > 0.0 } ?: 0.0
            }
            val tierIndex = thresholds.indexOfLast { threshold -> routeDistance >= threshold }
            if (tierIndex >= 0) {
                result[tierIndex].addAll(
                    salarySegments(listOf(route))
                        .filter { AccrualCondition.PASSENGER !in it.conditions },
                )
            }
        }
        return result
    }

    fun getWorkTimeAtTariffFlow(): Flow<Long> {
        return channelFlow {
            val personalNormaHoursInLong = getPersonalNormaInLong()
            val totalWorkTime = getTotalWorkTime(routeList).first()
            val passengerTime = getPassengerTime(routeList)
            val singleLocoTime = getSingleLocomotiveTime(routeList)
            val paymentHolidayHours = getHolidayTime(routeList)
            val overtime = getOvertime(totalWorkTime = totalWorkTime, holidayTime = paymentHolidayHours, personalNormaHoursInLong =  personalNormaHoursInLong, )

            var result =
                totalWorkTime - passengerTime - singleLocoTime - paymentHolidayHours - overtime
            if (result < 0) result = 0
            trySend(result)
            awaitClose()
        }
    }

    fun getWorkTimeAtTariffSingleRouteFlow(): Flow<Long> {
        return channelFlow {

            val totalWorkTime = getTotalWorkTime().first()
            val passengerTime = getPassengerTime(routeList)
            val singleLocoTime = getSingleLocomotiveTime(routeList)
            val paymentHolidayHours = getHolidayTime(routeList)

            var result =
                totalWorkTime - passengerTime - singleLocoTime - paymentHolidayHours
            if (result < 0) result = 0
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
            val totalWorkTime = getTotalWorkTime(routeList).first()
            val passengerTime = getPassengerTime(routeList)
            val singleLocoTime = getSingleLocomotiveTime(routeList)
            val paymentHolidayHours = getHolidayTime(routeList)
            val overtime = getOvertime(totalWorkTime = totalWorkTime, holidayTime = paymentHolidayHours, personalNormaHoursInLong = personalNormaHoursInLong)

            var result =
                totalWorkTime - passengerTime - singleLocoTime - paymentHolidayHours - overtime
            if (result < 0) result = 0

            trySend(result)
        }
    }

    fun getWorkTimeInPeriodAtTariffSingleRouteFlow(
        route: Route?
    ): Flow<Long> {
        return channelFlow {
            if (route == null) trySend(0L)
            else {
                val routeList = listOf(route)

                val totalWorkTime = getTotalWorkTime(routeList).first()
                val passengerTime = getPassengerTime(routeList)
                val singleLocoTime = getSingleLocomotiveTime(routeList)
                val paymentHolidayHours = getHolidayTime(routeList)

                var result =
                    totalWorkTime - passengerTime - singleLocoTime - paymentHolidayHours
                if (result < 0) result = 0

                trySend(result)
            }
        }
    }


    fun getMoneyAtWorkTimeAtTariff(): Flow<Double> {
        return channelFlow {
            if (dateSetTariffRate == null) {
                getWorkTimeAtTariffFlow().collect { timeInLong ->
                    val money =
                        timeInLong.times(currentTariffRate) / 3_600_000.toDouble()
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
                    firstRoutesTime.times(oldTariffRate) / 3_600_000.toDouble()

                val secondRoutesTime = getWorkTimeInPeriodAtTariffFlow(
                    routeList = secondRoutes,
                    period = Pair(date, lastDate)
                ).first()
                val secondRoutesMoney =
                    secondRoutesTime.times(currentTariffRate) / 3_600_000.toDouble()
                val result = firstRoutesMoney + secondRoutesMoney
                trySend(result)
            }
            awaitClose()
        }
    }

    fun getMoneyAtWorkTimeAtTariffSingleRoute(): Flow<Double> {
        return channelFlow {
            if (dateSetTariffRate == null) {
                getWorkTimeAtTariffSingleRouteFlow().collect { timeInLong ->
                    val money =
                        timeInLong.times(currentTariffRate) / 3_600_000.toDouble()
                    trySend(money)
                }
            } else {
                val pairRoutes = getTwoRouteList(routeList).first()
                val firstRoutes = pairRoutes.first
                val secondRoutes = pairRoutes.second

                val firstRoutesTime = getWorkTimeInPeriodAtTariffSingleRouteFlow(
                    route = firstRoutes.firstOrNull(),
                ).first()
                val secondRoutesTime = getWorkTimeInPeriodAtTariffSingleRouteFlow(
                    route = secondRoutes.firstOrNull(),
                ).first()

                val firstRoutesMoney =
                    firstRoutesTime.times(oldTariffRate) / 3_600_000.toDouble()

                val secondRoutesMoney =
                    secondRoutesTime.times(currentTariffRate) / 3_600_000.toDouble()

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
        return flow {
            val coefficient = salarySetting.nightTimePercent.nonNegativeFiniteOrZero() / 100
            val money = salarySegments()
                .filter { AccrualCondition.NIGHT in it.conditions }
                .sumOf { it.tariffMoney * coefficient }
            emit(money)
        }
    }

    fun getSingleLocomotiveTimeFlow(routes: List<Route> = routeList): Flow<Long> {
        return flow {
            emit(salarySegments(routes)
                .filter {
                    AccrualCondition.RESERVE in it.conditions &&
                            AccrualCondition.PASSENGER !in it.conditions
                }
                .sumOf { it.interval.durationMillis })
        }
    }

    fun getMoneyAtSingleLocomotiveFlow(): Flow<Double> {
        return flow {
            emit(salarySegments()
                .filter {
                    AccrualCondition.RESERVE in it.conditions &&
                            AccrualCondition.PASSENGER !in it.conditions
                }
                .sumOf { it.tariffMoney })
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
        return flow {
            emit(salarySegments()
                .filter { AccrualCondition.PASSENGER in it.conditions }
                .sumOf { it.tariffMoney })
        }
    }

    // Следование пассажиром ВНЕ рабочего времени («явка по прибытию»): оплачивается
    // отдельно по тарифу (без процентных надбавок), т.к. это проезд, а не работа.
    // При этом время входит в getWorkTime (месяц/норма/карточка), но НЕ в базу для
    // денег — база (getTotalWorkTime) вычитает это время обратно.
    @OptIn(kotlin.time.ExperimentalTime::class)
    fun getMoneyAtPassengerOutsideWorkFlow(): Flow<Double> {
        return flow {
            val initialRate = if (dateSetTariffRate != null) oldTariffRate else currentTariffRate
            val changes = dateSetTariffRate?.let { change ->
                val effectiveAt = LocalDate(
                    currentMonthOfYear.year,
                    currentMonthOfYear.month + 1,
                    change.dateNewRate,
                ).atStartOfDayIn(timeCalculationContext.crossMonthTZ).toEpochMilliseconds()
                listOf(TariffChange(effectiveAt, currentTariffRate))
            }.orEmpty()
            val money = routeList
                .getPassengerOutsideWorkIntervals(currentMonthOfYear, timeCalculationContext)
                .flatMap { it.applyTariffChanges(initialRate, changes) }
                .sumOf { it.tariffMoney }
            emit(money)
        }
    }

    fun getPassengerOutsideWorkTimeFlow(): Flow<Long> = flow {
        emit(routeList.getPassengerTimeOutsideWork(currentMonthOfYear, timeCalculationContext))
    }

    fun getHolidayTimeFlow(routes: List<Route> = routeList): Flow<Long> {
        return channelFlow {
            val time = getHolidayTime(routes)
            trySend(time)
            awaitClose()
        }
    }

    fun getMoneyAtHolidayFlow(): Flow<Double> {
        return flow {
            emit(salarySegments()
                .filter { AccrualCondition.HOLIDAY in it.conditions }
                .sumOf { it.tariffMoney * 2.0 })
        }
    }

    fun getMoneyAtQualificationClassFlow(): Flow<Double> {
        return flow {
            val surchargeQualificationClassPercent =
                salarySetting.surchargeQualificationClass.nonNegativeFiniteOrZero()
            emit(basicSurchargeSegments()
                .sumOf { it.tariffMoney * surchargeQualificationClassPercent / 100 })
        }
    }

    fun getTimeListSurchargeServicePhaseFlow(routes: List<Route> = routeList): Flow<List<Long>> = flow {
        val surcharges = validExtendedServicePhaseSurcharges(
            salarySetting.surchargeExtendedServicePhaseList,
        )
        emit(extendedServicePhaseSegments(routes, surcharges).map { segments ->
            segments.sumOf { it.interval.durationMillis }
        })
    }

    fun getTotalTimeSurchargeServicePhaseFlow(routes: List<Route> = routeList): Flow<Long> = flow {
        emit(getTimeListSurchargeServicePhaseFlow(routes).first().sum())
    }

    fun getPercentListSurchargeExtendedServicePhaseFlow(): Flow<List<String>> {
        return flow {
            val phaseList = validExtendedServicePhaseSurcharges(
                salarySetting.surchargeExtendedServicePhaseList,
            )
            val percentList = phaseList.map {
                it.percentSurcharge
            }
            emit(percentList)
        }
    }

    fun getMoneyListSurchargeExtendedServicePhaseFlow(): Flow<List<Double>> = flow {
        val surcharges = validExtendedServicePhaseSurcharges(
            salarySetting.surchargeExtendedServicePhaseList,
        )
        val segmentsByTier = extendedServicePhaseSegments(routeList, surcharges)
        emit(segmentsByTier.mapIndexed { index, segments ->
            val percent = surcharges[index].percentSurcharge.toDoubleOrZero() / 100
            segments.sumOf { it.tariffMoney * percent }
        })
    }

    fun getPercentOnePersonOperationPassengerTrainFlow(): Flow<Double> {
        return flow {
            val percent = salarySetting.onePersonOperationPassengerTrainPercent
                .nonNegativeFiniteOrZero()
            emit(percent)
        }
    }

    fun getTimeOnePersonOperationPassengerTrainFlow(routes: List<Route> = routeList): Flow<Long> {
        return flow {
            emit(salarySegments(routes)
                .filter {
                    AccrualCondition.ONE_PERSON_PASSENGER in it.conditions &&
                            AccrualCondition.PASSENGER !in it.conditions
                }
                .sumOf { it.interval.durationMillis })
        }
    }

    fun getMoneyOnePersonOperationPassengerTrainFlow(): Flow<Double> {
        return flow {
            val percent = getPercentOnePersonOperationPassengerTrainFlow().first()
            emit(salarySegments()
                .filter {
                    AccrualCondition.ONE_PERSON_PASSENGER in it.conditions &&
                            AccrualCondition.PASSENGER !in it.conditions
                }
                .sumOf { it.tariffMoney * percent / 100 })
        }
    }

    fun getPercentOnePersonOperationFlow(): Flow<Double> {
        return flow {
            val percent = salarySetting.onePersonOperationPercent.nonNegativeFiniteOrZero()
            emit(percent)
        }
    }

    fun getTimeOnePersonOperationFlow(routes: List<Route> = routeList): Flow<Long> {
        return flow {
            emit(salarySegments(routes)
                .filter {
                    AccrualCondition.ONE_PERSON_FREIGHT in it.conditions &&
                            AccrualCondition.PASSENGER !in it.conditions
                }
                .sumOf { it.interval.durationMillis })
        }
    }

    // 2
    fun getMoneyOnePersonOperationFlow(): Flow<Double> {
        return flow {
            val percent = getPercentOnePersonOperationFlow().first()
            emit(salarySegments()
                .filter {
                    AccrualCondition.ONE_PERSON_FREIGHT in it.conditions &&
                            AccrualCondition.PASSENGER !in it.conditions
                }
                .sumOf { it.tariffMoney * percent / 100 })
        }
    }

    fun getPercentHarmfulnessFlow(): Flow<Double> {
        return flow {
            val percent = salarySetting.harmfulnessPercent.nonNegativeFiniteOrZero()
            emit(percent)
        }
    }

    fun getTimeHarmfulnessFlow(routes: List<Route> = routeList): Flow<Long> {
        return flow {
            val insideAndWorking = salarySegments(routes).sumOf { it.interval.durationMillis }
            val passengerBeforeWork = routes.getPassengerTimeOutsideWork(
                currentMonthOfYear,
                timeCalculationContext,
            )
            emit(insideAndWorking + passengerBeforeWork)
        }
    }

    fun getMoneyHarmfulnessFlow(): Flow<Double> {
        return flow {
            val percent = getPercentHarmfulnessFlow().first()
            val insideAndWorking = salarySegments().sumOf { it.tariffMoney }
            val passengerBeforeWork = getMoneyAtPassengerOutsideWorkFlow().first()
            emit((insideAndWorking + passengerBeforeWork) * percent / 100)
        }
    }

    fun getPercentListSurchargeExtendedHeavyTrainsFlow(): Flow<List<String>> {
        return channelFlow {
            val percentList = validHeavyTrainSurcharges(salarySetting.surchargeHeavyTrainsList)
                .map { it.percentSurcharge }
            trySend(percentList)
            awaitClose()
        }
    }

    fun getTimeListSurchargeHeavyTrainsFlow(routes: List<Route> = routeList): Flow<List<Long>> = flow {
        val surcharges = validHeavyTrainSurcharges(salarySetting.surchargeHeavyTrainsList)
        val thresholds = surcharges.mapNotNull { it.weight.toExactIntOrNull() }
        emit(tieredTrainSurchargeSegments(
            routes = routes,
            thresholds = thresholds,
            condition = AccrualCondition.HEAVY_TRAIN,
            valueOf = { it.weight?.toExactIntOrNull() },
        ).map { segments ->
            segments.filter { AccrualCondition.PASSENGER !in it.conditions }
                .sumOf { it.interval.durationMillis }
        })
    }

    fun getTotalTimeHeavyTrainsFlow(routes: List<Route> = routeList): Flow<Long> = flow {
        emit(getTimeListSurchargeHeavyTrainsFlow(routes).first().sum())
    }

    fun getMoneyListSurchargeExtendedHeavyTrainsFlow(): Flow<List<Double>> = flow {
        val surcharges = validHeavyTrainSurcharges(salarySetting.surchargeHeavyTrainsList)
        val thresholds = surcharges.mapNotNull { it.weight.toExactIntOrNull() }
        val segmentsByTier = tieredTrainSurchargeSegments(
            routes = routeList,
            thresholds = thresholds,
            condition = AccrualCondition.HEAVY_TRAIN,
            valueOf = { it.weight?.toExactIntOrNull() },
        )
        emit(segmentsByTier.mapIndexed { index, segments ->
            val percent = surcharges[index].percentSurcharge.toDoubleOrZero() / 100
            segments.filter { AccrualCondition.PASSENGER !in it.conditions }
                .sumOf { it.tariffMoney * percent }
        })
    }

    fun getPercentListSurchargeLongTrainsFlow(): Flow<List<String>> {
        return channelFlow {
            val percentList = validLongTrainSurcharges(salarySetting.surchargeLongTrainsList)
                .map { it.percentSurcharge }
            trySend(percentList)
            awaitClose()
        }
    }

    fun getTimeListSurchargeLongTrainsFlow(routes: List<Route> = routeList): Flow<List<Long>> = flow {
        val surcharges = validLongTrainSurcharges(salarySetting.surchargeLongTrainsList)
        val thresholds = surcharges.mapNotNull { it.conditionalLength.toExactIntOrNull() }
        emit(tieredTrainSurchargeSegments(
            routes = routes,
            thresholds = thresholds,
            condition = AccrualCondition.LONG_TRAIN,
            valueOf = { it.conditionalLength?.toExactIntOrNull() },
        ).map { segments ->
            segments.filter { AccrualCondition.PASSENGER !in it.conditions }
                .sumOf { it.interval.durationMillis }
        })
    }

    fun getTotalTimeLongTrainsFlow(routes: List<Route> = routeList): Flow<Long> = flow {
        emit(getTimeListSurchargeLongTrainsFlow(routes).first().sum())
    }

    fun getMoneyListSurchargeLongTrainsFlow(): Flow<List<Double>> = flow {
        val surcharges = validLongTrainSurcharges(salarySetting.surchargeLongTrainsList)
        val thresholds = surcharges.mapNotNull { it.conditionalLength.toExactIntOrNull() }
        val segmentsByTier = tieredTrainSurchargeSegments(
            routes = routeList,
            thresholds = thresholds,
            condition = AccrualCondition.LONG_TRAIN,
            valueOf = { it.conditionalLength?.toExactIntOrNull() },
        )
        emit(segmentsByTier.mapIndexed { index, segments ->
            val percent = surcharges[index].percentSurcharge.toDoubleOrZero() / 100
            segments.filter { AccrualCondition.PASSENGER !in it.conditions }
                .sumOf { it.tariffMoney * percent }
        })
    }

    fun getTimeHeavyLongDistanceTrainsFlow(routes: List<Route> = routeList): Flow<Long> = flow {
        emit(salarySegments(routes)
            .filter {
                AccrualCondition.HEAVY_LONG_DISTANCE_TRAIN in it.conditions &&
                        AccrualCondition.PASSENGER !in it.conditions
            }
            .sumOf { it.interval.durationMillis })
    }

    fun getPercentHeavyLongDistanceTrainsFlow(): Flow<Double> = flow {
        emit(salarySetting.surchargeHeavyLongDistanceTrains.nonNegativeFiniteOrZero())
    }

    fun getMoneyHeavyLongDistanceTrainsFlow(): Flow<Double> = flow {
        val percent = getPercentHeavyLongDistanceTrainsFlow().first() / 100
        emit(salarySegments()
            .filter {
                AccrualCondition.HEAVY_LONG_DISTANCE_TRAIN in it.conditions &&
                        AccrualCondition.PASSENGER !in it.conditions
            }
            .sumOf { it.tariffMoney * percent })
    }

    fun getPercentZonalSurchargeFlow(): Flow<Double> {
        return flow {
            val percent = salarySetting.zonalSurcharge.nonNegativeFiniteOrZero()
            emit(percent)
        }
    }

    fun getTimeZonalSurchargeFlow(routes: List<Route> = routeList): Flow<Long> {
        return flow {
            val insideAndWorking = salarySegments(routes).sumOf { it.interval.durationMillis }
            val passengerBeforeWork = routes.getPassengerTimeOutsideWork(
                currentMonthOfYear,
                timeCalculationContext,
            )
            emit(insideAndWorking + passengerBeforeWork)
        }
    }

    fun getMoneyZonalSurchargeFlow(): Flow<Double> {
        return flow {
            val percent = getPercentZonalSurchargeFlow().first()
            val insideAndWorking = salarySegments().sumOf { it.tariffMoney }
            val passengerBeforeWork = getMoneyAtPassengerOutsideWorkFlow().first()
            emit((insideAndWorking + passengerBeforeWork) * percent / 100)
        }
    }

    fun getTimeOvertimeFlow(): Flow<Long> {
        return flow {
            val personalNormaHoursInLong = getPersonalNormaInLong()
            val totalWorkTime = getTotalWorkTime(allRoutes).first()
            val paymentHolidayHours = getHolidayTime(allRoutes)
            val overtime = getOvertime(
                totalWorkTime = totalWorkTime,
                personalNormaHoursInLong = personalNormaHoursInLong,
                holidayTime = paymentHolidayHours
            )
            emit(overtime)
        }
    }

    fun getMoneyOvertimeFlow(): Flow<Double> {
        return flow {
            val overTime = getTimeOvertimeFlow().first()
            emit(getOvertimeTariffMoney(overTime))
        }
    }

    /**
     * Обычная строка сверхурочных содержит только тарифную часть. Надбавки уже
     * начислены отдельными строками за фактическое время и входят повторно лишь
     * в дополнительную часть 0,5/1,0 через [getOvertimeMoneyPerMillis].
     */
    private suspend fun getOvertimeTariffMoney(overtime: Long): Double {
        if (overtime <= 0L) return 0.0
        val tariffChange = dateSetTariffRate
            ?: return overtime * currentTariffRate / HOUR_IN_MILLIS.toDouble()

        val (oldRateRoutes, newRateRoutes) = getTwoRouteList(allRoutes).first()
        val oldRateEligibleTime = (
                getTotalWorkTime(oldRateRoutes).first() - getHolidayTime(oldRateRoutes)
                ).coerceAtLeast(0L)
        val newRateEligibleTime = (
                getTotalWorkTime(newRateRoutes).first() - getHolidayTime(newRateRoutes)
                ).coerceAtLeast(0L)

        // При суммированном учёте переработка возникает после выработки нормы,
        // поэтому сначала относится к более поздней части расчётного месяца.
        val newRateOvertime = minOf(overtime, newRateEligibleTime)
        val oldRateOvertime = minOf(
            (overtime - newRateOvertime).coerceAtLeast(0L),
            oldRateEligibleTime,
        )
        return (
                newRateOvertime * currentTariffRate +
                        oldRateOvertime * oldTariffRate
                ) / HOUR_IN_MILLIS.toDouble()
    }

    fun getTimeSurchargeAtOvertime05Flow(): Flow<Long> {
        return flow {
            val overtime = getTimeOvertimeFlow().first()
            val time = calculateHalfRateOvertime(
                overtime = overtime,
                shiftCount = allRoutes.size,
                year = currentMonthOfYear.year,
                month = currentMonthOfYear.month,
                annualOvertimeBeforePeriod = annualOvertimeBeforePeriod,
            )
            emit(time)
        }
    }

    fun getMoneySurchargeOvertime05Flow(): Flow<Double> {
        return flow {
            val time = getTimeSurchargeAtOvertime05Flow().first()
            // С 01.09.2024 база сверхурочных включает компенсационные и
            // стимулирующие выплаты (ФЗ №91-ФЗ), поэтому и 0,5 считаем
            // от полной часовой базы, а не только от тарифа.
            val expandedBaseEffective = currentMonthOfYear.year > 2024 ||
                    (currentMonthOfYear.year == 2024 && currentMonthOfYear.month >= 8)
            val moneyPerMillis = if (expandedBaseEffective) {
                getOvertimeMoneyPerMillis()
            } else {
                currentTariffRate / HOUR_IN_MILLIS.toDouble()
            }
            val money = time.times(moneyPerMillis * 0.5)
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
            val surchargeAtOvertimeHour = getTimeSurchargeAtOvertimeFlow().first()
            val overtimeMoneyPerMillis = getOvertimeMoneyPerMillis()
            val money = surchargeAtOvertimeHour.times(overtimeMoneyPerMillis)
            emit(money)
        }
    }

    fun getPercentDistrictSurcharge(): Flow<Double> {
        return flow {
            val percent = salarySetting.districtCoefficient.nonNegativeFiniteOrZero()
            emit(percent)
        }
    }

    fun getMoneyDistrictSurcharge(): Flow<Double> {
        return flow {
            val baseForCalculation = getRegionalCoefficientBaseMoney()
            val districtCoefficient = getPercentDistrictSurcharge().first()
            val money = baseForCalculation.times(districtCoefficient / 100)
            emit(money)
        }
    }

    fun getPercentNordicSurcharge(): Flow<Double> {
        return flow {
            val percent = salarySetting.nordicPercent.nonNegativeFiniteOrZero()
            emit(percent)
        }
    }

    fun getMoneyNordicSurcharge(): Flow<Double> {
        return flow {
            val baseForCalculation = getRegionalCoefficientBaseMoney()
            val nordicCoefficient = getPercentNordicSurcharge().first()
            val money = baseForCalculation.times(nordicCoefficient / 100)
            emit(money)
        }
    }

    /**
     * Подтверждённая на текущем этапе база районного и северного коэффициентов:
     * обычные тарифные начисления/надбавки, сверхурочные и праздничная оплата.
     * Выплаты по среднему, линейный пробег и переотдых сюда намеренно не входят:
     * для среднего коэффициенты уже учтены, для двух последних нужна местная норма.
     */
    private suspend fun getRegionalCoefficientBaseMoney(): Double =
        getBasicMoney().first() + getMoneyAtHolidayFlow().first()

    fun getDayOffHoursFlow(): Flow<Long> {
        return flow {
            val hours = currentMonthOfYear.getDayoffHoursIncludingWeekends(workScheduleProfile)
            val hoursInLong: Long = hours.times(3_600_000).toLong()
            emit(hoursInLong)
        }
    }

    fun getMoneyAverageFlow(): Flow<Double> {
        return flow {
            val dayOffHoursInLong = getDayOffHoursFlow().first()
            val dayOffHours = dayOffHoursInLong.div(3_600_000)
            val money = averagePaymentHour.times(dayOffHours)
            emit(money)
        }
    }

    // ── Оплата недоработки ────────────────────────────────────────────
    // Если отработано меньше индивидуальной нормы (с учётом отвлечений) —
    // недостающее время. Норма приходит из ViewModel: на текущую дату (для
    // текущего месяца) или полная (для завершённого). Отработанное берём с
    // проездом до явки — как отображается на экране.
    fun getUnderworkTimeFlow(): Flow<Long> = flow {
        if (!salarySetting.showUnderworkPayments || effectiveNormaHoursForUnderwork <= 0) {
            emit(0L)
            return@flow
        }
        val worked = getTotalWorkTimeWithCommute(allRoutes).first()
        // Техзанятия норму не уменьшают, но оплачиваются отдельной строкой по
        // среднему часу — то есть эти часы норму уже «закрывают». Без их учёта
        // те же часы попали бы ещё и в недоработку → двойная оплата по среднему.
        val technicalStudy = getTechnicalStudyTimeFlow().first()
        val normaInLong = effectiveNormaHoursForUnderwork.toLong() * 3_600_000L
        emit((normaInLong - worked - technicalStudy).coerceAtLeast(0L))
    }

    // Оплата недоработки = недостающие часы × средний час.
    fun getMoneyUnderworkFlow(): Flow<Double> = flow {
        val underworkInLong = getUnderworkTimeFlow().first()
        val hours = underworkInLong.toDouble() / 3_600_000
        emit(averagePaymentHour.times(hours))
    }

    // ── Командировка ──────────────────────────────────────────────────
    // Отработанное время в маршрутах командировки (с проездом пассажиром до
    // явки — как в отображении общего отработанного времени).
    fun getBusinessTripTimeFlow(): Flow<Long> {
        return flow {
            val time = businessTripRoutes.getWorkTime(currentMonthOfYear, timeCalculationContext)
            emit(time)
        }
    }

    // true, если среди переданных маршрутов есть хотя бы один в периоде командировки.
    // Для формы (один маршрут) означает, что этот маршрут — командировочный.
    fun hasBusinessTripRoutes(): Boolean = businessTripRoutes.isNotEmpty()

    // Полное обнуление обычных строк допустимо только если после разрезания не
    // осталось ни одного обычного оплачиваемого фрагмента.
    fun isEntirelyBusinessTrip(): Boolean =
        businessTripRoutes.isNotEmpty() && routeList.isEmpty()

    // Оплата маршрутов командировки — ТОЛЬКО по среднему часу, без надбавок.
    fun getMoneyBusinessTripFlow(): Flow<Double> {
        return flow {
            val hoursInLong = getBusinessTripTimeFlow().first()
            val hours = hoursInLong.toDouble() / 3_600_000
            val money = averagePaymentHour.times(hours)
            emit(money)
        }
    }

    // ── Технические занятия ───────────────────────────────────────────
    // Явно заданные пользователем часы техзанятий (ReleaseType.TechnicalStudy)
    // за месяц. На норму не влияют, в «отработанное» не входят — оплачиваются
    // отдельной строкой по среднему часу.
    fun getTechnicalStudyTimeFlow(): Flow<Long> {
        return flow {
            val hours = currentMonthOfYear.getTechnicalStudyHours()
            emit((hours * 3_600_000).toLong())
        }
    }

    // Оплата технических занятий — ТОЛЬКО по среднему часу.
    fun getMoneyTechnicalStudyFlow(): Flow<Double> {
        return flow {
            val hours = currentMonthOfYear.getTechnicalStudyHours()
            emit(averagePaymentHour.times(hours))
        }
    }

    fun getHoursCaringForDisableChildren(): Flow<Long> {
        return flow {
            val hours = currentMonthOfYear.getDayoffHoursExcludingWeekends()
            val hoursInLong: Long = hours.times(3_600_000).toLong()
            emit(hoursInLong)
        }
    }

    fun getMoneyCaringForDisableChildren(): Flow<Double> {
        return flow {
            val hoursCaringForDisableChildrenInLong = getHoursCaringForDisableChildren().first()
            val hoursCaringForDisableChildren = hoursCaringForDisableChildrenInLong.div(3_600_000)
            val money = averagePaymentHour.times(hoursCaringForDisableChildren)
            emit(money)
        }
    }

    fun getPercentOtherSurchargeFlow(): Flow<Double> {
        return flow {
            val percent = salarySetting.otherSurcharge.nonNegativeFiniteOrZero()
            emit(percent)
        }
    }

    fun getMoneyOtherSurchargeFlow(): Flow<Double> {
        return flow {
            val percent = getPercentOtherSurchargeFlow().first()
            emit(basicSurchargeSegments().sumOf { it.tariffMoney * percent / 100 })
        }
    }

    // --- Надбавка за сдвоенные поезда (первый — 30%, второй — 15%) ---

    fun getTimeDoubledTrainFirstSurchargeFlow(routes: List<Route> = routeList): Flow<Long> {
        return flow {
            emit(salarySegments(routes)
                .filter {
                    AccrualCondition.DOUBLED_TRAIN_FIRST in it.conditions &&
                            AccrualCondition.PASSENGER !in it.conditions
                }
                .sumOf { it.interval.durationMillis })
        }
    }

    fun getTimeDoubledTrainSecondSurchargeFlow(routes: List<Route> = routeList): Flow<Long> {
        return flow {
            emit(salarySegments(routes)
                .filter {
                    AccrualCondition.DOUBLED_TRAIN_SECOND in it.conditions &&
                            AccrualCondition.PASSENGER !in it.conditions
                }
                .sumOf { it.interval.durationMillis })
        }
    }

    fun getMoneyDoubledTrainFirstSurchargeFlow(routes: List<Route> = routeList): Flow<Double> {
        return flow {
            emit(salarySegments(routes)
                .filter {
                    AccrualCondition.DOUBLED_TRAIN_FIRST in it.conditions &&
                            AccrualCondition.PASSENGER !in it.conditions
                }
                .sumOf { it.tariffMoney * 0.30 })
        }
    }

    fun getMoneyDoubledTrainSecondSurchargeFlow(routes: List<Route> = routeList): Flow<Double> {
        return flow {
            emit(salarySegments(routes)
                .filter {
                    AccrualCondition.DOUBLED_TRAIN_SECOND in it.conditions &&
                            AccrualCondition.PASSENGER !in it.conditions
                }
                .sumOf { it.tariffMoney * 0.15 })
        }
    }

    // всего начислено
    fun getMoneyTotalChargedFlow(): Flow<Double> {
        return flow {
            val baseMoney = getBasicMoney().first()
            val holidayMoney = getMoneyAtHolidayFlow().first()
            val averageMoney = getMoneyAverageFlow().first()
            val averageMoneyCaringForDisableChildren = getMoneyCaringForDisableChildren().first()
            val businessTripMoney = getMoneyBusinessTripFlow().first()
            val technicalStudyMoney = getMoneyTechnicalStudyFlow().first()
            val nordicSurcharge = getMoneyNordicSurcharge().first()
            val districtSurcharge = getMoneyDistrictSurcharge().first()
            // Оплата недоработки — по среднему часу, без районных/северных надбавок
            // (средний час их уже учитывает).
            val underworkMoney = getMoneyUnderworkFlow().first()
            val linearMileageMoney = getMoneyLinearMileageFlow().first()
            // Переотдых показывается отдельной строкой и является самостоятельным
            // начислением 2/3 тарифа, поэтому обязан входить в общий итог.
            val overRestMoney = getMoneyOverRestFlow().first()
            // 018L до явки находится вне окна работы и потому не входит в
            // getBasicMoney(), но остаётся самостоятельной тарифной выплатой.
            val passengerOutsideWorkMoney = getMoneyAtPassengerOutsideWorkFlow().first()

            val totalMoney =
                baseMoney + holidayMoney + averageMoney + averageMoneyCaringForDisableChildren + businessTripMoney + technicalStudyMoney + nordicSurcharge + districtSurcharge + underworkMoney + linearMileageMoney + overRestMoney + passengerOutsideWorkMoney

            emit(totalMoney)
        }
    }

    /** Разбивка доплаты по плечам: пробеги разных плеч никогда не смешиваются. */
    fun getLinearMileageAccrualsFlow(): Flow<List<LinearMileageAccrual>> = flow {
        val distancesByPhase = linkedMapOf<String, Double>()
        val phasesById = linkedMapOf<String, com.z_company.domain.entities.setting.ServicePhase>()
        val ratesByPhase = linkedMapOf<String, Double>()
        routeList.distinctBy { it.basicData.id }
            .filter { it.startsInSelectedMonth() }
            .forEach { route ->
            route.trains.forEach { train ->
                val savedPhase = train.servicePhase ?: return@forEach
                val currentPhase = userSettings.servicePhases.firstOrNull { it.id == savedPhase.id }
                    ?: savedPhase
                val rate = currentPhase.linearMileageRate.nonNegativeFiniteOrZero()
                if (rate == 0.0) return@forEach
                val distance = (train.distance?.replace(',', '.')?.toDoubleOrNull()
                    ?: savedPhase.distance.toDouble()).nonNegativeFiniteOrZero()
                phasesById[currentPhase.id] = currentPhase
                ratesByPhase[currentPhase.id] = rate
                distancesByPhase[currentPhase.id] = (distancesByPhase[currentPhase.id] ?: 0.0) + distance
            }
        }
        emit(distancesByPhase.map { (phaseId, distance) ->
            val phase = phasesById.getValue(phaseId)
            val rate = ratesByPhase.getValue(phaseId)
            LinearMileageAccrual(
                phaseId = phaseId,
                phaseName = "${phase.departureStation} — ${phase.arrivalStation}",
                distance = distance,
                rate = rate,
                money = distance * rate,
            )
        })
    }

    fun getLinearMileageDistanceFlow(): Flow<Double> = flow {
        emit(getLinearMileageAccrualsFlow().first().sumOf { it.distance })
    }

    fun getMoneyLinearMileageFlow(): Flow<Double> = flow {
        emit(getLinearMileageAccrualsFlow().first().sumOf { it.money })
    }

    fun getPercentNDFLRetentionFlow(): Flow<Double> {
        return flow {
            val percent = salarySetting.ndfl.nonNegativeFiniteOrZero()
            emit(percent)
        }
    }

    fun getMoneyNDFLRetentionFlow(): Flow<Double> {
        return flow {
            val percentNDFL = getPercentNDFLRetentionFlow().first()
            val averageMoneyCaringForDisableChildren = getMoneyCaringForDisableChildren().first()
            val baseForCalculation =
                getMoneyTotalChargedFlow().first() - averageMoneyCaringForDisableChildren
            val money = baseForCalculation.times(percentNDFL / 100)
            emit(money)
        }
    }

    fun getPercentUnionistsRetentionFlow(): Flow<Double> {
        return flow {
            val percent = salarySetting.unionistsRetention.nonNegativeFiniteOrZero()
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
            val percent = salarySetting.otherRetention.nonNegativeFiniteOrZero()
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

    fun getPercentWelfareRetentionFlow(): Flow<Double> {
        return flow {
            val percent = salarySetting.welfarePercent.nonNegativeFiniteOrZero()
            emit(percent)
        }
    }

    // Благосостояние — % от грязной суммы начисления (как Профсоюз/Прочие).
    fun getMoneyWelfareRetentionFlow(): Flow<Double> {
        return flow {
            val percentWelfare = getPercentWelfareRetentionFlow().first()
            val baseForCalculation = getMoneyTotalChargedFlow().first()
            val money = baseForCalculation.times(percentWelfare / 100)
            emit(money)
        }
    }

    fun getPercentAlimonyRetentionFlow(): Flow<Double> {
        return flow {
            val percent = salarySetting.alimonyPercent.nonNegativeFiniteOrZero()
            emit(percent)
        }
    }

    // База для алиментов — «чистая» сумма к выдаче без учёта самих алиментов:
    // всего начислено за вычетом НДФЛ, Профсоюза, Прочих удержаний и Благосостояния.
    fun getMoneyAlimonyBaseFlow(): Flow<Double> {
        return flow {
            val gross = getMoneyTotalChargedFlow().first()
            val ndfl = getMoneyNDFLRetentionFlow().first()
            val unionists = getMoneyUnionistsRetentionFlow().first()
            val other = getMoneyOtherRetentionFlow().first()
            val welfare = getMoneyWelfareRetentionFlow().first()
            emit(gross - ndfl - unionists - other - welfare)
        }
    }

    // Алименты — % от чистой суммы к выдаче (после НДФЛ, Профсоюза, Прочих
    // удержаний и Благосостояния).
    fun getMoneyAlimonyRetentionFlow(): Flow<Double> {
        return flow {
            val percentAlimony = getPercentAlimonyRetentionFlow().first()
            val baseForCalculation = getMoneyAlimonyBaseFlow().first()
            val money = baseForCalculation.times(percentAlimony / 100)
            emit(money)
        }
    }

    // всего удержано
    fun getMoneyTotalRetentionFlow(): Flow<Double> {
        return flow {
            val other = getMoneyOtherRetentionFlow().first()
            val unionists = getMoneyUnionistsRetentionFlow().first()
            val ndfl = getMoneyNDFLRetentionFlow().first()
            val welfare = getMoneyWelfareRetentionFlow().first()
            val alimony = getMoneyAlimonyRetentionFlow().first()
            val total = other + unionists + ndfl + welfare + alimony
            emit(total)
        }
    }

    fun getMoneyToBeCredited(): Flow<Double> {
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
                context = timeCalculationContext,
                isLastDayOfMonth = false
            )

            val secondRoutes = routeList.getNewRoutesToDayRange(
                days = date..lastDate,
                monthOfYear = userSettings.selectMonthOfYear,
                context = timeCalculationContext,
                isLastDayOfMonth = true
            )
            emit(Pair(firstRoutes, secondRoutes))
        }
    }

    @OptIn(kotlin.time.ExperimentalTime::class)
    private fun overRestSalarySegments(): List<com.z_company.domain.util.SalarySegment> {
        val minTimeRest = userSettings.minTimeRestPointOfTurnover
        val monthStart = LocalDate(
            currentMonthOfYear.year,
            currentMonthOfYear.month + 1,
            1,
        ).atStartOfDayIn(timeCalculationContext.crossMonthTZ).toEpochMilliseconds()
        val nextMonthStart = LocalDate(
            currentMonthOfYear.year,
            currentMonthOfYear.month + 1,
            1,
        ).plus(1, DateTimeUnit.MONTH)
            .atStartOfDayIn(timeCalculationContext.crossMonthTZ).toEpochMilliseconds()
        val monthInterval = TimeInterval(monthStart, nextMonthStart)
        val tariffChange = dateSetTariffRate
        val initialRate = if (tariffChange != null) oldTariffRate else currentTariffRate
        val changes = tariffChange?.let {
            val effectiveAt = LocalDate(
                currentMonthOfYear.year,
                currentMonthOfYear.month + 1,
                it.dateNewRate,
            ).atStartOfDayIn(timeCalculationContext.crossMonthTZ).toEpochMilliseconds()
            listOf(TariffChange(effectiveAt, currentTariffRate))
        }.orEmpty()

        val sorted = routeList.sortedBy { it.basicData.timeStartWork }
        return sorted.mapIndexedNotNull { index, route ->
            route.getOverRestInterval(sorted.getOrNull(index + 1), minTimeRest)
                ?.intersect(monthInterval)
        }.flatMap { interval -> interval.applyTariffChanges(initialRate, changes) }
    }

    fun getOverRestTimeFlow(): Flow<Long> = flow {
        emit(overRestSalarySegments().sumOf { it.interval.durationMillis })
    }

    fun getMoneyOverRestFlow(): Flow<Double> = flow {
        emit(overRestSalarySegments().sumOf { it.tariffMoney * (2.0 / 3.0) })
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
            val surchargeOnePersonOperationPassengerTrainFlow =
                getMoneyOnePersonOperationPassengerTrainFlow().first()
            val surchargeHarmfulnessSurchargeMoney = getMoneyHarmfulnessFlow().first()
            val surchargeHeavyTrains = getMoneyListSurchargeExtendedHeavyTrainsFlow().first().sum()
            val surchargeLongTrains = getMoneyListSurchargeLongTrainsFlow().first().sum()
            val surchargeHeavyLongDistanceTrains = getMoneyHeavyLongDistanceTrainsFlow().first()
            val otherSurcharge = getMoneyOtherSurchargeFlow().first()
            val surchargeDoubledTrainFirst = getMoneyDoubledTrainFirstSurchargeFlow().first()
            val surchargeDoubledTrainSecond = getMoneyDoubledTrainSecondSurchargeFlow().first()
            val basicMoney = paymentAtTariffMoney + paymentAtPassengerMoney +
                    paymentAtSingleLocomotiveMoney + zonalSurchargeMoney +
                    paymentNightTimeMoney + surchargeQualificationClassMoney +
                    surchargeExtendedServicePhaseMoney + surchargeOnePersonOperationMoney +
                    surchargeOnePersonOperationPassengerTrainFlow +
                    surchargeHarmfulnessSurchargeMoney +
                    surchargeHeavyTrains + surchargeLongTrains + surchargeHeavyLongDistanceTrains +
                    otherSurcharge + surchargeDoubledTrainFirst + surchargeDoubledTrainSecond

            emit(basicMoney)
        }
    }

    /**
     * Полная база сверхурочных в рублях на миллисекунду: тариф плюс все рассчитанные
     * компенсационные/стимулирующие надбавки на час. Выплату по тарифу
     * вычитаем из базы перед делением, чтобы не потерять тариф, когда все
     * обычные часы месяца уже попали в переработку. При полной командировке берём
     * заданный средний час: он не даёт обнулить оплату переработки.
     */
    private suspend fun getOvertimeMoneyPerMillis(): Double {
        val regularWorkTime = getTotalWorkTime(routeList).first()
        if (regularWorkTime <= 0L) {
            return maxOf(currentTariffRate, averagePaymentHour) /
                    HOUR_IN_MILLIS.toDouble()
        }
        val basicMoney = getBasicMoneyForOvertimeCalculation().first()
        val tariffMoney = getMoneyAtWorkTimeAtTariff().first()
        val surchargeMoney = (basicMoney - tariffMoney).coerceAtLeast(0.0)
        return currentTariffRate / HOUR_IN_MILLIS.toDouble() +
                surchargeMoney / regularWorkTime
    }

    // База рабочего времени для ДЕНЕГ: «чистая» работа без проезда пассажиром до явки
    // (getWorkTime теперь включает этот проезд, поэтому вычитаем его обратно). Так
    // процентные надбавки и переработка считаются только от фактической работы, а
    // проезд оплачивается отдельно по тарифу (getMoneyAtPassengerOutsideWorkFlow).
    fun getTotalWorkTime(routes: List<Route> = routeList) = flow {
        val time = routes.getWorkTime(currentMonthOfYear, timeCalculationContext) -
                routes.getPassengerTimeOutsideWork(currentMonthOfYear, timeCalculationContext)
        emit(time)
    }

    // Полное отработанное время С проездом пассажиром до явки — для ОТОБРАЖЕНИЯ
    // (экран «Зарплата»), чтобы совпадало с главным экраном и карточкой маршрута.
    fun getTotalWorkTimeWithCommute(routes: List<Route> = allRoutes) = flow {
        emit(routes.getWorkTime(currentMonthOfYear, timeCalculationContext))
    }

    private fun getPassengerTime(routeList: List<Route>) = salarySegments(routeList)
        .filter { AccrualCondition.PASSENGER in it.conditions }
        .sumOf { it.interval.durationMillis }

    private fun getSingleLocomotiveTime(routeList: List<Route>) =
        routeList.getSingleLocomotiveTime()

    private fun getHolidayTime(routeList: List<Route>) = salarySegments(routeList)
        .filter { AccrualCondition.HOLIDAY in it.conditions }
        .sumOf { it.interval.durationMillis }

    private fun getOvertime(totalWorkTime: Long, holidayTime: Long, personalNormaHoursInLong: Int) =
        if (totalWorkTime - holidayTime > personalNormaHoursInLong) {
            (totalWorkTime - holidayTime) - personalNormaHoursInLong
        } else {
            0L
        }
    private fun getPersonalNormaInLong(): Int {
        return userSettings.selectMonthOfYear.getPersonalNormaHours(workScheduleProfile) * 3_600_000
    }

    private fun getPersonalNormaHoursToPeriod(period: Pair<Int, Int>): Int {
        return userSettings.selectMonthOfYear.getPersonalNormaHoursInPeriod(
            period,
            currentMonthOfYear
        ) * 3_600_000
    }

    private fun getBasicTimeForCalculationSurcharge(routes: List<Route> = routeList): Flow<Long> {
        return flow {
            emit(basicSurchargeSegments(routes).sumOf { it.interval.durationMillis })
        }
    }

    private fun basicSurchargeSegments(routes: List<Route> = routeList) = salarySegments(routes)
        .filter { AccrualCondition.PASSENGER !in it.conditions }
}
