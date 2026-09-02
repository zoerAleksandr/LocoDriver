@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.route.viewmodel

import com.z_company.domain.entities.DateSetTariffRate
import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.TrainAssist
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.SurchargeHeavyTrains
import com.z_company.domain.entities.setting.UserSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Test
import kotlin.test.assertEquals

class OvertimePaymentCompositionTest {
    private val hour = 3_600_000L

    private fun instant(hour: Int): Long = instant(day = 5, hour = hour)

    private fun instant(day: Int, hour: Int): Long = LocalDateTime(2025, 1, day, hour, 0)
        .toInstant(TimeZone.of("GMT+3"))
        .toEpochMilliseconds()

    private fun helper(): SalaryCalculationHelper {
        val route = Route(
            basicData = BasicData(
                isOnePersonOperation = true,
                timeStartWork = instant(8),
                timeEndWork = instant(12),
            ),
            trains = mutableListOf(Train(number = "2503")),
        )
        return SalaryCalculationHelper(
            userSettings = UserSettings(
                selectMonthOfYear = MonthOfYear(
                    year = 2025,
                    month = 0,
                    tariffRate = 100.0,
                    days = emptyList(), // норма 0: все четыре часа сверхурочные
                ),
                timeZone = 0L,
            ),
            salarySetting = SalarySetting(
                onePersonOperationPercent = 40.0,
                zonalSurcharge = 0.0,
                harmfulnessPercent = 0.0,
            ),
            allRoutes = listOf(route),
        )
    }

    @Test
    fun regularOvertimeLineContainsTariffOnlyAndDoesNotRepeatSurcharge() = runTest {
        val helper = helper()

        assertEquals(4 * hour, helper.getTimeOvertimeFlow().first())
        assertEquals(400.0, helper.getMoneyOvertimeFlow().first(), 0.001)
        assertEquals(160.0, helper.getMoneyOnePersonOperationFlow().first(), 0.001)
    }

    @Test
    fun overtimeLinesTogetherProduceOneAndHalfThenDoubleApplicableBase() = runTest {
        val helper = helper()

        val tariffPart = helper.getMoneyOvertimeFlow().first()
        val onePersonPart = helper.getMoneyOnePersonOperationFlow().first()
        val halfRatePart = helper.getMoneySurchargeOvertime05Flow().first()
        val fullRatePart = helper.getMoneySurchargeOvertimeFlow().first()

        assertEquals(140.0, halfRatePart, 0.001)
        assertEquals(280.0, fullRatePart, 0.001)
        assertEquals(980.0, tariffPart + onePersonPart + halfRatePart + fullRatePart, 0.001)
    }

    @Test
    fun september2026UsesActualOvertimePerShiftInsteadOfRouteCount() = runTest {
        fun at(day: Int, hour: Int): Long = LocalDateTime(2026, 9, day, hour, 0)
            .toInstant(TimeZone.of("GMT+3"))
            .toEpochMilliseconds()
        val overtimeShift = Route(basicData = BasicData(
            timeStartWork = at(1, 8),
            timeEndWork = at(1, 12),
        ))
        val zeroDurationShift = Route(basicData = BasicData(
            timeStartWork = at(2, 8),
            timeEndWork = at(2, 8),
        ))
        val helper = SalaryCalculationHelper(
            userSettings = UserSettings(
                selectMonthOfYear = MonthOfYear(
                    year = 2026,
                    month = 8,
                    tariffRate = 100.0,
                    days = emptyList(),
                ),
                timeZone = 0L,
            ),
            salarySetting = SalarySetting(
                nightTimePercent = 0.0,
                zonalSurcharge = 0.0,
                harmfulnessPercent = 0.0,
            ),
            allRoutes = listOf(overtimeShift, zeroDurationShift),
        )

        assertEquals(4 * hour, helper.getTimeOvertimeFlow().first())
        assertEquals(2 * hour, helper.getTimeSurchargeAtOvertime05Flow().first())
        assertEquals(2 * hour, helper.getTimeSurchargeAtOvertimeFlow().first())
    }

