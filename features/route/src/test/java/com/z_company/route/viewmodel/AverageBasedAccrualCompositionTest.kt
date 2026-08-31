package com.z_company.route.viewmodel

import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.ReleaseType
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AverageBasedAccrualCompositionTest {
    @Test
    fun averageChildCareAndTechnicalStudyUseSeparateHoursAndEnterTotalOnce() = runTest {
        val helper = SalaryCalculationHelper(
            userSettings = UserSettings(
                selectMonthOfYear = MonthOfYear(
                    year = 2025,
                    month = 0,
                    tariffRate = 100.0,
                    days = listOf(
                        Day(1, TagForDay.WORKING_DAY, true, ReleaseType.Other),
                        Day(2, TagForDay.SHORTENED_DAY, true, ReleaseType.ChildCare),
                        Day(
                            dayOfMonth = 3,
                            tag = TagForDay.WORKING_DAY,
                            isReleaseDay = true,
                            releaseType = ReleaseType.TechnicalStudy,
                            hours = 2.5,
                        ),
                    ),
                ),
            ),
            salarySetting = SalarySetting(
                averagePaymentHour = 200.0,
                zonalSurcharge = 0.0,
                harmfulnessPercent = 0.0,
            ),
            allRoutes = emptyList(),
        )

        assertEquals(8 * 3_600_000L, helper.getDayOffHoursFlow().first())
        assertEquals(1_600.0, helper.getMoneyAverageFlow().first(), 0.001)
        assertEquals(7 * 3_600_000L, helper.getHoursCaringForDisableChildren().first())
        assertEquals(1_400.0, helper.getMoneyCaringForDisableChildren().first(), 0.001)
        assertEquals((2.5 * 3_600_000).toLong(), helper.getTechnicalStudyTimeFlow().first())
        assertEquals(500.0, helper.getMoneyTechnicalStudyFlow().first(), 0.001)
        assertEquals(3_500.0, helper.getMoneyTotalChargedFlow().first(), 0.001)
    }
}
