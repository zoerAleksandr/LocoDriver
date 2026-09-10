@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.route.viewmodel

import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Test
import kotlin.test.assertEquals

/**
 * «Ожидание следования пассажиром» (018M) — непокрытый остаток рабочего времени
 * строго перед отправлением пассажиром. Оплачивается по тарифу как следование,
 * но БЕЗ зональной надбавки; вредность (4%) на него начисляется (памятка, п. 1.4).
 */
class PassengerWaitingCalculationTest {
    private val hour = 3_600_000L
    private val moscow = TimeZone.of("Europe/Moscow")

    private fun instant(day: Int, hour: Int): Long =
        LocalDateTime(2025, 1, day, hour, 0).toInstant(moscow).toEpochMilliseconds()

    private fun helper(
        route: Route,
        zonalPercent: Double = 10.0,
        harmfulnessPercent: Double = 4.0,
    ) = SalaryCalculationHelper(
        userSettings = UserSettings(
            selectMonthOfYear = MonthOfYear(
                year = 2025,
                month = 0,
                tariffRate = 100.0,
                days = (1..31).map { Day(it, TagForDay.WORKING_DAY) },
            ),
        ),
        salarySetting = SalarySetting(
            zonalSurcharge = zonalPercent,
            harmfulnessPercent = harmfulnessPercent,
            surchargeQualificationClass = 0.0,
            otherSurcharge = 0.0,
        ),
        allRoutes = listOf(route),
    )

    // Сдал локомотив (22:00) → ждал час → поехал пассажиром (23:00–24:00).
    // Работа: 20:00–24:00 (4 ч), локомотив: 20:00–22:00, пассажир: 23:00–24:00,
    // ожидание: 22:00–23:00 (1 ч).
    private fun waitingRoute() = Route(
        basicData = BasicData(
            timeStartWork = instant(10, 20),
            timeEndWork = instant(11, 0),
        ),
        locomotives = mutableListOf(
            Locomotive(
                basicId = "",
                timeStartOfAcceptance = instant(10, 20),
                timeEndOfDelivery = instant(10, 22),
            ),
        ),
        passengers = mutableListOf(
            Passenger(
                timeDeparture = instant(10, 23),
                timeArrival = instant(11, 0),
            ),
        ),
    )

    @Test
    fun waitingBeforePassengerIsPaidAtTariffWithoutZonal() = runTest {
        val calc = helper(waitingRoute())

        // Ожидание: 1 ч по тарифу.
        assertEquals(1 * hour, calc.getPassengerWaitingTimeFlow().first())
        assertEquals(100.0, calc.getMoneyAtPassengerWaitingFlow().first(), 0.001)

        // Следование пассажиром: 1 ч по тарифу.
        assertEquals(1 * hour, calc.getPassengerTimeFlow().first())
        assertEquals(100.0, calc.getMoneyAtPassengerFlow().first(), 0.001)

        // «Работа по тарифу» = 4 ч − 1 ч следования − 1 ч ожидания = 2 ч.
        assertEquals(2 * hour, calc.getWorkTimeAtTariffFlow().first())
        assertEquals(200.0, calc.getMoneyAtWorkTimeAtTariff().first(), 0.001)
    }

    @Test
    fun zonalExcludesWaitingButIncludesPassengerFollowing() = runTest {
        val calc = helper(waitingRoute(), zonalPercent = 10.0)

        // База зональной = локомотив (2 ч) + следование (1 ч) = 300, без ожидания.
        // 300 × 10% = 30. Если бы ожидание входило — было бы 400 × 10% = 40.
        assertEquals(30.0, calc.getMoneyZonalSurchargeFlow().first(), 0.001)
        assertEquals(3 * hour, calc.getTimeZonalSurchargeFlow().first())
    }

    @Test
    fun harmfulnessIncludesWaiting() = runTest {
        val calc = helper(waitingRoute(), harmfulnessPercent = 4.0)

        // Вредность начисляется на всё рабочее время, включая ожидание:
        // (200 + 100 ожидание + 100 следование) × 4% = 16.
        assertEquals(16.0, calc.getMoneyHarmfulnessFlow().first(), 0.001)
    }

    @Test
    fun freightRouteWithoutPassengerHasNoWaiting() = runTest {
        // Чисто грузовой маршрут: работа 20:00–24:00, локомотив 20:00–22:00.
        // Остаток 22:00–24:00 — это ПЗ-время, а не ожидание (нет следования
        // пассажиром), поэтому остаётся работой по тарифу.
        val route = Route(
            basicData = BasicData(
                timeStartWork = instant(10, 20),
                timeEndWork = instant(11, 0),
            ),
            locomotives = mutableListOf(
                Locomotive(
                    basicId = "",
                    timeStartOfAcceptance = instant(10, 20),
                    timeEndOfDelivery = instant(10, 22),
                ),
            ),
        )
        val calc = helper(route)

        assertEquals(0L, calc.getPassengerWaitingTimeFlow().first())
        assertEquals(0.0, calc.getMoneyAtPassengerWaitingFlow().first(), 0.001)
        assertEquals(4 * hour, calc.getWorkTimeAtTariffFlow().first())
    }

    @Test
    fun gapNotBeforePassengerIsNotWaiting() = runTest {
        // Поехал пассажиром (20:00–21:00), затем ждал (21:00–22:00), затем принял
        // локомотив и работал (22:00–24:00). Пауза 21:00–22:00 идёт ПОСЛЕ прибытия
        // пассажиром и перед работой на локомотиве — это не «ожидание следования
        // пассажиром», а обычная работа по тарифу.
        val route = Route(
            basicData = BasicData(
                timeStartWork = instant(10, 20),
                timeEndWork = instant(11, 0),
            ),
            locomotives = mutableListOf(
                Locomotive(
                    basicId = "",
                    timeStartOfAcceptance = instant(10, 22),
                    timeEndOfDelivery = instant(11, 0),
                ),
            ),
            passengers = mutableListOf(
                Passenger(
                    timeDeparture = instant(10, 20),
                    timeArrival = instant(10, 21),
                ),
            ),
        )
        val calc = helper(route)

        assertEquals(0L, calc.getPassengerWaitingTimeFlow().first())
        // Следование пассажиром 1 ч, работа по тарифу 3 ч (21:00–24:00).
        assertEquals(1 * hour, calc.getPassengerTimeFlow().first())
        assertEquals(3 * hour, calc.getWorkTimeAtTariffFlow().first())
    }
}