    @Test
    fun augustAndSeptember2024UseTariffOnlyThenExpandedOvertimeBase() = runTest {
        fun helper(month: Int): SalaryCalculationHelper {
            fun at(hourOfDay: Int): Long = LocalDateTime(2024, month + 1, 5, hourOfDay, 0)
                .toInstant(TimeZone.of("GMT+3"))
                .toEpochMilliseconds()
            return SalaryCalculationHelper(
                userSettings = UserSettings(
                    selectMonthOfYear = MonthOfYear(
                        year = 2024,
                        month = month,
                        tariffRate = 100.0,
                        days = emptyList(),
                    ),
                    timeZone = 0L,
                ),
                salarySetting = SalarySetting(
                    onePersonOperationPercent = 40.0,
                    nightTimePercent = 0.0,
                    zonalSurcharge = 0.0,
                    harmfulnessPercent = 0.0,
                ),
                allRoutes = listOf(Route(
                    basicData = BasicData(
                        isOnePersonOperation = true,
                        timeStartWork = at(8),
                        timeEndWork = at(12),
                    ),
                    trains = mutableListOf(Train(number = "2503")),
                )),
            )
        }

        val august = helper(month = 7)
        assertEquals(100.0, august.getMoneySurchargeOvertime05Flow().first(), 0.001)
        assertEquals(200.0, august.getMoneySurchargeOvertimeFlow().first(), 0.001)

        val september = helper(month = 8)
        assertEquals(140.0, september.getMoneySurchargeOvertime05Flow().first(), 0.001)
        assertEquals(280.0, september.getMoneySurchargeOvertimeFlow().first(), 0.001)
    }

    @Test
    fun nightOvertimeUsesPreLawAndLaw91BasesOnDifferentSidesOfSeptember2024() = runTest {
        fun helper(month: Int): SalaryCalculationHelper {
            fun at(day: Int, hour: Int): Long = LocalDateTime(2024, month + 1, day, hour, 0)
                .toInstant(TimeZone.of("GMT+3"))
                .toEpochMilliseconds()
            return SalaryCalculationHelper(
                userSettings = UserSettings(
                    selectMonthOfYear = MonthOfYear(
                        year = 2024,
                        month = month,
                        tariffRate = 100.0,
                        days = emptyList(),
                    ),
                    timeZone = 0L,
                ),
                salarySetting = SalarySetting(
                    nightTimePercent = 40.0,
                    zonalSurcharge = 0.0,
                    harmfulnessPercent = 0.0,
                ),
                allRoutes = listOf(Route(basicData = BasicData(
                    timeStartWork = at(day = 5, hour = 22),
                    timeEndWork = at(day = 6, hour = 2),
                ))),
            )
        }

        val august = helper(month = 7)
        assertEquals(160.0, august.getMoneyAtNightTimeFlow().first(), 0.001)
        assertEquals(100.0, august.getMoneySurchargeOvertime05Flow().first(), 0.001)
        assertEquals(200.0, august.getMoneySurchargeOvertimeFlow().first(), 0.001)

        val september = helper(month = 8)
        assertEquals(160.0, september.getMoneyAtNightTimeFlow().first(), 0.001)
        assertEquals(140.0, september.getMoneySurchargeOvertime05Flow().first(), 0.001)
        assertEquals(280.0, september.getMoneySurchargeOvertimeFlow().first(), 0.001)
    }

    @Test
    fun addingZeroDurationRouteDoesNotChangeOvertimeMoney() = runTest {
        val base = helper()
        val sourceRoute = Route(
            basicData = BasicData(
                isOnePersonOperation = true,
                timeStartWork = instant(8),
                timeEndWork = instant(12),
            ),
            trains = mutableListOf(Train(number = "2503")),
        )
        val zeroRoute = Route(basicData = BasicData(
            timeStartWork = instant(day = 6, hour = 8),
            timeEndWork = instant(day = 6, hour = 8),
        ))
        val withZero = SalaryCalculationHelper(
            userSettings = UserSettings(
                selectMonthOfYear = MonthOfYear(
                    year = 2025,
                    month = 0,
                    tariffRate = 100.0,
                    days = emptyList(),
                ),
                timeZone = 0L,
            ),
            salarySetting = SalarySetting(
                onePersonOperationPercent = 40.0,
                zonalSurcharge = 0.0,
                harmfulnessPercent = 0.0,
            ),
            allRoutes = listOf(sourceRoute, zeroRoute),
        )

        assertEquals(
            base.getMoneyTotalChargedFlow().first(),
            withZero.getMoneyTotalChargedFlow().first(),
            0.001,
        )
        assertEquals(
            base.getTimeSurchargeAtOvertime05Flow().first(),
            withZero.getTimeSurchargeAtOvertime05Flow().first(),
        )
    }

