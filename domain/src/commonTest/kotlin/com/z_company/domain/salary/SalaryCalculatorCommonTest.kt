@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.domain.salary

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
import com.z_company.domain.entities.setting.SurchargeLongTrains
import com.z_company.domain.entities.setting.UserSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SalaryCalculatorCommonTest {
    private val moscow = TimeZone.of("GMT+3")

    private fun instant(hour: Int): Long =
        LocalDateTime(2025, 1, 10, hour, 0).toInstant(moscow).toEpochMilliseconds()

    private fun calculator(setting: SalarySetting, route: Route) = SalaryCalculationHelper(
        userSettings = UserSettings(
            selectMonthOfYear = MonthOfYear(
                year = 2025,
                month = 0,
                tariffRate = 100.0,
                days = (1..31).map { Day(it, TagForDay.WORKING_DAY) },
            ),
            timeZone = 0L,
        ),
        salarySetting = setting,
        allRoutes = listOf(route),
    )

    @Test
    fun noConditionsProduceNoConditionalSurcharges() = runTest {
        val helper = calculator(
            SalarySetting(
                harmfulnessPercent = 0.0,
                zonalSurcharge = 0.0,
                surchargeHeavyTrainsList = emptyList(),
                surchargeLongTrainsList = emptyList(),
            ),
            Route(basicData = BasicData(timeStartWork = instant(8), timeEndWork = instant(12))),
        )

        assertEquals(0.0, helper.getMoneyHarmfulnessFlow().first())
        assertEquals(0.0, helper.getMoneyZonalSurchargeFlow().first())
        assertEquals(0.0, helper.getMoneyListSurchargeExtendedHeavyTrainsFlow().first().sum())
        assertEquals(0.0, helper.getMoneyListSurchargeLongTrainsFlow().first().sum())
        assertEquals(0.0, helper.getMoneyDoubledTrainFirstSurchargeFlow().first())
    }

    @Test
    fun totalEqualsAllSimultaneouslyApplicableAccrualRows() = runTest {
        val route = Route(
            basicData = BasicData(isOnePersonOperation = true, timeStartWork = instant(8), timeEndWork = instant(12)),
            trains = mutableListOf(
                Train(
                    number = "2503",
                    weight = "7000",
                    axle = "351",
                    conditionalLength = "90",
                    doubledTrain = TrainAssist(isFirst = true),
                    stations = mutableListOf(Station(timeDeparture = instant(8)), Station(timeArrival = instant(12))),
                ),
            ),
        )
        val helper = calculator(
            SalarySetting(
                nightTimePercent = 0.0,
                districtCoefficient = 10.0,
                nordicPercent = 20.0,
                onePersonOperationPercent = 40.0,
                harmfulnessPercent = 4.0,
                zonalSurcharge = 25.0,
                surchargeQualificationClass = 10.0,
                surchargeHeavyLongDistanceTrains = 5.0,
                otherSurcharge = 5.0,
                surchargeHeavyTrainsList = listOf(SurchargeHeavyTrains(weight = "6000", percentSurcharge = "10")),
                surchargeLongTrainsList = listOf(SurchargeLongTrains(conditionalLength = "80", percentSurcharge = "20")),
            ),
            route,
        )

        val rows = listOf(
            helper.getMoneyAtWorkTimeAtTariff().first(),
            helper.getMoneyAtQualificationClassFlow().first(),
            helper.getMoneyOnePersonOperationFlow().first(),
            helper.getMoneyHarmfulnessFlow().first(),
            helper.getMoneyZonalSurchargeFlow().first(),
            helper.getMoneyListSurchargeExtendedHeavyTrainsFlow().first().sum(),
            helper.getMoneyListSurchargeLongTrainsFlow().first().sum(),
            helper.getMoneyHeavyLongDistanceTrainsFlow().first(),
            helper.getMoneyOtherSurchargeFlow().first(),
            helper.getMoneyDoubledTrainFirstSurchargeFlow().first(),
            helper.getMoneyDistrictSurcharge().first(),
            helper.getMoneyNordicSurcharge().first(),
        )

        assertTrue(rows.count { it > 0.0 } >= 10)
        assertEquals(rows.sum(), helper.getMoneyTotalChargedFlow().first(), 0.001)
    }
}
