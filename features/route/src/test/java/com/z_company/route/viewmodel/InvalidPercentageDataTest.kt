@file:OptIn(kotlin.time.ExperimentalTime::class)

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
import kotlin.test.assertTrue

class InvalidPercentageDataTest {
    private val moscow = TimeZone.of("GMT+3")

    private fun instant(hour: Int): Long =
        LocalDateTime(2025, 1, 10, hour, 0).toInstant(moscow).toEpochMilliseconds()

    private fun helper(setting: SalarySetting) = SalaryCalculationHelper(
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
        allRoutes = listOf(Route(basicData = BasicData(
            timeStartWork = instant(8),
            timeEndWork = instant(9),
        ))),
    )

    @Test
    fun invalidAccrualPercentagesFromLegacyDataAreTreatedAsZero() = runTest {
        val helper = helper(SalarySetting(
            nightTimePercent = Double.NaN,
            surchargeQualificationClass = -10.0,
            onePersonOperationPassengerTrainPercent = Double.POSITIVE_INFINITY,
            onePersonOperationPercent = -20.0,
            harmfulnessPercent = Double.NaN,
            surchargeHeavyLongDistanceTrains = Double.NEGATIVE_INFINITY,
            zonalSurcharge = -25.0,
            districtCoefficient = Double.NaN,
            nordicPercent = -30.0,
            otherSurcharge = Double.POSITIVE_INFINITY,
        ))

        assertEquals(0.0, helper.getPercentOnePersonOperationPassengerTrainFlow().first())
        assertEquals(0.0, helper.getPercentOnePersonOperationFlow().first())
        assertEquals(0.0, helper.getPercentHarmfulnessFlow().first())
        assertEquals(0.0, helper.getPercentHeavyLongDistanceTrainsFlow().first())
        assertEquals(0.0, helper.getPercentZonalSurchargeFlow().first())
        assertEquals(0.0, helper.getPercentDistrictSurcharge().first())
        assertEquals(0.0, helper.getPercentNordicSurcharge().first())
        assertEquals(0.0, helper.getPercentOtherSurchargeFlow().first())
        assertEquals(0.0, helper.getMoneyAtNightTimeFlow().first())
        assertEquals(0.0, helper.getMoneyAtQualificationClassFlow().first())
        assertEquals(100.0, helper.getMoneyTotalChargedFlow().first(), 0.001)
    }

    @Test
    fun invalidRetentionPercentagesFromLegacyDataAreTreatedAsZero() = runTest {
        val helper = helper(SalarySetting(
            nightTimePercent = 0.0,
            zonalSurcharge = 0.0,
            ndfl = Double.NaN,
            unionistsRetention = -1.0,
            otherRetention = Double.POSITIVE_INFINITY,
            welfarePercent = Double.NEGATIVE_INFINITY,
            alimonyPercent = -25.0,
        ))

        assertEquals(0.0, helper.getMoneyNDFLRetentionFlow().first())
        assertEquals(0.0, helper.getMoneyUnionistsRetentionFlow().first())
        assertEquals(0.0, helper.getMoneyOtherRetentionFlow().first())
        assertEquals(0.0, helper.getMoneyWelfareRetentionFlow().first())
        assertEquals(0.0, helper.getMoneyAlimonyRetentionFlow().first())
        assertEquals(0.0, helper.getMoneyTotalRetentionFlow().first())
    }

    @Test
    fun percentagesAboveOneHundredRemainSupportedWhenFiniteAndNonNegative() = runTest {
        val helper = helper(SalarySetting(
            nightTimePercent = 0.0,
            zonalSurcharge = 0.0,
            surchargeQualificationClass = 150.0,
        ))

        assertEquals(150.0, helper.getMoneyAtQualificationClassFlow().first(), 0.001)
        assertTrue(helper.getMoneyTotalChargedFlow().first().isFinite())
    }
}