    @Test
    fun overtimeTariffPartUsesRateOfLaterPeriodWhereOvertimeOccurred() = runTest {
        fun route(day: Int) = Route(
            basicData = BasicData(
                timeStartWork = instant(day, 8),
                timeEndWork = instant(day, 16),
            ),
        )
        val helper = SalaryCalculationHelper(
            userSettings = UserSettings(
                selectMonthOfYear = MonthOfYear(
                    year = 2025,
                    month = 0,
                    tariffRate = 200.0,
                    dateSetTariffRate = DateSetTariffRate(dateNewRate = 15, oldRate = 100.0),
                    days = (1..31).map { day ->
                        Day(
                            dayOfMonth = day,
                            tag = if (day == 1) TagForDay.WORKING_DAY else TagForDay.NON_WORKING_DAY,
                        )
                    },
                ),
                timeZone = 0L,
            ),
            salarySetting = SalarySetting(zonalSurcharge = 0.0, harmfulnessPercent = 0.0),
            allRoutes = listOf(route(day = 5), route(day = 20)),
        )

        assertEquals(8 * hour, helper.getTimeOvertimeFlow().first())
        assertEquals(1_600.0, helper.getMoneyOvertimeFlow().first(), 0.001)
    }

    @Test
    fun harmfulnessInOvertimeUsesTariffOfActualSegmentInsteadOfMonthlyAverage() = runTest {
        fun route(day: Int, durationHours: Int) = Route(basicData = BasicData(
            timeStartWork = instant(day, 8),
            timeEndWork = instant(day, 8) + durationHours * hour,
        ))
        val helper = SalaryCalculationHelper(
            userSettings = UserSettings(
                selectMonthOfYear = MonthOfYear(
                    year = 2025,
                    month = 0,
                    tariffRate = 200.0,
                    dateSetTariffRate = DateSetTariffRate(dateNewRate = 15, oldRate = 100.0),
                    days = listOf(Day(dayOfMonth = 5, tag = TagForDay.WORKING_DAY)),
                ),
                timeZone = 0L,
            ),
            salarySetting = SalarySetting(
                harmfulnessPercent = 10.0,
                nightTimePercent = 0.0,
                zonalSurcharge = 0.0,
            ),
            allRoutes = listOf(
                route(day = 5, durationHours = 8),
                route(day = 20, durationHours = 4),
            ),
        )

        assertEquals(4 * hour, helper.getTimeOvertimeFlow().first())
        assertEquals(160.0, helper.getMoneyHarmfulnessFlow().first(), 0.001)
        assertEquals(220.0, helper.getMoneySurchargeOvertime05Flow().first(), 0.001)
        assertEquals(440.0, helper.getMoneySurchargeOvertimeFlow().first(), 0.001)
    }

    @Test
    fun qualificationClassAndZonalInOvertimeUseActualTariffSegment() = runTest {
        fun route(day: Int, durationHours: Int) = Route(basicData = BasicData(
            timeStartWork = instant(day, 8),
            timeEndWork = instant(day, 8) + durationHours * hour,
        ))
        val helper = SalaryCalculationHelper(
            userSettings = UserSettings(
                selectMonthOfYear = MonthOfYear(
                    year = 2025,
                    month = 0,
                    tariffRate = 200.0,
                    dateSetTariffRate = DateSetTariffRate(dateNewRate = 15, oldRate = 100.0),
                    days = listOf(Day(dayOfMonth = 5, tag = TagForDay.WORKING_DAY)),
                ),
                timeZone = 0L,
            ),
            salarySetting = SalarySetting(
                surchargeQualificationClass = 10.0,
                zonalSurcharge = 25.0,
                harmfulnessPercent = 0.0,
                nightTimePercent = 0.0,
            ),
            allRoutes = listOf(
                route(day = 5, durationHours = 8),
                route(day = 20, durationHours = 4),
            ),
        )

        assertEquals(160.0, helper.getMoneyAtQualificationClassFlow().first(), 0.001)
        assertEquals(400.0, helper.getMoneyZonalSurchargeFlow().first(), 0.001)
        assertEquals(270.0, helper.getMoneySurchargeOvertime05Flow().first(), 0.001)
        assertEquals(540.0, helper.getMoneySurchargeOvertimeFlow().first(), 0.001)
    }

