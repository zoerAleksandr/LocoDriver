@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.route.viewmodel

import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals

class SalaryCalculationOrderInvariantTest {
    private val zone = TimeZone.of("GMT+3")

    private fun instant(day: Int, hour: Int): Long =
        LocalDateTime(2025, 1, day, hour, 0).toInstant(zone).toEpochMilliseconds()

    private fun helper(routes: List<Route>) = SalaryCalculationHelper(
        userSettings = UserSettings(
            selectMonthOfYear = MonthOfYear(
                year = 2025,
                month = 0,
                tariffRate = 150.0,
                days = listOf(Day(10, TagForDay.WORKING_DAY)),
            ),
            timeZone = 0L,
        ),
        salarySetting = SalarySetting(
            nightTimePercent = 40.0,
            harmfulnessPercent = 4.0,
            zonalSurcharge = 25.0,
            surchargeQualificationClass = 10.0,
            districtCoefficient = 15.0,
            nordicPercent = 30.0,
            ndfl = 13.0,
            unionistsRetention = 1.0,
        ),
        allRoutes = routes,
    )

    @Test
    fun routePermutationDoesNotChangeTimeAccrualsRetentionsOrTotal() = runTest {
        val first = Route(
            basicData = BasicData(
                timeStartWork = instant(10, 20),
                timeEndWork = instant(11, 2),
                timeStartBreak = instant(10, 23),
                timeEndBreak = instant(11, 0),
            ),
            trains = mutableListOf(Train(
                number = "2503",
                stations = mutableListOf(
                    Station(timeDeparture = instant(10, 20)),
                    Station(timeArrival = instant(11, 2)),
                ),
            )),
        )
        val second = Route(basicData = BasicData(
            isOnePersonOperation = true,
            timeStartWork = instant(12, 8),
            timeEndWork = instant(12, 13),
        ))
        val chronological = helper(listOf(first, second))
        val reversed = helper(listOf(second, first))

        suspend fun snapshot(helper: SalaryCalculationHelper): List<Double> = listOf(
            helper.getTotalWorkTime().first().toDouble(),
            helper.getNightTimeFlow().first().toDouble(),
            helper.getTimeOvertimeFlow().first().toDouble(),
            helper.getMoneyAtWorkTimeAtTariff().first(),
            helper.getMoneyAtNightTimeFlow().first(),
            helper.getMoneyHarmfulnessFlow().first(),
            helper.getMoneyZonalSurchargeFlow().first(),
            helper.getMoneyAtQualificationClassFlow().first(),
            helper.getMoneyOnePersonOperationFlow().first(),
            helper.getMoneySurchargeOvertime05Flow().first(),
            helper.getMoneySurchargeOvertimeFlow().first(),
            helper.getMoneyDistrictSurcharge().first(),
            helper.getMoneyNordicSurcharge().first(),
            helper.getMoneyTotalChargedFlow().first(),
            helper.getMoneyTotalRetentionFlow().first(),
            helper.getMoneyToBeCredited().first(),
        )

        snapshot(chronological).zip(snapshot(reversed)).forEach { (expected, actual) ->
            assertEquals(expected, actual, 0.001)
        }
    }
}
