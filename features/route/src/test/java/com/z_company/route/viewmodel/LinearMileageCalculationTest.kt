@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.route.viewmodel

import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.entities.setting.UserSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Test
import kotlin.test.assertEquals

class LinearMileageCalculationTest {
    private val moscow = TimeZone.of("Europe/Moscow")

    private fun instant(year: Int, month: Int, day: Int, hour: Int = 10): Long =
        LocalDateTime(year, month, day, hour, 0).toInstant(moscow).toEpochMilliseconds()

    private fun phase(id: String, distance: Int, rate: Double) = ServicePhase(
        id = id,
        departureStation = "А$id",
        arrivalStation = "Б$id",
        distance = distance,
        linearMileageRate = rate,
    )

    private fun route(start: Long, vararg trains: Train) = Route(
        basicData = BasicData(timeStartWork = start, timeEndWork = start + 3_600_000L),
        trains = trains.toMutableList(),
    )

    private fun helper(routes: List<Route>, phases: List<ServicePhase>) = SalaryCalculationHelper(
        userSettings = UserSettings(
            selectMonthOfYear = MonthOfYear(
                year = 2025,
                month = 0,
                days = (1..31).map { Day(it, TagForDay.WORKING_DAY) },
            ),
            servicePhases = phases,
        ),
        salarySetting = SalarySetting(),
        allRoutes = routes,
    )

    @Test
    fun eachPhaseUsesOwnCurrentRateAndActualOrSavedDistance() = runTest {
        val savedA = phase(id = "A", distance = 100, rate = 1.0)
        val savedB = phase(id = "B", distance = 50, rate = 3.0)
        val currentA = savedA.copy(linearMileageRate = 2.0)
        val calculation = helper(
            routes = listOf(
                route(
                    instant(2025, 1, 10),
                    Train(servicePhase = savedA, distance = "120,5"),
                    Train(servicePhase = savedA, distance = null),
                    Train(servicePhase = savedB, distance = "50"),
                ),
            ),
            phases = listOf(currentA, savedB),
        )

        val accruals = calculation.getLinearMileageAccrualsFlow().first()

        assertEquals(2, accruals.size)
        assertEquals(220.5, accruals.first { it.phaseId == "A" }.distance, 0.001)
        assertEquals(441.0, accruals.first { it.phaseId == "A" }.money, 0.001)
        assertEquals(150.0, accruals.first { it.phaseId == "B" }.money, 0.001)
        assertEquals(591.0, calculation.getMoneyLinearMileageFlow().first(), 0.001)
        assertEquals(591.0, calculation.getMoneyTotalChargedFlow().first(), 0.001)
    }

    @Test
    fun zeroRateAndTrainWithoutPhaseDoNotCreateAccrual() = runTest {
        val zero = phase(id = "zero", distance = 100, rate = 0.0)
        val calculation = helper(
            routes = listOf(
                route(
                    instant(2025, 1, 10),
                    Train(servicePhase = zero, distance = "100"),
                    Train(servicePhase = null, distance = "200"),
                ),
            ),
            phases = listOf(zero),
        )

        assertEquals(emptyList(), calculation.getLinearMileageAccrualsFlow().first())
        assertEquals(0.0, calculation.getMoneyLinearMileageFlow().first(), 0.001)
    }

    @Test
    fun repeatedActualPassagesAreCountedButOtherMonthIsIgnored() = runTest {
        val saved = phase(id = "A", distance = 100, rate = 2.0)
        val januaryRoute = route(
            instant(2025, 1, 31, 23),
            Train(servicePhase = saved, distance = "100"),
            Train(servicePhase = saved, distance = "100"),
        ).copy(basicData = BasicData(
            timeStartWork = instant(2025, 1, 31, 23),
            timeEndWork = instant(2025, 2, 1, 2),
        ))
        val decemberRoute = route(
            instant(2024, 12, 31, 23),
            Train(servicePhase = saved, distance = "500"),
        )
        val calculation = helper(listOf(januaryRoute, decemberRoute), listOf(saved))

        val accrual = calculation.getLinearMileageAccrualsFlow().first().single()

        assertEquals(200.0, accrual.distance, 0.001)
        assertEquals(400.0, accrual.money, 0.001)
    }
}