    @Test
    fun otherSurchargeInOvertimeUsesActualTariffSegment() = runTest {
        fun route(day: Int, durationHours: Int) = Route(basicData = BasicData(
            timeStartWork = instant(day, 8),
            timeEndWork = instant(day, 8) + durationHours * hour,
        ))
        val helper = SalaryCalculationHelper(
            userSettings = UserSettings(
                selectMonthOfYear = MonthOfYear(
                    year = 2025,
                    month = 0,
                    tariffRate = 200.0,
                    dateSetTariffRate = DateSetTariffRate(dateNewRate = 15, oldRate = 100.0),
                    days = listOf(Day(dayOfMonth = 5, tag = TagForDay.WORKING_DAY)),
                ),
                timeZone = 0L,
            ),
            salarySetting = SalarySetting(
                otherSurcharge = 15.0,
                zonalSurcharge = 0.0,
                harmfulnessPercent = 0.0,
                nightTimePercent = 0.0,
            ),
            allRoutes = listOf(
                route(day = 5, durationHours = 8),
                route(day = 20, durationHours = 4),
            ),
        )

        assertEquals(240.0, helper.getMoneyOtherSurchargeFlow().first(), 0.001)
        assertEquals(230.0, helper.getMoneySurchargeOvertime05Flow().first(), 0.001)
        assertEquals(460.0, helper.getMoneySurchargeOvertimeFlow().first(), 0.001)
    }

    @Test
    fun doubledTrainInOvertimeAppliesOnlyToActualTrainSegment() = runTest {
        fun route(day: Int, doubled: Boolean) = Route(
            basicData = BasicData(
                timeStartWork = instant(day, 8),
                timeEndWork = instant(day, 12),
            ),
            trains = mutableListOf(Train(
                doubledTrain = doubled.takeIf { it }?.let { TrainAssist(isFirst = true) },
                stations = mutableListOf(
                    Station(timeDeparture = instant(day, 8)),
                    Station(timeArrival = instant(day, 12)),
                ),
            )),
        )
        val helper = SalaryCalculationHelper(
            userSettings = UserSettings(
                selectMonthOfYear = MonthOfYear(
                    year = 2025,
                    month = 0,
                    tariffRate = 200.0,
                    dateSetTariffRate = DateSetTariffRate(dateNewRate = 15, oldRate = 100.0),
                    days = emptyList(),
                ),
                timeZone = 0L,
            ),
            salarySetting = SalarySetting(
                zonalSurcharge = 0.0,
                harmfulnessPercent = 0.0,
                nightTimePercent = 0.0,
            ),
            allRoutes = listOf(
                route(day = 5, doubled = false),
                route(day = 20, doubled = true),
            ),
        )

        assertEquals(240.0, helper.getMoneyDoubledTrainFirstSurchargeFlow().first(), 0.001)
        // До 01.09.2026 полуторная ступень составляет по 2 ч на каждую из двух
        // смен: 4 ч старой ставки × 0,5. Последние 4 ч новой ставки получают
        // 30% сдвоенного поезда в полной ступени.
        assertEquals(200.0, helper.getMoneySurchargeOvertime05Flow().first(), 0.001)
        assertEquals(1_040.0, helper.getMoneySurchargeOvertimeFlow().first(), 0.001)
    }

    @Test
    fun tieredHeavyTrainPremiumUsesTierOfActualOvertimeSegment() = runTest {
        fun route(day: Int, weight: String) = Route(
            basicData = BasicData(
                timeStartWork = instant(day, 8),
                timeEndWork = instant(day, 12),
            ),
            trains = mutableListOf(Train(
                weight = weight,
                stations = mutableListOf(
                    Station(timeDeparture = instant(day, 8)),
                    Station(timeArrival = instant(day, 12)),
                ),
            )),
        )
        val helper = SalaryCalculationHelper(
            userSettings = UserSettings(
                selectMonthOfYear = MonthOfYear(
                    year = 2025,
                    month = 0,
                    tariffRate = 200.0,
                    dateSetTariffRate = DateSetTariffRate(dateNewRate = 15, oldRate = 100.0),
                    days = emptyList(),
                ),
                timeZone = 0L,
            ),
            salarySetting = SalarySetting(
                surchargeHeavyTrainsList = listOf(
                    SurchargeHeavyTrains(weight = "6000", percentSurcharge = "10"),
                ),
                surchargeLongTrainsList = emptyList(),
                surchargeExtendedServicePhaseList = emptyList(),
                zonalSurcharge = 0.0,
                harmfulnessPercent = 0.0,
                nightTimePercent = 0.0,
            ),
            allRoutes = listOf(
                route(day = 5, weight = "5999"),
                route(day = 20, weight = "6000"),
            ),
        )

        assertEquals(
            listOf(80.0),
            helper.getMoneyListSurchargeExtendedHeavyTrainsFlow().first(),
        )
        assertEquals(200.0, helper.getMoneySurchargeOvertime05Flow().first(), 0.001)
        assertEquals(880.0, helper.getMoneySurchargeOvertimeFlow().first(), 0.001)
    }

