package com.z_company.route.viewmodel

import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(kotlin.time.ExperimentalTime::class)
class RegionalCoefficientCalculationTest {
    private val moscow = TimeZone.of("Europe/Moscow")

    private fun millis(hour: Int): Long = LocalDateTime(2025, 1, 1, hour, 0)
        .toInstant(moscow)
        .toEpochMilliseconds()

    private fun millis(day: Int, hour: Int): Long = LocalDateTime(2025, 1, day, hour, 0)
        .toInstant(moscow)
        .toEpochMilliseconds()

    private fun helper(
        district: Double,
        nordic: Double,
        averagePaymentHour: Double = 0.0,
    ): SalaryCalculationHelper {
        val days = (1..31).map { day ->
            Day(dayOfMonth = day, tag = if (day == 1) TagForDay.HOLIDAY else TagForDay.WORKING_DAY)
        }
        return SalaryCalculationHelper(
            userSettings = UserSettings(
                selectMonthOfYear = MonthOfYear(year = 2025, month = 0, tariffRate = 100.0, days = days)
            ),
            salarySetting = SalarySetting(
                districtCoefficient = district,
                nordicPercent = nordic,
                averagePaymentHour = averagePaymentHour,
                zonalSurcharge = 0.0,
                nightTimePercent = 0.0,
                harmfulnessPercent = 0.0,
                otherSurcharge = 0.0,
            ),
            allRoutes = listOf(
                Route(basicData = BasicData(timeStartWork = millis(8), timeEndWork = millis(18)))
            ),
        )
    }

    @Test
    fun coefficientsIncludeHolidayPaymentAndRemainIndependent() = runTest {
        val helper = helper(district = 10.0, nordic = 20.0)

        assertEquals(200.0, helper.getMoneyDistrictSurcharge().first(), 0.01)
        assertEquals(400.0, helper.getMoneyNordicSurcharge().first(), 0.01)
    }

    @Test
    fun zeroCoefficientsReturnZero() = runTest {
        val helper = helper(district = 0.0, nordic = 0.0)

        assertEquals(0.0, helper.getMoneyDistrictSurcharge().first(), 0.01)
        assertEquals(0.0, helper.getMoneyNordicSurcharge().first(), 0.01)
    }

    @Test
    fun averagePaymentIsNotCoefficientedAgain() = runTest {
        val withoutAverage = helper(district = 10.0, nordic = 20.0)
        val withAverage = helper(
            district = 10.0,
            nordic = 20.0,
            averagePaymentHour = 10_000.0,
        )

        assertEquals(
            withoutAverage.getMoneyDistrictSurcharge().first(),
            withAverage.getMoneyDistrictSurcharge().first(),
            0.01,
        )
        assertEquals(
            withoutAverage.getMoneyNordicSurcharge().first(),
            withAverage.getMoneyNordicSurcharge().first(),
            0.01,
        )
    }

    @Test
    fun manualNightHolidayAndTwoCoefficientsExample() = runTest {
        val helper = SalaryCalculationHelper(
            userSettings = UserSettings(
                selectMonthOfYear = MonthOfYear(
                    year = 2025,
                    month = 0,
                    tariffRate = 100.0,
                    days = (1..31).map { day ->
                        Day(day, if (day == 2) TagForDay.HOLIDAY else TagForDay.WORKING_DAY)
                    },
                ),
                timeZone = 0L,
            ),
            salarySetting = SalarySetting(
                nightTimePercent = 40.0,
                districtCoefficient = 10.0,
                nordicPercent = 20.0,
                zonalSurcharge = 0.0,
                harmfulnessPercent = 0.0,
            ),
            allRoutes = listOf(Route(basicData = BasicData(
                timeStartWork = millis(day = 1, hour = 22),
                timeEndWork = millis(day = 2, hour = 2),
            ))),
        )

        assertEquals(200.0, helper.getMoneyAtWorkTimeAtTariff().first(), 0.001)
        assertEquals(160.0, helper.getMoneyAtNightTimeFlow().first(), 0.001)
        assertEquals(400.0, helper.getMoneyAtHolidayFlow().first(), 0.001)
        assertEquals(76.0, helper.getMoneyDistrictSurcharge().first(), 0.001)
        assertEquals(152.0, helper.getMoneyNordicSurcharge().first(), 0.001)
        assertEquals(988.0, helper.getMoneyTotalChargedFlow().first(), 0.001)
    }
}