    @Test
    fun nightAtStartOfMonthIsNotAveragedIntoDaytimeOvertimeTail() = runTest {
        val nightRoute = Route(basicData = BasicData(
            timeStartWork = instant(day = 5, hour = 22),
            timeEndWork = instant(day = 6, hour = 6),
        ))
        val daytimeOvertimeRoute = Route(basicData = BasicData(
            timeStartWork = instant(day = 20, hour = 8),
            timeEndWork = instant(day = 20, hour = 12),
        ))
        val helper = SalaryCalculationHelper(
            userSettings = UserSettings(
                selectMonthOfYear = MonthOfYear(
                    year = 2025,
                    month = 0,
                    tariffRate = 100.0,
                    days = listOf(Day(dayOfMonth = 5, tag = TagForDay.WORKING_DAY)),
                ),
                timeZone = 0L,
            ),
            salarySetting = SalarySetting(
                nightTimePercent = 40.0,
                zonalSurcharge = 0.0,
                harmfulnessPercent = 0.0,
            ),
            allRoutes = listOf(nightRoute, daytimeOvertimeRoute),
        )

        assertEquals(4 * hour, helper.getTimeOvertimeFlow().first())
        assertEquals(320.0, helper.getMoneyAtNightTimeFlow().first(), 0.001)
        assertEquals(100.0, helper.getMoneySurchargeOvertime05Flow().first(), 0.001)
        assertEquals(200.0, helper.getMoneySurchargeOvertimeFlow().first(), 0.001)
    }

    @Test
    fun onePersonAtStartOfMonthIsNotAveragedIntoOrdinaryOvertimeTail() = runTest {
        val onePersonRoute = Route(
            basicData = BasicData(
                isOnePersonOperation = true,
                timeStartWork = instant(day = 5, hour = 8),
                timeEndWork = instant(day = 5, hour = 16),
            ),
            trains = mutableListOf(Train(number = "2503")),
        )
        val ordinaryOvertimeRoute = Route(basicData = BasicData(
            timeStartWork = instant(day = 20, hour = 8),
            timeEndWork = instant(day = 20, hour = 12),
        ))
        val helper = SalaryCalculationHelper(
            userSettings = UserSettings(
                selectMonthOfYear = MonthOfYear(
                    year = 2025,
                    month = 0,
                    tariffRate = 100.0,
                    days = listOf(Day(dayOfMonth = 5, tag = TagForDay.WORKING_DAY)),
                ),
                timeZone = 0L,
            ),
            salarySetting = SalarySetting(
                onePersonOperationPercent = 40.0,
                nightTimePercent = 0.0,
                zonalSurcharge = 0.0,
                harmfulnessPercent = 0.0,
            ),
            allRoutes = listOf(onePersonRoute, ordinaryOvertimeRoute),
        )

        assertEquals(4 * hour, helper.getTimeOvertimeFlow().first())
        assertEquals(320.0, helper.getMoneyOnePersonOperationFlow().first(), 0.001)
        assertEquals(100.0, helper.getMoneySurchargeOvertime05Flow().first(), 0.001)
        assertEquals(200.0, helper.getMoneySurchargeOvertimeFlow().first(), 0.001)
    }

    @Test
    fun nightInOvertimeTailIsIncludedOnlyForActualNightSegment() = runTest {
        val regularRoute = Route(basicData = BasicData(
            timeStartWork = instant(day = 5, hour = 8),
            timeEndWork = instant(day = 5, hour = 16),
        ))
        val overtimeRoute = Route(basicData = BasicData(
            timeStartWork = instant(day = 20, hour = 22),
            timeEndWork = instant(day = 21, hour = 2),
        ))
        val helper = SalaryCalculationHelper(
            userSettings = UserSettings(
                selectMonthOfYear = MonthOfYear(
                    year = 2025,
                    month = 0,
                    tariffRate = 100.0,
                    days = listOf(Day(dayOfMonth = 5, tag = TagForDay.WORKING_DAY)),
                ),
                timeZone = 0L,
            ),
            salarySetting = SalarySetting(
                nightTimePercent = 40.0,
                zonalSurcharge = 0.0,
                harmfulnessPercent = 0.0,
            ),
            allRoutes = listOf(regularRoute, overtimeRoute),
        )

        assertEquals(4 * hour, helper.getTimeOvertimeFlow().first())
        assertEquals(140.0, helper.getMoneySurchargeOvertime05Flow().first(), 0.001)
        assertEquals(280.0, helper.getMoneySurchargeOvertimeFlow().first(), 0.001)
    }

    @Test
    fun overRestPaymentIsNotIncludedInOvertimeMultiplierBase() = runTest {
        val firstRoute = Route(
            basicData = BasicData(
                timeStartWork = instant(day = 5, hour = 8),
                timeEndWork = instant(day = 5, hour = 12),
                restPointOfTurnover = true,
            ),
        )
        val secondRoute = Route(
            basicData = BasicData(
                timeStartWork = instant(day = 5, hour = 18),
                timeEndWork = instant(day = 5, hour = 22),
            ),
        )
        val helper = SalaryCalculationHelper(
            userSettings = UserSettings(
                selectMonthOfYear = MonthOfYear(
                    year = 2025,
                    month = 0,
                    tariffRate = 100.0,
                    days = emptyList(),
                ),
                timeZone = 0L,
            ),
            salarySetting = SalarySetting(zonalSurcharge = 0.0, harmfulnessPercent = 0.0),
            allRoutes = listOf(firstRoute, secondRoute),
        )

        assertEquals(2 * hour, helper.getOverRestTimeFlow().first())
        assertEquals(200.0 * (2.0 / 3.0), helper.getMoneyOverRestFlow().first(), 0.001)
        assertEquals(200.0, helper.getMoneySurchargeOvertime05Flow().first(), 0.001)
        assertEquals(400.0, helper.getMoneySurchargeOvertimeFlow().first(), 0.001)
        // 800 тариф + 200 доплата 50% + 400 доплата 100% + 133,33 переотдых.
        assertEquals(1_533.333, helper.getMoneyTotalChargedFlow().first(), 0.001)
    }

    @Test
    fun overRestCrossingTariffBoundaryUsesRateOfEachRestSegment() = runTest {
        val firstRoute = Route(
            basicData = BasicData(
                timeStartWork = instant(day = 10, hour = 15),
                timeEndWork = instant(day = 10, hour = 19),
                restPointOfTurnover = true,
            ),
        )
        val secondRoute = Route(
            basicData = BasicData(
                timeStartWork = instant(day = 11, hour = 1),
                timeEndWork = instant(day = 11, hour = 5),
            ),
        )
        val helper = SalaryCalculationHelper(
            userSettings = UserSettings(
                selectMonthOfYear = MonthOfYear(
                    year = 2025,
                    month = 0,
                    tariffRate = 200.0,
                    dateSetTariffRate = DateSetTariffRate(dateNewRate = 11, oldRate = 100.0),
                    days = emptyList(),
                ),
                timeZone = 0L,
            ),
            salarySetting = SalarySetting(),
            allRoutes = listOf(firstRoute, secondRoute),
        )

        assertEquals(2 * hour, helper.getOverRestTimeFlow().first())
        assertEquals((100.0 + 200.0) * (2.0 / 3.0), helper.getMoneyOverRestFlow().first(), 0.001)
    }

    @Test
    fun overRestCrossingMonthIsClippedBySelectedMonthTimezone() = runTest {
        val zone = TimeZone.of("GMT+3")
        fun at(month: Int, day: Int, hour: Int): Long =
            LocalDateTime(2025, month, day, hour, 0).toInstant(zone).toEpochMilliseconds()
        val firstRoute = Route(
            basicData = BasicData(
                timeStartWork = at(month = 1, day = 31, hour = 15),
                timeEndWork = at(month = 1, day = 31, hour = 19),
                restPointOfTurnover = true,
            ),
        )
        val secondRoute = Route(
            basicData = BasicData(
                timeStartWork = at(month = 2, day = 1, hour = 1),
                timeEndWork = at(month = 2, day = 1, hour = 5),
            ),
        )
        val helper = SalaryCalculationHelper(
            userSettings = UserSettings(
                selectMonthOfYear = MonthOfYear(
                    year = 2025,
                    month = 0,
                    tariffRate = 100.0,
                    days = emptyList(),
                ),
                timeZone = 0L,
            ),
            salarySetting = SalarySetting(),
            allRoutes = listOf(firstRoute, secondRoute),
        )

        assertEquals(hour, helper.getOverRestTimeFlow().first())
        assertEquals(100.0 * (2.0 / 3.0), helper.getMoneyOverRestFlow().first(), 0.001)
    }
}
